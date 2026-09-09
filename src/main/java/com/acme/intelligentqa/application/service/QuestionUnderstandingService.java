package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.ClarificationRequest;
import com.acme.intelligentqa.domain.model.ConversationContext;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.model.QuestionUnderstanding;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 将意图识别结果与持久化会话上下文合并，完成指代消解、歧义判断和追问恢复。
 */
@Component
public class QuestionUnderstandingService {

    /**
     * 日期列表意图中的业务日期实体名称。
     */
    private static final String BUSINESS_DATE = "businessDate";
    /**
     * 不能作为明确实体的回复集合。
     */
    private static final Set<String> NON_ENTITY_REPLIES = new java.util.HashSet<>(
            java.util.Arrays.asList("那个", "这个", "它", "上述", "不知道", "不清楚", "没有", "算了", "都不是"));
    /**
     * 配置参数。
     */
    private final QaProperties properties;

    /**
     * 创建 {@code QuestionUnderstandingService} 实例。
     *
     * @param properties 配置参数。
     */
    public QuestionUnderstandingService(final QaProperties properties) {
        this.properties = properties;
    }

    /**
     * 执行指代消解和追问恢复决策。
     *
     * @param question 用户问题。
     *
     * @param recognized 意图识别结果。
     *
     * @param context 结构化会话上下文。
     *
     * @param currentMessageId 当前用户消息 ID。
     *
     * @return 执行指代消解和追问恢复决策。
     */
    public QuestionUnderstanding resolve(
            final String question,
            final QueryIntent recognized,
            final ConversationContext context,
            final UUID currentMessageId) {
        final UUID safeMessageId = java.util.Objects.requireNonNull(
                currentMessageId, "currentMessageId must not be null");
        final Map<String, UUID> currentSources = currentSources(recognized, safeMessageId);
        // 优先消费上一轮追问，保证“第二个”等简短回复仍按原始意图执行，而不是被当作全新问题。
        final QuestionUnderstanding pending = resolvePendingClarification(
                question, recognized, context, safeMessageId);
        if (pending != null) {
            if (pending.requiresClarification()) {
                return pending;
            }
            final QuestionUnderstanding invalidPendingDate = invalidBusinessDate(
                    pending.resolvedIntent(), pending.entitySourceMessageIds());
            return invalidPendingDate == null ? pending : invalidPendingDate;
        }
        if (requiresIntentClarification(recognized)) {
            return lowConfidence(recognized);
        }
        if (!recognized.candidates().isEmpty()) {
            return ambiguous(recognized, currentSources);
        }
        final QuestionUnderstanding invalidDate = invalidBusinessDate(recognized, currentSources);
        if (invalidDate != null) {
            return invalidDate;
        }
        if (recognized.unresolvedEntities().isEmpty()) {
            return safelyResolved(recognized, currentSources);
        }
        return resolveUnresolved(question, recognized, context, currentSources);
    }

    /**
     * 严格校验日期列表意图的显式业务日期，并在格式或日历值非法时确定性追问。
     *
     * @param intent 待校验查询意图。
     * @param entitySources 已确认实体对应的来源用户消息 ID。
     * @return 日期非法时返回追问结果，否则返回空值。
     */
    private QuestionUnderstanding invalidBusinessDate(
            final QueryIntent intent,
            final Map<String, UUID> entitySources) {
        if (intent.type() != QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY) {
            return null;
        }
        final String value = intent.entities().get(BUSINESS_DATE);
        if (value == null) {
            return null;
        }
        try {
            LocalDate.parse(value);
            return null;
        } catch (final DateTimeParseException exception) {
            // 日期是受控查询参数；在问题理解阶段失败关闭，避免知识库先行和数据库解析异常。
            final Map<String, UUID> verifiedSources = new LinkedHashMap<>(entitySources);
            verifiedSources.remove(BUSINESS_DATE);
            return QuestionUnderstanding.clarification(new ClarificationRequest(
                    ClarificationRequest.Reason.MISSING_REQUIRED_PARAMETER,
                    BUSINESS_DATE,
                    "业务日期无效，请按 yyyy-MM-dd 格式提供真实日历日期，例如 2026-09-08。",
                    intent,
                    Collections.emptyList()), verifiedSources);
        }
    }

