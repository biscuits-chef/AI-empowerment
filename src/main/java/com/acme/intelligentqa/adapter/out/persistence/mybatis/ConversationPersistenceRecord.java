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

    /**
     * 唯一标识。
     */
    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 用户所有者 ID。
     */
    @TableField("owner_id")
    private String ownerId;
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

    /**
     * 返回唯一标识。
     *
     * @return 唯一标识。
     */
    public String getId() { return id; }
    /**
     * 设置唯一标识。
     *
     * @param id 唯一标识。
     */
    public void setId(final String id) { this.id = id; }
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
