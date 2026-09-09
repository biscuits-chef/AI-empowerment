package com.acme.intelligentqa.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 回答在某一时刻的完整持久化快照，包含内容、状态和错误信息。
 */
public final class AnswerSnapshot {

    /** 回答从创建到结束所处的业务状态。 */
    public enum Status {
        /**
         * 已持久化受理，等待生成。
         */
        PENDING,
        /**
         * 正在检索知识库证据。
         */
        RETRIEVING,
        /**
         * 正在读取受控业务数据。
         */
        QUERYING,
        /**
         * 正在生成回答文本。
         */
        GENERATING,
        /**
         * 等待用户补充信息的追问终态。
         */
        NEEDS_CLARIFICATION,
        /**
         * 回答生成成功并已保存完整内容。
         */
        COMPLETED,
        /**
         * 未产生有效回答即失败。
         */
        FAILED,
        /**
         * 生成中断但已保留部分回答内容。
         */
        INCOMPLETE,
        /**
         * 已收到停止请求，等待停止任务收敛。
         */
        CANCEL_REQUESTED,
        /**
         * 回答已停止。
         */
        CANCELLED,
        /**
         * 停止任务最终失败。
         */
        CANCEL_FAILED
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
     * 问题 ID。
     */
    private final UUID questionId;
    /**
     * 链路追踪 ID。
     */
    private final UUID traceId;
    /**
     * 原回答 ID。
     */
    private final UUID regeneratedFromAnswerId;
    /**
     * 业务状态。
     */
    private final Status status;
    /**
     * 内容。
     */
    private final String content;
    /**
     * 错误码。
     */
    private final String errorCode;
    /**
     * 创建时间。
     */
    private final Instant createdAt;
    /**
     * 完成时间。
     */
    private final Instant completedAt;
    /**
     * 停止原因。
     */
    private final String cancelReason;
    /**
     * 停止时所处阶段。
     */
    private final String cancelledStage;
    /**
     * 停止失败错误码。
     */
    private final String cancelErrorCode;
    /**
     * 停止请求时间。
     */
    private final Instant cancelRequestedAt;
    /**
     * 停止完成时间。
     */
    private final Instant cancelledAt;

    /**
     * 创建 {@code AnswerSnapshot} 实例。
     *
     * @param id 唯一标识。
     *
     * @param conversationId 会话 ID。
     *
     * @param questionId 问题 ID。
     *
     * @param traceId 链路追踪 ID。
     *
     * @param regeneratedFromAnswerId 原回答 ID。
     *
     * @param status 业务状态。
     *
     * @param content 内容。
     *
     * @param errorCode 错误码。
     *
     * @param createdAt 创建时间。
     *
     * @param completedAt 完成时间。
     */
    public AnswerSnapshot(
            final UUID id,
            final UUID conversationId,
            final UUID questionId,
            final UUID traceId,
            final UUID regeneratedFromAnswerId,
            final Status status,
            final String content,
            final String errorCode,
            final Instant createdAt,
            final Instant completedAt) {
        this(id, conversationId, questionId, traceId, regeneratedFromAnswerId, status,
                content, errorCode, createdAt, completedAt, null, null, null, null, null);
    }

    /**
     * 创建 {@code AnswerSnapshot} 实例。
     *
     * @param id 唯一标识。
     *
     * @param conversationId 会话 ID。
     *
     * @param questionId 问题 ID。
     *
     * @param traceId 链路追踪 ID。
     *
     * @param regeneratedFromAnswerId 原回答 ID。
     *
     * @param status 业务状态。
     *
     * @param content 内容。
     *
     * @param errorCode 错误码。
     *
     * @param createdAt 创建时间。
     *
     * @param completedAt 完成时间。
     *
     * @param cancelReason 停止原因。
     *
     * @param cancelledStage 停止时所处阶段。
     *
     * @param cancelErrorCode 停止失败错误码。
     *
     * @param cancelRequestedAt 停止请求时间。
     *
     * @param cancelledAt 停止完成时间。
     */
    public AnswerSnapshot(
            final UUID id,
            final UUID conversationId,
            final UUID questionId,
            final UUID traceId,
            final UUID regeneratedFromAnswerId,
            final Status status,
            final String content,
            final String errorCode,
            final Instant createdAt,
            final Instant completedAt,
            final String cancelReason,
            final String cancelledStage,
            final String cancelErrorCode,
            final Instant cancelRequestedAt,
            final Instant cancelledAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
        this.questionId = Objects.requireNonNull(questionId, "questionId must not be null");
        this.traceId = Objects.requireNonNull(traceId, "traceId must not be null");
        this.regeneratedFromAnswerId = regeneratedFromAnswerId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.content = content == null ? "" : content;
        this.errorCode = errorCode;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.completedAt = completedAt;
        this.cancelReason = cancelReason;
        this.cancelledStage = cancelledStage;
        this.cancelErrorCode = cancelErrorCode;
        this.cancelRequestedAt = cancelRequestedAt;
        this.cancelledAt = cancelledAt;
    }

    /**
     * 处理唯一标识。
     *
     * @return 唯一标识。
     */
    public UUID id() { return id; }
    /**
     * 返回会话 ID。
     *
     * @return 会话 ID。
     */
    public UUID conversationId() { return conversationId; }
    /**
     * 返回问题 ID。
     *
     * @return 问题 ID。
     */
    public UUID questionId() { return questionId; }
    /**
     * 返回链路追踪 ID。
     *
     * @return 链路追踪 ID。
     */
    public UUID traceId() { return traceId; }
    /**
     * 返回原回答 ID。
     *
     * @return 原回答 ID。
     */
    public UUID regeneratedFromAnswerId() { return regeneratedFromAnswerId; }
    /**
     * 返回业务状态。
     *
     * @return 业务状态。
     */
    public Status status() { return status; }
    /**
     * 返回内容。
     *
     * @return 内容。
     */
    public String content() { return content; }
    /**
     * 返回错误码。
     *
     * @return 错误码。
     */
    public String errorCode() { return errorCode; }
    /**
     * 返回创建时间。
     *
     * @return 创建时间。
     */
    public Instant createdAt() { return createdAt; }
    /**
     * 返回完成时间。
     *
     * @return 完成时间。
     */
    public Instant completedAt() { return completedAt; }
    /**
     * 返回停止原因。
     *
     * @return 停止原因。
     */
    public String cancelReason() { return cancelReason; }
    /**
     * 返回停止时所处阶段。
     *
     * @return 停止时所处阶段。
     */
    public String cancelledStage() { return cancelledStage; }
    /**
     * 返回停止失败错误码。
     *
     * @return 停止失败错误码。
     */
    public String cancelErrorCode() { return cancelErrorCode; }
    /**
     * 返回停止请求时间。
     *
     * @return 停止请求时间。
     */
    public Instant cancelRequestedAt() { return cancelRequestedAt; }
    /**
     * 返回停止完成时间。
     *
     * @return 停止完成时间。
     */
    public Instant cancelledAt() { return cancelledAt; }
}