    /**
     * 判断意图是否不受支持或低于置信度阈值。
     *
     * @param recognized 意图识别结果。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean requiresIntentClarification(final QueryIntent recognized) {
        return recognized.type() == QueryIntent.Type.UNSUPPORTED
                || recognized.confidence() < properties.minIntentConfidence();
    }

    /**
     * 为低置信度意图创建确定性追问。
     *
     * @param recognized 意图识别结果。
     *
     * @return 为低置信度意图创建确定性追问。
     */
    private QuestionUnderstanding lowConfidence(final QueryIntent recognized) {
        // 当前协议只有意图级置信度，无法证明其中任一实体独立可信；失败关闭时不提升任何实体来源。
        return QuestionUnderstanding.clarification(new ClarificationRequest(
                ClarificationRequest.Reason.LOW_CONFIDENCE,
                null,
                "我还不能确定您要查询产品基础信息、交易信息还是最新产品资料，请补充具体查询对象和内容。",
                recognized,
                Collections.emptyList()), Collections.emptyMap());
    }

    /**
     * 从结构化上下文补全尚未确认的指代实体。
     *
     * @param question 用户问题。
     *
     * @param recognized 意图识别结果。
     *
     * @param context 结构化会话上下文。
     *
     * @param currentSources 当前消息中显式实体的来源映射。
     *
     * @return 从结构化上下文补全尚未确认的指代实体。
     */
    private QuestionUnderstanding resolveUnresolved(
            final String question,
            final QueryIntent recognized,
            final ConversationContext context,
            final Map<String, UUID> currentSources) {
        final Map<String, String> entities = new LinkedHashMap<>(recognized.entities());
        final Map<String, UUID> entitySources = new LinkedHashMap<>(currentSources);
        final List<String> missing = new ArrayList<>();
        for (final String entityName : recognized.unresolvedEntities()) {
            final String contextualValue = context == null ? null : context.entities().get(entityName);
            final UUID contextualSource = context == null
                    ? null : context.entitySourceMessageIds().get(entityName);
            // 实体值与来源消息必须成对存在；遗留或损坏上下文不得被当成可信指代依据。
            if (contextualValue == null || contextualValue.trim().isEmpty() || contextualSource == null) {
                missing.add(entityName);
            } else {
                entities.put(entityName, contextualValue);
                entitySources.put(entityName, contextualSource);
            }
        }
        if (missing.isEmpty()) {
            return safelyResolved(recognized.withEntities(entities), entitySources);
        }
        final String entityName = missing.get(0);
        final boolean reference = containsReference(question);
        final ClarificationRequest.Reason reason = reference
                ? ClarificationRequest.Reason.REFERENCE_NOT_FOUND
                : ClarificationRequest.Reason.MISSING_REQUIRED_PARAMETER;
        return QuestionUnderstanding.clarification(new ClarificationRequest(
                reason,
                entityName,
                missingPrompt(entityName, reference),
                recognized,
                Collections.emptyList()), entitySources);
    }

