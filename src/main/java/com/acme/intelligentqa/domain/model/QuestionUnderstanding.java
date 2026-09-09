package com.acme.intelligentqa.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 问题理解的封闭结果：要么可以安全执行，要么必须向用户追问。
 */
public final class QuestionUnderstanding {

    /**
     * 已具备执行条件的查询意图。
     */
    private final QueryIntent resolvedIntent;
    /**
     * 追问信息。
     */
    private final ClarificationRequest clarification;
    /**
     * 当前理解结果中每个实体对应的来源用户消息 ID。
     */
    private final Map<String, UUID> entitySourceMessageIds;

    /**
     * 创建 {@code QuestionUnderstanding} 实例。
     *
     * @param resolvedIntent 已具备执行条件的查询意图。
     *
     * @param clarification 追问信息。
     *
     * @param entitySourceMessageIds 每个实体对应的来源用户消息 ID。
     */
    private QuestionUnderstanding(
            final QueryIntent resolvedIntent,
            final ClarificationRequest clarification,
            final Map<String, UUID> entitySourceMessageIds) {
        this.resolvedIntent = resolvedIntent;
        this.clarification = clarification;
        this.entitySourceMessageIds = immutableSources(entitySourceMessageIds);
    }

    /**
     * 构造可以进入证据查询阶段的问题理解结果。
     *
     * @param intent 查询意图。
     *
     * @param entitySourceMessageIds 每个实体对应的来源用户消息 ID。
     *
     * @return 构造可以进入证据查询阶段的问题理解结果。
     */
    public static QuestionUnderstanding resolved(
            final QueryIntent intent,
            final Map<String, UUID> entitySourceMessageIds) {
        final QueryIntent safeIntent = Objects.requireNonNull(intent, "intent must not be null");
        if (!safeIntent.entities().keySet().equals(entitySourceMessageIds.keySet())) {
            throw new IllegalArgumentException("resolved entities require exact source message ids");
        }
        return new QuestionUnderstanding(safeIntent, null, entitySourceMessageIds);
    }

    /**
     * 处理追问信息。
     *
     * @param request 接口请求。
     *
     * @param entitySourceMessageIds 已确认实体对应的来源用户消息 ID。
     *
     * @return 追问信息。
     */
    public static QuestionUnderstanding clarification(
            final ClarificationRequest request,
            final Map<String, UUID> entitySourceMessageIds) {
        return new QuestionUnderstanding(
                null, Objects.requireNonNull(request, "request must not be null"), entitySourceMessageIds);
    }

    /**
     * 判断问题理解结果是否需要追问。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    public boolean requiresClarification() { return clarification != null; }
    /**
     * 处理已具备执行条件的查询意图。
     *
     * @return 已具备执行条件的查询意图。
     */
    public QueryIntent resolvedIntent() { return resolvedIntent; }
    /**
     * 返回追问信息。
     *
     * @return 追问信息。
     */
    public ClarificationRequest clarification() { return clarification; }

    /**
     * 返回当前理解结果中每个实体的来源用户消息 ID。
     *
     * @return 不可变的实体来源消息 ID 映射。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Map is an unmodifiable defensive copy")
    public Map<String, UUID> entitySourceMessageIds() { return entitySourceMessageIds; }

    /**
     * 创建不允许调用方修改且不包含空值的实体来源映射。
     *
     * @param sources 实体来源消息 ID 映射。
     * @return 不可变的实体来源映射。
     */
    private static Map<String, UUID> immutableSources(final Map<String, UUID> sources) {
        final Map<String, UUID> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, UUID> entry : Objects.requireNonNull(
                sources, "entitySourceMessageIds must not be null").entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new IllegalArgumentException("entity source name must not be blank");
            }
            copy.put(entry.getKey(),
                    Objects.requireNonNull(entry.getValue(), "entity source message id must not be null"));
        }
        return Collections.unmodifiableMap(copy);
    }
}
