package com.acme.intelligentqa.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 会话中的用户或助手消息，助手消息可携带回答终态。
 */
public final class ChatMessage {

    /**
     * 会话消息发送者角色。
     */
    public enum Role {
        /**
         * 用户提交的消息。
         */
        USER,
        /**
         * 系统生成的回答或追问消息。
         */
        ASSISTANT
    }

    /**
     * 唯一标识。
     */
    private final UUID id;
    /**
     * 会话 ID。
     */
    private final UUID conversationId;
    /**
     * 回答 ID。
     */
    private final UUID answerId;
    /**
     * 回答状态。
     */
    private final AnswerSnapshot.Status answerStatus;
    /**
     * 消息角色。
     */
    private final Role role;
    /**
     * 内容。
     */
    private final String content;
    /**
     * 创建时间。
     */
    private final Instant createdAt;
    /** 随用户消息提交的临时文件元数据快照。 */
    private final List<MessageAttachment> attachments;
    /** 助手消息对应的可恢复执行事件。 */
    private final List<AnswerEvent> executionEvents;

    /**
     * 创建 {@code ChatMessage} 实例。
     *
     * @param id 唯一标识。
     *
     * @param conversationId 会话 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param role 消息角色。
     *
     * @param content 内容。
     *
     * @param createdAt 创建时间。
     */
    public ChatMessage(
            final UUID id,
            final UUID conversationId,
            final UUID answerId,
            final Role role,
            final String content,
            final Instant createdAt) {
        this(id, conversationId, answerId, null, role, content, createdAt,
                Collections.<MessageAttachment>emptyList());
    }

    /**
     * 创建 {@code ChatMessage} 实例。
     *
     * @param id 唯一标识。
     *
     * @param conversationId 会话 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param answerStatus 回答状态。
     *
     * @param role 消息角色。
     *
     * @param content 内容。
     *
     * @param createdAt 创建时间。
     */
    public ChatMessage(
            final UUID id,
            final UUID conversationId,
            final UUID answerId,
            final AnswerSnapshot.Status answerStatus,
            final Role role,
            final String content,
            final Instant createdAt) {
        this(id, conversationId, answerId, answerStatus, role, content, createdAt,
                Collections.<MessageAttachment>emptyList(), Collections.<AnswerEvent>emptyList());
    }

    /**
     * 创建包含附件元数据的 {@code ChatMessage} 实例。
     *
     * @param id 唯一标识。
     * @param conversationId 会话 ID。
     * @param answerId 回答 ID。
     * @param answerStatus 回答状态。
     * @param role 消息角色。
     * @param content 内容。
     * @param createdAt 创建时间。
     * @param attachments 随用户消息提交的附件元数据。
     */
    public ChatMessage(
            final UUID id,
            final UUID conversationId,
            final UUID answerId,
            final AnswerSnapshot.Status answerStatus,
            final Role role,
            final String content,
            final Instant createdAt,
            final List<MessageAttachment> attachments) {
        this(id, conversationId, answerId, answerStatus, role, content, createdAt,
                attachments, Collections.<AnswerEvent>emptyList());
    }

    /**
     * 创建包含附件和执行事件的 {@code ChatMessage} 实例。
     *
     * @param id 唯一标识。
     * @param conversationId 会话 ID。
     * @param answerId 回答 ID。
     * @param answerStatus 回答状态。
     * @param role 消息角色。
     * @param content 内容。
     * @param createdAt 创建时间。
     * @param attachments 随用户消息提交的附件元数据。
     * @param executionEvents 助手消息对应的可恢复执行事件。
     */
    public ChatMessage(
            final UUID id,
            final UUID conversationId,
            final UUID answerId,
            final AnswerSnapshot.Status answerStatus,
            final Role role,
            final String content,
            final Instant createdAt,
            final List<MessageAttachment> attachments,
            final List<AnswerEvent> executionEvents) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
        this.answerId = answerId;
        this.answerStatus = answerStatus;
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.attachments = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(attachments, "attachments must not be null")));
        this.executionEvents = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(executionEvents, "executionEvents must not be null")));
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
     * 返回会话 ID。
     *
     * @return 会话 ID。
     */
    public UUID conversationId() {
        return conversationId;
    }

    /**
     * 返回回答 ID。
     *
     * @return 回答 ID。
     */
    public UUID answerId() {
        return answerId;
    }

    /**
     * 返回回答状态。
     *
     * @return 回答状态。
     */
    public AnswerSnapshot.Status answerStatus() {
        return answerStatus;
    }

    /**
     * 返回消息角色。
     *
     * @return 消息角色。
     */
    public Role role() {
        return role;
    }

    /**
     * 返回内容。
     *
     * @return 内容。
     */
    public String content() {
        return content;
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
     * 返回随该用户消息提交的附件元数据；助手消息通常为空列表。
     *
     * @return 不可变附件元数据列表。
     */
    public List<MessageAttachment> attachments() {
        return attachments;
    }

    /**
     * 返回助手消息对应的持久化执行事件。
     *
     * @return 不可变执行事件列表。
     */
    public List<AnswerEvent> executionEvents() { return executionEvents; }
}