    /**
     * 根据多个实体候选生成确定性选择提示。
     *
     * @param recognized 意图识别结果。
     *
     * @param entitySources 已确认实体对应的来源用户消息 ID。
     *
     * @return 根据多个实体候选生成确定性选择提示。
     */
    private QuestionUnderstanding ambiguous(
            final QueryIntent recognized,
            final Map<String, UUID> entitySources) {
        final Map.Entry<String, List<EntityCandidate>> ambiguity = recognized.candidates().entrySet().iterator().next();
        final List<EntityCandidate> candidates = ambiguity.getValue();
        final Map<String, UUID> verifiedSources = new LinkedHashMap<>(entitySources);
        // 候选尚未由用户选定，原始匹配文本只能留在待追问意图中，不能成为长期可信实体。
        verifiedSources.remove(ambiguity.getKey());
        if (candidates.size() < 2 || candidates.size() > properties.maxContextItems()) {
            return QuestionUnderstanding.clarification(new ClarificationRequest(
                    ClarificationRequest.Reason.MISSING_REQUIRED_PARAMETER,
                    ambiguity.getKey(),
                    "匹配结果过多或不足，请提供更精确的" + entityLabel(ambiguity.getKey()) + "。",
                    recognized,
                    Collections.emptyList()), verifiedSources);
        }
        final StringBuilder prompt = new StringBuilder("找到多个可能的")
                .append(entityLabel(ambiguity.getKey())).append("，请回复序号或完整名称：");
        for (int index = 0; index < candidates.size(); index++) {
            prompt.append('\n').append(index + 1).append(". ").append(candidates.get(index).label());
        }
        return QuestionUnderstanding.clarification(new ClarificationRequest(
                ClarificationRequest.Reason.AMBIGUOUS_ENTITY,
                ambiguity.getKey(),
                prompt.toString(),
                recognized,
                candidates), verifiedSources);
    }

    /**
     * 优先恢复上一轮未完成追问。
     *
     * @param question 用户问题。
     *
     * @param recognized 意图识别结果。
     *
     * @param context 结构化会话上下文。
     *
     * @param currentMessageId 当前用户消息 ID。
     *
     * @return 优先恢复上一轮未完成追问。
     */
    private QuestionUnderstanding resolvePendingClarification(
            final String question,
            final QueryIntent recognized,
            final ConversationContext context,
            final UUID currentMessageId) {
        if (context == null || context.pendingClarification() == null) {
            return null;
        }
        final ClarificationRequest pending = context.pendingClarification();
        final Map<String, UUID> retainedSources = retainedSources(context, pending.pendingIntent());
        if (isSelfContainedNewIntent(recognized)) {
            return null;
        }
        if (pending.entityName() == null) {
            return hasEntitySignal(recognized)
                    ? null : QuestionUnderstanding.clarification(pending, retainedSources);
        }
        if (pending.candidates().isEmpty()) {
            return resolveOpenPrompt(
                    question, recognized, pending, retainedSources, currentMessageId);
        }
        return resolveCandidatePrompt(
                question, recognized, pending, retainedSources, currentMessageId);
    }

    /**
     * 判断当前输入是否已经构成无需实体补充的明确新问题。
     *
     * @param recognized 当前轮意图识别结果。
     * @return 当前输入应放弃旧追问时返回 true。
     */
    private boolean isSelfContainedNewIntent(final QueryIntent recognized) {
        return recognized.type() == QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY
                && recognized.confidence() >= properties.minIntentConfidence();
    }

    /**
     * 将用户回复与已持久化候选匹配。
     *
     * @param question 用户问题。
     *
     * @param recognized 意图识别结果。
     *
     * @param pending 尚未完成的上一轮追问。
     *
     * @param retainedSources 原始待补意图已有实体的来源消息 ID。
     *
     * @param currentMessageId 当前用户消息 ID。
     *
     * @return 将用户回复与已持久化候选匹配。
     */
    private QuestionUnderstanding resolveCandidatePrompt(
            final String question,
            final QueryIntent recognized,
            final ClarificationRequest pending,
            final Map<String, UUID> retainedSources,
            final UUID currentMessageId) {
        final EntityCandidate selected = selectedCandidate(question, pending.candidates());
        final EntityCandidate recognizedCandidate = selected == null
                ? selectedCandidate(recognized.entities().get(pending.entityName()), pending.candidates()) : selected;
        if (recognizedCandidate != null) {
            return resolvedPending(
                    pending, recognizedCandidate.reference(), retainedSources, currentMessageId);
        }
        // 用户给出另一类明确实体时视为切换主题；无可信新实体时继续追问，绝不猜测候选。
        if (hasDifferentExplicitEntity(recognized, pending.entityName())
                || !recognized.candidates().isEmpty() || !recognized.unresolvedEntities().isEmpty()) {
            return null;
        }
        return QuestionUnderstanding.clarification(pending, retainedSources);
    }

