package com.acme.intelligentqa.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 跨历史窗口持久化的结构化会话上下文及待追问状态。
 */
public final class ConversationContext {

    /**
     * 会话 ID。
     */
    private final UUID conversationId;
    /**
     * 用户所有者 ID。
     */
    private final String ownerId;
    /**
     * 乐观锁版本号。
     */
    private final long version;
    /**
     * 上一轮已记录的意图类型。
     */
    private final QueryIntent.Type lastIntent;
    /**
     * 结构化实体。
     */
    private final Map<String, String> entities;
    /**
     * 每个结构化实体对应的来源用户消息 ID。
     */
    private final Map<String, UUID> entitySourceMessageIds;
    /**
     * 来源问题 ID。
     */
    private final UUID sourceQuestionId;
    /**
     * 待处理追问。
     */
    private final ClarificationRequest pendingClarification;
    /**
     * 更新时间。
     */
    private final Instant updatedAt;

    /**
     * 创建 {@code ConversationContext} 实例。
     *
     * @param conversationId 会话 ID。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param version 乐观锁版本号。
     *
     * @param lastIntent 上一轮已记录的意图类型。
     *
     * @param entities 结构化实体。
     *
     * @param entitySourceMessageIds 每个结构化实体对应的来源用户消息 ID。
     *
     * @param sourceQuestionId 来源问题 ID。
     *
     * @param pendingClarification 待处理追问。
     *
     * @param updatedAt 更新时间。
     */
    public ConversationContext(
            final UUID conversationId,
            final String ownerId,
            final long version,
            final QueryIntent.Type lastIntent,
            final Map<String, String> entities,
            final Map<String, UUID> entitySourceMessageIds,
            final UUID sourceQuestionId,
            final ClarificationRequest pendingClarification,
            final Instant updatedAt) {
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
        this.ownerId = requireText(ownerId, "ownerId");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        this.version = version;
        this.lastIntent = lastIntent;
        this.entities = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(entities, "entities must not be null")));
        this.entitySourceMessageIds = immutableEntitySources(entitySourceMessageIds, this.entities);
        this.sourceQuestionId = sourceQuestionId;
        this.pendingClarification = pendingClarification;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * 返回会话 ID。
     *
     * @return 会话 ID。
     */
    public UUID conversationId() { return conversationId; }
    /**
     * 返回用户所有者 ID。
     *
     * @return 用户所有者 ID。
     */
    public String ownerId() { return ownerId; }
    /**
     * 返回乐观锁版本号。
     *
     * @return 乐观锁版本号。
     */
    public long version() { return version; }
    /**
     * 处理上一轮已记录的意图类型。
     *
     * @return 上一轮已记录的意图类型。
     */
    public QueryIntent.Type lastIntent() { return lastIntent; }

    /**
     * 返回结构化实体。
     *
     * @return 结构化实体。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Map is an unmodifiable defensive copy")
    public Map<String, String> entities() { return entities; }

    /**
     * 返回每个结构化实体的来源用户消息 ID。
     *
     * @return 不可变的实体来源消息 ID 映射。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Map is an unmodifiable defensive copy")
    public Map<String, UUID> entitySourceMessageIds() { return entitySourceMessageIds; }

    /**
     * 返回来源问题 ID。
     *
     * @return 来源问题 ID。
     */
    public UUID sourceQuestionId() { return sourceQuestionId; }
    /**
     * 返回待处理追问。
     *
     * @return 待处理追问。
     */
    public ClarificationRequest pendingClarification() { return pendingClarification; }
    /**
     * 返回更新时间。
     *
     * @return 更新时间。
     */
    public Instant updatedAt() { return updatedAt; }

    /**
     * 校验实体来源仅引用当前上下文中的实体，并创建不可变副本。
     *
     * @param sources 实体来源消息 ID 映射。
     * @param entityValues 当前上下文中的实体值映射。
     * @return 经过校验的不可变实体来源映射。
     */
    private static Map<String, UUID> immutableEntitySources(
            final Map<String, UUID> sources,
            final Map<String, String> entityValues) {
        final Map<String, UUID> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, UUID> entry : Objects.requireNonNull(
                sources, "entitySourceMessageIds must not be null").entrySet()) {
            if (!entityValues.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("entity source requires a matching entity");
            }
            copy.put(requireText(entry.getKey(), "entity source name"),
                    Objects.requireNonNull(entry.getValue(), "entity source message id must not be null"));
        }
        return Collections.unmodifiableMap(copy);
    }

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
