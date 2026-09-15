package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * 与 {@code qa_file} 表对应的临时文件持久化记录。
 */
public class TemporaryFilePersistenceRecord {

    /** 数据库内部自增主键。 */
    private Long id;
    /** 对外稳定文件 UUID 标识。 */
    private String publicId;
    /** 所属会话 ID。 */
    private String conversationId;
    /** 用户所有者 ID。 */
    private String ownerId;
    /** 上传幂等键。 */
    private String idempotencyKey;
    /** 安全展示文件名。 */
    private String originalName;
    /** 规范内容类型。 */
    private String contentType;
    /** 文件字节数。 */
    private Long sizeBytes;
    /** 文件内容摘要。 */
    private String sha256;
    /** 服务端对象键。 */
    private String objectKey;
    /** 文件使用角色。 */
    private String usageType;
    /** 文件处理状态。 */
    private String status;
    /** 创建时间。 */
    private Timestamp createdAt;
    /** 更新时间。 */
    private Timestamp updatedAt;
    /** 逻辑删除时间。 */
    private Timestamp deletedAt;

    /** @return 数据库内部自增主键。 */
    public Long getId() { return id; }
    /** @param value 数据库内部自增主键。 */
    public void setId(final Long value) { this.id = value; }
    /** @return 对外稳定文件 UUID 标识。 */
    public String getPublicId() { return publicId; }
    /** @param value 对外稳定文件 UUID 标识。 */
    public void setPublicId(final String value) { this.publicId = value; }
    /** @return 所属会话 ID。 */
    public String getConversationId() { return conversationId; }
    /** @param value 所属会话 ID。 */
    public void setConversationId(final String value) { this.conversationId = value; }
    /** @return 用户所有者 ID。 */
    public String getOwnerId() { return ownerId; }
    /** @param value 用户所有者 ID。 */
    public void setOwnerId(final String value) { this.ownerId = value; }
    /** @return 上传幂等键。 */
    public String getIdempotencyKey() { return idempotencyKey; }
    /** @param value 上传幂等键。 */
    public void setIdempotencyKey(final String value) { this.idempotencyKey = value; }
    /** @return 安全展示文件名。 */
    public String getOriginalName() { return originalName; }
    /** @param value 安全展示文件名。 */
    public void setOriginalName(final String value) { this.originalName = value; }
    /** @return 规范内容类型。 */
    public String getContentType() { return contentType; }
    /** @param value 规范内容类型。 */
    public void setContentType(final String value) { this.contentType = value; }
    /** @return 文件字节数。 */
    public Long getSizeBytes() { return sizeBytes; }
    /** @param value 文件字节数。 */
    public void setSizeBytes(final Long value) { this.sizeBytes = value; }
    /** @return 文件内容摘要。 */
    public String getSha256() { return sha256; }
    /** @param value 文件内容摘要。 */
    public void setSha256(final String value) { this.sha256 = value; }
    /** @return 服务端对象键。 */
    public String getObjectKey() { return objectKey; }
    /** @param value 服务端对象键。 */
    public void setObjectKey(final String value) { this.objectKey = value; }
    /** @return 文件使用角色。 */
    public String getUsageType() { return usageType; }
    /** @param value 文件使用角色。 */
    public void setUsageType(final String value) { this.usageType = value; }
    /** @return 文件处理状态。 */
    public String getStatus() { return status; }
    /** @param value 文件处理状态。 */
    public void setStatus(final String value) { this.status = value; }
    /** @return 创建时间。 */
    public Timestamp getCreatedAt() { return copy(createdAt); }
    /** @param value 创建时间。 */
    public void setCreatedAt(final Timestamp value) { this.createdAt = copy(value); }
    /** @return 更新时间。 */
    public Timestamp getUpdatedAt() { return copy(updatedAt); }
    /** @param value 更新时间。 */
    public void setUpdatedAt(final Timestamp value) { this.updatedAt = copy(value); }
    /** @return 逻辑删除时间。 */
    public Timestamp getDeletedAt() { return copy(deletedAt); }
    /** @param value 逻辑删除时间。 */
    public void setDeletedAt(final Timestamp value) { this.deletedAt = copy(value); }

    /**
     * 将创建时间转换为 UTC 时间对象。
     *
     * @return 创建时间。
     */
    public Instant createdAtInstant() { return requiredInstant(createdAt, "createdAt"); }

    /**
     * 将更新时间转换为 UTC 时间对象。
     *
     * @return 更新时间。
     */
    public Instant updatedAtInstant() { return requiredInstant(updatedAt, "updatedAt"); }

    /**
     * 复制可变数据库时间对象。
     *
     * @param value 数据库时间对象。
     * @return 独立副本。
     */
    private static Timestamp copy(final Timestamp value) {
        if (value == null) {
            return null;
        }
        final Timestamp result = new Timestamp(value.getTime());
        result.setNanos(value.getNanos());
        return result;
    }

    /**
     * 将数据库时间转换为必填 UTC 时间。
     *
     * @param value 数据库时间对象。
     * @param field 字段名称。
     * @return UTC 时间对象。
     */
    private static Instant requiredInstant(final Timestamp value, final String field) {
        if (value == null) {
            throw new IllegalStateException("temporary file " + field + " was not mapped");
        }
        return value.toInstant();
    }
}