    /**
     * 将用户补充的实体合并到原始意图。
     *
     * @param question 用户问题。
     *
     * @param recognized 意图识别结果。
     *
     * @param pending 尚未完成的上一轮追问。
     *
     * @param retainedSources 原始待补意图已有实体的来源消息 ID。
     *
     * @param currentMessageId 当前用户消息 ID。
     *
     * @return 将用户补充的实体合并到原始意图。
     */
    private QuestionUnderstanding resolveOpenPrompt(
            final String question,
            final QueryIntent recognized,
            final ClarificationRequest pending,
            final Map<String, UUID> retainedSources,
            final UUID currentMessageId) {
        final String recognizedValue = recognized.entities().get(pending.entityName());
        if (recognizedValue != null && !recognizedValue.trim().isEmpty()) {
            return resolvedPending(pending, recognizedValue, retainedSources, currentMessageId);
        }
        if (!recognized.entities().isEmpty() || !recognized.candidates().isEmpty()
                || !recognized.unresolvedEntities().isEmpty()) {
            return null;
        }
        if (isDirectEntityReply(question)) {
            return resolvedPending(pending, question.trim(), retainedSources, currentMessageId);
        }
        return QuestionUnderstanding.clarification(pending, retainedSources);
    }

    /**
     * 使用已选实体恢复原始查询意图。
     *
     * @param pending 尚未完成的上一轮追问。
     *
     * @param reference 实体稳定引用。
     *
     * @param retainedSources 原始待补意图已有实体的来源消息 ID。
     *
     * @param currentMessageId 当前用户消息 ID。
     *
     * @return 使用已选实体恢复原始查询意图。
     */
    private QuestionUnderstanding resolvedPending(
            final ClarificationRequest pending,
            final String reference,
            final Map<String, UUID> retainedSources,
            final UUID currentMessageId) {
        final Map<String, String> entities = new LinkedHashMap<>(pending.pendingIntent().entities());
        entities.put(pending.entityName(), reference);
        final Map<String, UUID> entitySources = new LinkedHashMap<>(retainedSources);
        entitySources.put(pending.entityName(), currentMessageId);
        return safelyResolved(pending.pendingIntent().withEntities(entities), entitySources);
    }

    /**
     * 仅在每个已解析实体都具有来源消息时构造可执行结果。
     *
     * @param intent 已解析查询意图。
     * @param entitySources 实体来源消息 ID 映射。
     * @return 可执行的问题理解结果，或要求重新提供无来源实体的追问。
     */
    private QuestionUnderstanding safelyResolved(
            final QueryIntent intent,
            final Map<String, UUID> entitySources) {
        for (final String entityName : intent.entities().keySet()) {
            if (!entitySources.containsKey(entityName)) {
                // 历史结构缺少逐实体来源时必须要求用户重新确认，不能把最新上下文消息冒充来源。
                return QuestionUnderstanding.clarification(new ClarificationRequest(
                        ClarificationRequest.Reason.REFERENCE_NOT_FOUND,
                        entityName,
                        "当前上下文中的" + entityLabel(entityName)
                                + "缺少可核验来源，请重新提供其完整名称或唯一标识。",
                        intent,
                        Collections.emptyList()), entitySources);
            }
        }
        return QuestionUnderstanding.resolved(intent, entitySources);
    }

    /**
     * 将本轮显式识别出的实体绑定到当前用户消息。
     *
     * @param recognized 本轮意图识别结果。
     * @param currentMessageId 当前用户消息 ID。
     * @return 本轮显式实体的来源消息 ID 映射。
     */
    private Map<String, UUID> currentSources(
            final QueryIntent recognized,
            final UUID currentMessageId) {
        final Map<String, UUID> sources = new LinkedHashMap<>();
        for (final String entityName : recognized.entities().keySet()) {
            sources.put(entityName, currentMessageId);
        }
        return sources;
    }

