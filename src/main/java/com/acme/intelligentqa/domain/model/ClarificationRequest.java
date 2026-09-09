package com.acme.intelligentqa.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 无法安全执行查询时返回并持久化的确定性追问信息。
 */
public final class ClarificationRequest {

    /** 触发追问的原因，供前端展示和质量评测使用。 */
    public enum Reason {
        /**
         * 意图识别置信度不足。
         */
        LOW_CONFIDENCE,
        /**
         * 上下文无法确认指代对象。
         */
        REFERENCE_NOT_FOUND,
        /**
         * 存在多个可能实体，需要用户选择。
         */
        AMBIGUOUS_ENTITY,
        /**
         * 缺少执行查询所需的参数。
         */
        MISSING_REQUIRED_PARAMETER
    }

    /**
     * 原因。
     */
    private final Reason reason;
    /**
     * 实体字段名称。
     */
    private final String entityName;
    /**
     * 向用户展示的追问文本。
     */
    private final String prompt;
    /**
     * 待恢复的原始意图。
     */
    private final QueryIntent pendingIntent;
    /**
     * 实体候选列表。
     */
    private final List<EntityCandidate> candidates;

    /**
     * 创建 {@code ClarificationRequest} 实例。
     *
     * @param reason 原因。
     *
     * @param entityName 实体字段名称。
     *
     * @param prompt 向用户展示的追问文本。
     *
     * @param pendingIntent 待恢复的原始意图。
     *
     * @param candidates 实体候选列表。
     */
    public ClarificationRequest(
            final Reason reason,
            final String entityName,
            final String prompt,
            final QueryIntent pendingIntent,
            final List<EntityCandidate> candidates) {
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.entityName = entityName;
        this.prompt = requireText(prompt, "prompt");
        this.pendingIntent = Objects.requireNonNull(pendingIntent, "pendingIntent must not be null");
        this.candidates = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(candidates, "candidates must not be null")));
    }

    /**
     * 返回原因。
     *
     * @return 原因。
     */
    public Reason reason() { return reason; }
    /**
     * 返回实体字段名称。
     *
     * @return 实体字段名称。
     */
    public String entityName() { return entityName; }
    /**
     * 处理向用户展示的追问文本。
     *
     * @return 向用户展示的追问文本。
     */
    public String prompt() { return prompt; }
    /**
     * 返回待恢复的原始意图。
     *
     * @return 待恢复的原始意图。
     */
    public QueryIntent pendingIntent() { return pendingIntent; }

    /**
     * 返回实体候选列表。
     *
     * @return 实体候选列表。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "List is an unmodifiable defensive copy")
    public List<EntityCandidate> candidates() { return candidates; }

    /**
     * 校验文本非空并返回原值。
     *
     * @param value 输入值。
     *
     * @param field 字段名称。
     *
     * @return 校验文本非空并返回原值。
     */
    private static String requireText(final String value, final String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
