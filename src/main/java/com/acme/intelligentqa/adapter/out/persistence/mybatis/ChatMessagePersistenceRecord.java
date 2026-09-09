package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import java.time.Instant;

/** 与 {@code qa_message} 表对应的持久化记录。 */
@TableName("qa_message")
public class ChatMessagePersistenceRecord {

    /**
     * 唯一标识。
     */
    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 会话 ID。
     */
    @TableField("conversation_id")
    private String conversationId;
    /**
     * 回答 ID。
     */
    @TableField("answer_id")
    private String answerId;
    /**
     * 回答状态。
     */
    @TableField(exist = false)
    private String answerStatus;
    /**
     * 消息角色。
     */
    private String role;
    /**
     * 内容。
     */
    private String content;
    /**
     * 创建时间。
     */
    @TableField("created_at")
    private Timestamp createdAt;

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
     * 返回会话 ID。
     *
     * @return 会话 ID。
     */
    public String getConversationId() { return conversationId; }
    /**
     * 设置会话 ID。
     *
     * @param conversationId 会话 ID。
     */
    public void setConversationId(final String conversationId) { this.conversationId = conversationId; }
    /**
     * 返回回答 ID。
     *
     * @return 回答 ID。
     */
    public String getAnswerId() { return answerId; }
    /**
     * 设置回答 ID。
     *
     * @param answerId 回答 ID。
     */
    public void setAnswerId(final String answerId) { this.answerId = answerId; }
    /**
     * 返回回答持久化状态。
     *
     * @return 回答持久化状态。
     */
    public String getAnswerStatus() { return answerStatus; }
    /**
     * 设置回答持久化状态。
     *
     * @param answerStatus 回答状态。
     */
    public void setAnswerStatus(final String answerStatus) { this.answerStatus = answerStatus; }
    /**
     * 返回消息角色。
     *
     * @return 消息角色。
     */
    public String getRole() { return role; }
    /**
     * 设置消息角色。
     *
     * @param role 消息角色。
     */
    public void setRole(final String role) { this.role = role; }
    /**
     * 返回回答或消息内容。
     *
     * @return 回答或消息内容。
     */
    public String getContent() { return content; }
    /**
     * 设置回答或消息内容。
     *
     * @param content 内容。
     */
    public void setContent(final String content) { this.content = content; }
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
     * 将创建时间转换为 UTC 时间对象。
     *
     * @return 创建时间。
     */
    public Instant createdAtInstant() {
        if (createdAt == null) {
            throw new IllegalStateException("message createdAt was not mapped");
        }
        return createdAt.toInstant();
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