    /**
     * 从持久化上下文恢复待补意图中仍可证明来源的实体。
     *
     * @param context 持久化会话上下文。
     * @param pendingIntent 待恢复的原始意图。
     * @return 实体值与待补意图一致且来源可证明的消息 ID 映射。
     */
    private Map<String, UUID> retainedSources(
            final ConversationContext context,
            final QueryIntent pendingIntent) {
        final Map<String, UUID> sources = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entity : pendingIntent.entities().entrySet()) {
            final String storedValue = context.entities().get(entity.getKey());
            final UUID storedSource = context.entitySourceMessageIds().get(entity.getKey());
            if (entity.getValue().equals(storedValue) && storedSource != null) {
                sources.put(entity.getKey(), storedSource);
            }
        }
        return sources;
    }

    /**
     * 根据稳定引用、完整名称或序号选择候选。
     *
     * @param question 用户问题。
     *
     * @param candidates 实体候选列表。
     *
     * @return 根据稳定引用、完整名称或序号选择候选。
     */
    private EntityCandidate selectedCandidate(final String question, final List<EntityCandidate> candidates) {
        if (question == null) {
            return null;
        }
        final String value = question.trim();
        for (int index = 0; index < candidates.size(); index++) {
            final EntityCandidate candidate = candidates.get(index);
            final List<String> acceptedValues = java.util.Arrays.asList(
                    candidate.reference(), candidate.label(), candidate.canonicalName(),
                    Integer.toString(index + 1), ordinal(index));
            if (acceptedValues.contains(value)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 判断识别结果是否携带实体或待消歧信号。
     *
     * @param recognized 意图识别结果。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean hasEntitySignal(final QueryIntent recognized) {
        return !recognized.entities().isEmpty()
                || !recognized.candidates().isEmpty()
                || !recognized.unresolvedEntities().isEmpty();
    }

    /**
     * 判断新问题是否明确切换到了另一类实体。
     *
     * @param recognized 意图识别结果。
     *
     * @param expectedEntity 上一轮追问期望补充的实体字段。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean hasDifferentExplicitEntity(final QueryIntent recognized, final String expectedEntity) {
        for (final String entityName : recognized.entities().keySet()) {
            if (!expectedEntity.equals(entityName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断回复是否可以作为直接补充的实体文本。
     *
     * @param question 用户问题。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean isDirectEntityReply(final String question) {
        final String value = question.trim();
        return !value.isEmpty() && value.length() <= 80 && !NON_ENTITY_REPLIES.contains(value)
                && !value.matches(".*[？?，。！!].*")
                && !value.matches(".*(查询|请问|多少|是谁|最新|费率|经理|交易员|说明书|公告|备案).*");
    }

    /**
     * 取得候选下标对应的中文序号回复。
     *
     * @param index 当前候选项下标。
     *
     * @return 取得候选下标对应的中文序号回复。
     */
    private String ordinal(final int index) {
        final String[] values = {"第一个", "第二个", "第三个", "第四个", "第五个", "第六个", "第七个", "第八个"};
        return index < values.length ? values[index] : "";
    }

    /**
     * 判断问题中是否存在产品或交易指代词。
     *
     * @param question 用户问题。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean containsReference(final String question) {
        return question.contains("它") || question.contains("这个产品") || question.contains("该产品")
                || question.contains("此产品") || question.contains("这笔交易")
                || question.contains("该交易") || question.contains("上一笔交易");
    }

    /**
     * 根据缺失实体与指代原因生成补充提示。
     *
     * @param entityName 实体字段名称。
     *
     * @param reference 实体稳定引用。
     *
     * @return 根据缺失实体与指代原因生成补充提示。
     */
    private String missingPrompt(final String entityName, final boolean reference) {
        final String label = entityLabel(entityName);
        if (reference) {
            return "当前对话中没有可唯一确认的" + label + "，请提供其完整名称或唯一标识。";
        }
        return "还缺少" + label + "，请提供其完整名称或唯一标识后继续。";
    }

    /**
     * 取得实体字段对应的中文显示名称。
     *
     * @param entityName 实体字段名称。
     *
     * @return 取得实体字段对应的中文显示名称。
     */
    private String entityLabel(final String entityName) {
        if ("productReference".equals(entityName)) {
            return "产品";
        }
        if ("tradeReference".equals(entityName)) {
            return "交易";
        }
        return "查询对象";
    }
}
