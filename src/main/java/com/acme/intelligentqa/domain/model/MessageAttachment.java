package com.acme.intelligentqa.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * 表示随某一条用户消息持久化展示的临时文件元数据快照。
 */
public final class MessageAttachment {

    /** 文件唯一标识。 */
    private final UUID fileId;
    /** 安全展示文件名。 */
    private final String name;
    /** 服务端识别的内容类型。 */
    private final String contentType;
    /** 文件字节数。 */
    private final long sizeBytes;
    /** 文件在该次问题中的使用角色。 */
    private final TemporaryFile.Usage usage;
    /** 文件当前处理状态。 */
    private final TemporaryFile.Status status;

    /**
     * 创建用户消息附件元数据快照。
     *
     * @param fileId 文件唯一标识。
     * @param name 安全展示文件名。
     * @param contentType 服务端识别的内容类型。
     * @param sizeBytes 文件字节数。
     * @param usage 文件在该次问题中的使用角色。
     * @param status 文件当前处理状态。
     */
    public MessageAttachment(
            final UUID fileId,
            final String name,
            final String contentType,
            final long sizeBytes,
            final TemporaryFile.Usage usage,
            final TemporaryFile.Status status) {
        this.fileId = Objects.requireNonNull(fileId, "fileId must not be null");
        this.name = requireText(name, "name");
        this.contentType = requireText(contentType, "contentType");
        if (sizeBytes <= 0L) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        this.sizeBytes = sizeBytes;
        this.usage = Objects.requireNonNull(usage, "usage must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /** @return 文件唯一标识。 */
    public UUID fileId() { return fileId; }
    /** @return 安全展示文件名。 */
    public String name() { return name; }
    /** @return 服务端识别的内容类型。 */
    public String contentType() { return contentType; }
    /** @return 文件字节数。 */
    public long sizeBytes() { return sizeBytes; }
    /** @return 文件在该次问题中的使用角色。 */
    public TemporaryFile.Usage usage() { return usage; }
    /** @return 文件当前处理状态。 */
    public TemporaryFile.Status status() { return status; }

    /**
     * 校验文本字段非空并返回规范值。
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
}
