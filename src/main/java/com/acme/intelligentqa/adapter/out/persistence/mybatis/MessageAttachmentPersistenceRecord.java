package com.acme.intelligentqa.adapter.out.persistence.mybatis;

/**
 * 承载历史用户消息与临时文件元数据联查结果。
 */
public class MessageAttachmentPersistenceRecord {

    /** 用户消息唯一标识。 */
    private String messageId;
    /** 文件唯一标识。 */
    private String fileId;
    /** 安全展示文件名。 */
    private String originalName;
    /** 服务端识别的内容类型。 */
    private String contentType;
    /** 文件字节数。 */
    private Long sizeBytes;
    /** 文件在该次问题中的使用角色。 */
    private String usageType;
    /** 文件当前处理状态。 */
    private String status;

    /** @return 用户消息唯一标识。 */
    public String getMessageId() { return messageId; }
    /** @param value 用户消息唯一标识。 */
    public void setMessageId(final String value) { this.messageId = value; }
    /** @return 文件唯一标识。 */
    public String getFileId() { return fileId; }
    /** @param value 文件唯一标识。 */
    public void setFileId(final String value) { this.fileId = value; }
    /** @return 安全展示文件名。 */
    public String getOriginalName() { return originalName; }
    /** @param value 安全展示文件名。 */
    public void setOriginalName(final String value) { this.originalName = value; }
    /** @return 服务端识别的内容类型。 */
    public String getContentType() { return contentType; }
    /** @param value 服务端识别的内容类型。 */
    public void setContentType(final String value) { this.contentType = value; }
    /** @return 文件字节数。 */
    public Long getSizeBytes() { return sizeBytes; }
    /** @param value 文件字节数。 */
    public void setSizeBytes(final Long value) { this.sizeBytes = value; }
    /** @return 文件在该次问题中的使用角色。 */
    public String getUsageType() { return usageType; }
    /** @param value 文件在该次问题中的使用角色。 */
    public void setUsageType(final String value) { this.usageType = value; }
    /** @return 文件当前处理状态。 */
    public String getStatus() { return status; }
    /** @param value 文件当前处理状态。 */
    public void setStatus(final String value) { this.status = value; }
}
