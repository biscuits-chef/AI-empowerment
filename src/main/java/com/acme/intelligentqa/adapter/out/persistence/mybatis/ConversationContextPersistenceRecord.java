package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import java.sql.Timestamp;

/** 与 {@code qa_conversation_context} 表对应的持久化记录。 */
public class ConversationContextPersistenceRecord {

    /** 数据库内部自增主键。 */
    private Long id;
    /**
     * 会话 ID。
     */
    private String conversationId;
    /** @return 数据库内部自增主键。 */
    public Long getId() { return id; }
    /** @param value 数据库内部自增主键。 */
    public void setId(final Long value) { this.id = value; }
    /**
     * 用户所有者 ID。
     */
    private String ownerId;
    /**
     * 乐观锁版本号。
     */
    private Long version;
    /**
     * 结构化上下文 JSON。
     */
    private String stateJson;
    /**
     * 来源问题 ID。
     */
    private String sourceQuestionId;
    /**
     * 更新时间。
     */
    private Timestamp updatedAt;

    /**
     * 返回会话 ID。
     *
     * @return 会话 ID。
     */
    public String getConversationId() { return conversationId; }
    /**
     * 设置会话 ID。
     *
     * @param value 输入值。
     */
    public void setConversationId(final String value) { this.conversationId = value; }
    /**
     * 返回所属用户 ID。
     *
     * @return 所属用户 ID。
     */
    public String getOwnerId() { return ownerId; }
    /**
     * 设置所属用户 ID。
     *
     * @param value 输入值。
     */
    public void setOwnerId(final String value) { this.ownerId = value; }
    /**
     * 返回乐观锁版本号。
     *
     * @return 乐观锁版本号。
     */
    public Long getVersion() { return version; }
    /**
     * 设置乐观锁版本号。
     *
     * @param value 输入值。
     */
    public void setVersion(final Long value) { this.version = value; }
    /**
     * 返回结构化上下文 JSON。
     *
     * @return 结构化上下文 JSON。
     */
    public String getStateJson() { return stateJson; }
    /**
     * 设置结构化上下文 JSON。
     *
     * @param value 输入值。
     */
    public void setStateJson(final String value) { this.stateJson = value; }
    /**
     * 返回上下文来源问题 ID。
     *
     * @return 上下文来源问题 ID。
     */
    public String getSourceQuestionId() { return sourceQuestionId; }
    /**
     * 设置上下文来源问题 ID。
     *
     * @param value 输入值。
     */
    public void setSourceQuestionId(final String value) { this.sourceQuestionId = value; }
    /**
     * 返回更新时间。
     *
     * @return 更新时间。
     */
    public Timestamp getUpdatedAt() { return updatedAt == null ? null : new Timestamp(updatedAt.getTime()); }
    /**
     * 设置更新时间。
     *
     * @param value 输入值。
     */
    public void setUpdatedAt(final Timestamp value) {
        this.updatedAt = value == null ? null : new Timestamp(value.getTime());
    }
}
