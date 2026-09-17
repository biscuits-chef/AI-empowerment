package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import java.time.Instant;

/** 与 {@code qa_conversation} 表对应的持久化记录。 */
@TableName("qa_conversation")
public class ConversationPersistenceRecord {

    /** 数据库内部自增主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 对外稳定 UUID 标识。
     */
    @TableField("public_id")
    private String publicId;
    /**
     * 用户所有者 ID。
     */
    @TableField("owner_id")
    private String ownerId;
    /** 首次提问创建会话时使用的幂等键。 */
    @TableField("creation_idempotency_key")
    private String creationIdempotencyKey;
    /** 会话创建时选定且不可变更的 Agent 类型。 */
    @TableField("agent_type")
    private String agentType;
    /**
     * 会话名称。
     */
    private String title;
    /**
     * 创建时间。
     */
    @TableField("created_at")
    private Timestamp createdAt;
    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private Timestamp updatedAt;
    /**
     * 逻辑删除时间。
     */
    @TableField("deleted_at")
    private Timestamp deletedAt;

    /** @return 数据库内部自增主键。 */
    public Long getId() { return id; }
    /** @param value 数据库内部自增主键。 */
    public void setId(final Long value) { this.id = value; }
    /**
     * 返回对外稳定 UUID 标识。
     *
     * @return 对外稳定 UUID 标识。
     */
    public String getPublicId() { return publicId; }
    /**
     * 设置对外稳定 UUID 标识。
     *
     * @param value 对外稳定 UUID 标识。
     */
    public void setPublicId(final String value) { this.publicId = value; }
    /**
     * 返回所属用户 ID。
     *
     * @return 所属用户 ID。
     */
    public String getOwnerId() { return ownerId; }
    /**
     * 设置所属用户 ID。
     *
     * @param ownerId 用户所有者 ID。
     */
    public void setOwnerId(final String ownerId) { this.ownerId = ownerId; }
    /** @return 首次提问创建会话时使用的幂等键。 */
    public String getCreationIdempotencyKey() { return creationIdempotencyKey; }
    /** @param value 首次提问创建会话时使用的幂等键。 */
    public void setCreationIdempotencyKey(final String value) { this.creationIdempotencyKey = value; }
    /** @return 会话创建时选定且不可变更的 Agent 类型。 */
    public String getAgentType() { return agentType; }
    /** @param value 会话创建时选定且不可变更的 Agent 类型。 */
    public void setAgentType(final String value) { this.agentType = value; }
    /**
     * 返回会话名称。
     *
     * @return 会话名称。
     */
    public String getTitle() { return title; }
    /**
     * 设置会话名称。
     *
     * @param title 会话名称。
     */
    public void setTitle(final String title) { this.title = title; }
    /**
     * 返回创建时间。
     *
     * @return 创建时间。
     */
    public Timestamp getCreatedAt() { return copy(createdAt); }
    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间。
     */
    public void setCreatedAt(final Timestamp createdAt) { this.createdAt = copy(createdAt); }
    /**
     * 返回更新时间。
     *
     * @return 更新时间。
     */
    public Timestamp getUpdatedAt() { return copy(updatedAt); }
    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间。
     */
    public void setUpdatedAt(final Timestamp updatedAt) { this.updatedAt = copy(updatedAt); }
    /**
     * 返回逻辑删除时间。
     *
     * @return 逻辑删除时间。
     */
    public Timestamp getDeletedAt() { return copy(deletedAt); }
    /**
     * 设置逻辑删除时间。
     *
     * @param deletedAt 逻辑删除时间。
     */
    public void setDeletedAt(final Timestamp deletedAt) { this.deletedAt = copy(deletedAt); }
    /**
     * 将创建时间转换为 UTC 时间对象。
     *
     * @return 创建时间。
     */
    public Instant createdAtInstant() {
        if (createdAt == null) {
            throw new IllegalStateException("conversation createdAt was not mapped");
        }
        return createdAt.toInstant();
    }
    /**
     * 将更新时间转换为 UTC 时间对象。
     *
     * @return 更新时间。
     */
    public Instant updatedAtInstant() {
        if (updatedAt == null) {
            throw new IllegalStateException("conversation updatedAt was not mapped");
        }
        return updatedAt.toInstant();
    }

    /**
     * 将完整回答复制到系统剪贴板。
     *
     * @param value 输入值。
     *
     * @return 将完整回答复制到系统剪贴板。
     */
    private static Timestamp copy(final Timestamp value) {
        if (value == null) {
            return null;
        }
        final Timestamp result = new Timestamp(value.getTime());
        result.setNanos(value.getNanos());
        return result;
    }
}
