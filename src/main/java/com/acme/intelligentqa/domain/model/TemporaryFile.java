package com.acme.intelligentqa.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 表示会话内上传且不会进入公司知识库的临时文件。
 */
public final class TemporaryFile {

    /** 文件唯一标识。 */
    private final UUID id;
    /** 所属会话 ID。 */
    private final UUID conversationId;
    /** 用户所有者 ID。 */
    private final String ownerId;
    /** 经安全净化后用于展示的原始文件名。 */
    private final String originalName;
    /** 服务端识别的内容类型。 */
    private final String contentType;
    /** 文件字节数。 */
    private final long sizeBytes;
    /** 文件内容的 SHA-256 摘要。 */
    private final String sha256;
    /** 服务端生成的对象存储键。 */
    private final String objectKey;
    /** 文件在问题中的使用角色。 */
    private final Usage usage;
    /** 文件处理状态。 */
    private final Status status;
    /** 创建时间。 */
    private final Instant createdAt;
    /** 更新时间。 */
    private final Instant updatedAt;

    /**
     * 创建临时文件领域对象。
     *
     * @param id 文件唯一标识。
     * @param conversationId 所属会话 ID。
     * @param ownerId 用户所有者 ID。
     * @param originalName 用于安全展示的文件名。
     * @param contentType 服务端识别的内容类型。
     * @param sizeBytes 文件字节数。
     * @param sha256 文件内容摘要。
     * @param objectKey 服务端对象存储键。
     * @param usage 文件使用角色。
     * @param status 文件处理状态。
     * @param createdAt 创建时间。
     * @param updatedAt 更新时间。
     */
    public TemporaryFile(
            final UUID id,
            final UUID conversationId,
            final String ownerId,
            final String originalName,
            final String contentType,
            final long sizeBytes,
            final String sha256,
            final String objectKey,
            final Usage usage,
            final Status status,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
        this.ownerId = requireText(ownerId, "ownerId");
        this.originalName = requireText(originalName, "originalName");
        this.contentType = requireText(contentType, "contentType");
        if (sizeBytes <= 0L) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        this.sizeBytes = sizeBytes;
        this.sha256 = requireText(sha256, "sha256");
        this.objectKey = requireText(objectKey, "objectKey");
        this.usage = Objects.requireNonNull(usage, "usage must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /** @return 文件唯一标识。 */
    public UUID id() { return id; }
    /** @return 所属会话 ID。 */
    public UUID conversationId() { return conversationId; }
    /** @return 用户所有者 ID。 */
    public String ownerId() { return ownerId; }
    /** @return 用于安全展示的文件名。 */
    public String originalName() { return originalName; }
    /** @return 服务端识别的内容类型。 */
    public String contentType() { return contentType; }
    /** @return 文件字节数。 */
    public long sizeBytes() { return sizeBytes; }
    /** @return 文件内容摘要。 */
    public String sha256() { return sha256; }
    /** @return 服务端对象存储键。 */
    public String objectKey() { return objectKey; }
    /** @return 文件使用角色。 */
    public Usage usage() { return usage; }
    /** @return 文件处理状态。 */
    public Status status() { return status; }
    /** @return 创建时间。 */
    public Instant createdAt() { return createdAt; }
    /** @return 更新时间。 */
    public Instant updatedAt() { return updatedAt; }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验文本。
     * @param field 字段名称。
     * @return 去除首尾空白后的文本。
     */
    private static String requireText(final String value, final String field) {
        Objects.requireNonNull(value, field + " must not be null");
        final String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    /**
     * 文件在一次问题中的业务角色。
     */
    public enum Usage {
        /** 由后续处理流程自动识别角色。 */
        AUTO,
        /** 作为批量编号等结构化查询参数。 */
        QUERY_INPUT,
        /** 作为需要核验和引用的用户临时证据。 */
        EVIDENCE
    }

    /**
     * 临时文件生命周期状态。
     */
    public enum Status {
        /** 元数据已建立且对象正在写入。 */
        UPLOADING,
        /** 对象已保存，等待安全扫描、解析或 OCR。 */
        STORED,
        /** 文件已完成当前环境要求的处理，可以随问题提交。 */
        READY,
        /** 文件上传或处理失败。 */
        FAILED,
        /** 文件已经逻辑删除，等待物理清理。 */
        DELETE_PENDING
    }
}
