package com.acme.intelligentqa.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 归属于单一用户的问答会话聚合摘要。
 */
public final class Conversation {

    /**
     * 唯一标识。
     */
    private final UUID id;
    /**
     * 用户所有者 ID。
     */
    private final String ownerId;
    /**
     * 会话名称。
     */
    private final String title;
    /**
     * 会话创建时选定且不可变更的 Agent 类型。
     */
    private final AgentType agentType;
    /**
     * 创建时间。
     */
    private final Instant createdAt;
    /**
     * 更新时间。
     */
    private final Instant updatedAt;

    /**
     * 创建 {@code Conversation} 实例。
     *
     * @param id 唯一标识。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param title 会话名称。
     *
     * @param createdAt 创建时间。
     *
     * @param updatedAt 更新时间。
     */
    public Conversation(
            final UUID id,
            final String ownerId,
            final String title,
            final Instant createdAt,
            final Instant updatedAt) {
        this(id, ownerId, title, AgentType.SMART_DATA, createdAt, updatedAt);
    }

    /**
     * 创建包含固定 Agent 类型的会话。
     *
     * @param id 唯一标识。
     * @param ownerId 用户所有者 ID。
     * @param title 会话名称。
     * @param agentType 会话创建时选定的 Agent 类型。
     * @param createdAt 创建时间。
     * @param updatedAt 更新时间。
     */
    public Conversation(
            final UUID id,
            final String ownerId,
            final String title,
            final AgentType agentType,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = requireText(ownerId, "ownerId");
        this.title = requireText(title, "title");
        this.agentType = Objects.requireNonNull(agentType, "agentType must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * 处理唯一标识。
     *
     * @return 唯一标识。
     */
    public UUID id() {
        return id;
    }

    /**
     * 返回用户所有者 ID。
     *
     * @return 用户所有者 ID。
     */
    public String ownerId() {
        return ownerId;
    }

    /**
     * 返回会话名称。
     *
     * @return 会话名称。
     */
    public String title() {
        return title;
    }

    /**
     * 返回会话创建时选定的 Agent 类型。
     *
     * @return 不可变更的 Agent 类型。
     */
    public AgentType agentType() {
        return agentType;
    }

    /**
     * 返回创建时间。
     *
     * @return 创建时间。
     */
    public Instant createdAt() {
        return createdAt;
    }

    /**
     * 返回更新时间。
     *
     * @return 更新时间。
     */
    public Instant updatedAt() {
        return updatedAt;
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
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
