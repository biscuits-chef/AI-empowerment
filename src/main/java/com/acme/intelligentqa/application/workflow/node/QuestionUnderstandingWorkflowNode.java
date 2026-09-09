package com.acme.intelligentqa.application.workflow.node;

import com.acme.intelligentqa.application.service.QuestionUnderstandingService;
import com.acme.intelligentqa.application.service.ResolvedQuestionBuilder;
import com.acme.intelligentqa.application.workflow.QuestionWorkflowContext;
import com.acme.intelligentqa.application.workflow.WorkflowNode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeCode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeResult;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.model.QuestionUnderstanding;
import com.acme.intelligentqa.domain.model.ClarificationRequest;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import com.acme.intelligentqa.domain.port.out.IntentRecognitionPort;
import com.acme.intelligentqa.domain.port.out.ProductEntityResolutionPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 执行意图识别、实体提取、上下文消解和确定性追问判断。
 */
@Component
public class QuestionUnderstandingWorkflowNode implements WorkflowNode {
    /** 产品引用实体字段名。 */
    private static final String PRODUCT_REFERENCE = "productReference";
    /** 意图识别出站端口。 */
    private final IntentRecognitionPort intentRecognition;
    /** 问题理解与追问恢复服务。 */
    private final QuestionUnderstandingService questionUnderstanding;
    /** 产品代码、名称、简称和全称解析端口。 */
    private final ProductEntityResolutionPort productEntityResolution;
    /** 问题理解候选数量上限配置。 */
    private final QaProperties properties;

    /**
     * 创建问题理解工作流节点。
     *
     * @param intentRecognition 意图识别出站端口。
     * @param questionUnderstanding 问题理解与追问恢复服务。
     * @param productEntityResolution 产品实体解析端口。
     * @param properties 问题理解候选数量上限配置。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public QuestionUnderstandingWorkflowNode(
            final IntentRecognitionPort intentRecognition,
            final QuestionUnderstandingService questionUnderstanding,
            final ProductEntityResolutionPort productEntityResolution,
            final QaProperties properties) {
        this.intentRecognition = Objects.requireNonNull(
                intentRecognition, "intentRecognition must not be null");
        this.questionUnderstanding = Objects.requireNonNull(
                questionUnderstanding, "questionUnderstanding must not be null");
        this.productEntityResolution = Objects.requireNonNull(
                productEntityResolution, "productEntityResolution must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 返回问题理解节点稳定编码。
     *
     * @return 问题理解节点编码。
     */
    @Override
    public WorkflowNodeCode code() { return WorkflowNodeCode.QUESTION_UNDERSTANDING; }

    /**
     * 识别并消解问题；需要用户补充信息时合法短路后续证据查询。
     *
     * @param context 当前问答工作流上下文。
     * @return 参数完整时继续，否则停止并等待追问。
     */
    @Override
    public WorkflowNodeResult execute(final QuestionWorkflowContext context) {
        final QueryIntent recognized = intentRecognition.recognize(
                context.ownerId(), context.question(), context.history());
        context.ensureActive();
        final QuestionUnderstanding understanding = questionUnderstanding.resolve(
                context.question(), recognized, context.conversationContext(), context.answer().questionId());
        if (understanding.requiresClarification()) {
            context.clarify(
                    understanding.clarification(), understanding.entitySourceMessageIds());
            return WorkflowNodeResult.STOP;
        }
        final QueryIntent intent = resolveProductEntity(
                context, understanding.resolvedIntent(), understanding.entitySourceMessageIds());
        if (context.clarification() != null) {
            return WorkflowNodeResult.STOP;
        }
        context.resolve(
                contextIntent(intent),
                ResolvedQuestionBuilder.build(context.question(), intent),
                understanding.entitySourceMessageIds());
        return WorkflowNodeResult.CONTINUE;
    }

    /**
     * 将用户输入的产品名称或代码转换为 GoldenDB 中唯一产品代码。
     *
     * @param context 当前问答工作流上下文。
     * @param intent 已完成上下文消解的查询意图。
     * @param entitySources 已确认实体对应的来源用户消息 ID。
     * @return 绑定唯一产品代码后的查询意图；需要追问时返回原意图。
     */
    private QueryIntent resolveProductEntity(
            final QuestionWorkflowContext context,
            final QueryIntent intent,
            final Map<String, UUID> entitySources) {
        final String reference = intent.entities().get(PRODUCT_REFERENCE);
        if (reference == null || intent.type() == QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY) {
            return intent;
        }
        final List<EntityCandidate> candidates = productEntityResolution.resolve(context.ownerId(), reference);
        if (candidates.size() == 1) {
            final Map<String, String> entities = new LinkedHashMap<>(intent.entities());
            entities.put(PRODUCT_REFERENCE, candidates.get(0).reference());
            return intent.withEntities(entities);
        }
        final ClarificationRequest request = candidates.isEmpty()
                ? notFound(intent) : ambiguous(intent, candidates);
        final Map<String, UUID> verifiedSources = new LinkedHashMap<>(entitySources);
        // 产品名尚未映射到唯一 GoldenDB 代码时只保留在追问载荷中，不能写入长期可信上下文。
        verifiedSources.remove(PRODUCT_REFERENCE);
        context.clarify(request, verifiedSources);
        return intent;
    }

    /**
     * 创建产品未找到时的确定性追问。
     *
     * @param intent 待恢复的原始意图。
     * @return 产品未找到追问。
     */
    private ClarificationRequest notFound(final QueryIntent intent) {
        return new ClarificationRequest(
                ClarificationRequest.Reason.REFERENCE_NOT_FOUND,
                PRODUCT_REFERENCE,
                "未找到对应产品，请提供准确的产品代码、产品名称、简称或全称。",
                intent,
                Collections.emptyList());
    }

    /**
     * 创建多个产品精确匹配时的确定性选择追问。
     *
     * @param intent 待恢复的原始意图。
     * @param candidates 受权产品候选列表。
     * @return 产品候选选择追问。
     */
    private ClarificationRequest ambiguous(
            final QueryIntent intent,
            final List<EntityCandidate> candidates) {
        if (candidates.size() > properties.maxContextItems()) {
            return new ClarificationRequest(
                    ClarificationRequest.Reason.MISSING_REQUIRED_PARAMETER,
                    PRODUCT_REFERENCE,
                    "匹配产品过多，请提供更准确的产品代码或完整名称。",
                    intent,
                    Collections.emptyList());
        }
        final StringBuilder prompt = new StringBuilder("找到多个可能的产品，请回复序号、产品代码或完整名称：");
        for (int index = 0; index < candidates.size(); index++) {
            prompt.append('\n').append(index + 1).append(". ").append(candidates.get(index).label());
        }
        return new ClarificationRequest(
                ClarificationRequest.Reason.AMBIGUOUS_ENTITY,
                PRODUCT_REFERENCE,
                prompt.toString(),
                intent,
                candidates);
    }

    /**
     * 校验问题理解结果包含可执行意图。
     *
     * @param intent 已消解的查询意图。
     * @return 非空查询意图。
     */
    private QueryIntent contextIntent(final QueryIntent intent) {
        return Objects.requireNonNull(intent, "resolved intent must not be null");
    }
}
