package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import java.time.Instant;

/** 与 {@code qa_answer} 表对应的持久化记录。 */
@TableName("qa_answer")
public class AnswerPersistenceRecord {

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
     * 问题 ID。
     */
    @TableField("question_id")
    private String questionId;
    /**
     * 用户所有者 ID。
     */
    @TableField("owner_id")
    private String ownerId;
    /**
     * 幂等键。
     */
    @TableField("idempotency_key")
    private String idempotencyKey;
    /**
     * 链路追踪 ID。
     */
    @TableField("trace_id")
    private String traceId;
    /**
     * 原回答 ID。
     */
    @TableField("regenerated_from_answer_id")
    private String regeneratedFromAnswerId;
    /**
     * 业务状态。
     */
    private String status;
    /**
     * 内容。
     */
    private String content;
    /**
     * 错误码。
     */
    @TableField("error_code")
    private String errorCode;
    /**
     * 创建时间。
     */
    @TableField("created_at")
    private Timestamp createdAt;
    /**
     * 完成时间。
     */
    @TableField("completed_at")
    private Timestamp completedAt;
    /**
     * 停止原因。
     */
    @TableField("cancel_reason")
    private String cancelReason;
    /**
     * 停止时所处阶段。
     */
    @TableField("cancelled_stage")
    private String cancelledStage;
    /**
     * 公司模型侧消息 ID。
     */
    @TableField("provider_message_id")
    private String providerMessageId;
    /**
     * 停止失败错误码。
     */
    @TableField("cancel_error_code")
    private String cancelErrorCode;
    /**
     * 停止请求时间。
     */
    @TableField("cancel_requested_at")
    private Timestamp cancelRequestedAt;
    /**
     * 停止完成时间。
     */
    @TableField("cancelled_at")
    private Timestamp cancelledAt;

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
     * 返回问题 ID。
     *
     * @return 问题 ID。
     */
    public String getQuestionId() { return questionId; }
    /**
     * 设置问题 ID。
     *
     * @param questionId 问题 ID。
     */
    public void setQuestionId(final String questionId) { this.questionId = questionId; }
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
     * 返回请求幂等键。
     *
     * @return 请求幂等键。
     */
    public String getIdempotencyKey() { return idempotencyKey; }
    /**
     * 设置请求幂等键。
     *
     * @param idempotencyKey 幂等键。
     */
    public void setIdempotencyKey(final String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    /**
     * 返回链路追踪 ID。
     *
     * @return 链路追踪 ID。
     */
    public String getTraceId() { return traceId; }
    /**
     * 设置链路追踪 ID。
     *
     * @param traceId 链路追踪 ID。
     */
    public void setTraceId(final String traceId) { this.traceId = traceId; }
    /**
     * 返回此次重生成对应的原回答 ID。
     *
     * @return 此次重生成对应的原回答 ID。
     */
    public String getRegeneratedFromAnswerId() { return regeneratedFromAnswerId; }
    /**
     * 设置此次重生成对应的原回答 ID。
     *
     * @param value 输入值。
     */
    public void setRegeneratedFromAnswerId(final String value) { this.regeneratedFromAnswerId = value; }
    /**
     * 返回HTTP 或回答状态。
     *
     * @return HTTP 或回答状态。
     */
    public String getStatus() { return status; }
    /**
     * 设置HTTP 或回答状态。
     *
     * @param status 业务状态。
     */
    public void setStatus(final String status) { this.status = status; }
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
     * 返回错误码。
     *
     * @return 错误码。
     */
    public String getErrorCode() { return errorCode; }
    /**
     * 设置错误码。
     *
     * @param errorCode 错误码。
     */
    public void setErrorCode(final String errorCode) { this.errorCode = errorCode; }
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
     * 返回回答终态时间。
     *
     * @return 回答终态时间。
     */
    public Timestamp getCompletedAt() { return copy(completedAt); }
    /**
     * 设置回答终态时间。
     *
     * @param completedAt 完成时间。
     */
    public void setCompletedAt(final Timestamp completedAt) { this.completedAt = copy(completedAt); }
    /**
     * 返回用户停止回答的原因。
     *
     * @return 用户停止回答的原因。
     */
    public String getCancelReason() { return cancelReason; }
    /**
     * 设置用户停止回答的原因。
     *
     * @param cancelReason 停止原因。
     */
    public void setCancelReason(final String cancelReason) { this.cancelReason = cancelReason; }
    /**
     * 返回停止时所处的生成阶段。
     *
     * @return 停止时所处的生成阶段。
     */
    public String getCancelledStage() { return cancelledStage; }
    /**
     * 设置停止时所处的生成阶段。
     *
     * @param cancelledStage 停止时所处阶段。
     */
    public void setCancelledStage(final String cancelledStage) { this.cancelledStage = cancelledStage; }
    /**
     * 返回公司模型侧消息 ID。
     *
     * @return 公司模型侧消息 ID。
     */
    public String getProviderMessageId() { return providerMessageId; }
    /**
     * 设置公司模型侧消息 ID。
     *
     * @param value 输入值。
     */
    public void setProviderMessageId(final String value) { this.providerMessageId = value; }
    /**
     * 返回停止失败错误码。
     *
     * @return 停止失败错误码。
     */
    public String getCancelErrorCode() { return cancelErrorCode; }
    /**
     * 设置停止失败错误码。
     *
     * @param cancelErrorCode 停止失败错误码。
     */
    public void setCancelErrorCode(final String cancelErrorCode) { this.cancelErrorCode = cancelErrorCode; }
    /**
     * 返回停止请求时间。
     *
     * @return 停止请求时间。
     */
    public Timestamp getCancelRequestedAt() { return copy(cancelRequestedAt); }
    /**
     * 设置停止请求时间。
     *
     * @param value 输入值。
     */
    public void setCancelRequestedAt(final Timestamp value) { this.cancelRequestedAt = copy(value); }
    /**
     * 返回停止完成时间。
     *
     * @return 停止完成时间。
     */
    public Timestamp getCancelledAt() { return copy(cancelledAt); }
    /**
     * 设置停止完成时间。
     *
     * @param cancelledAt 停止完成时间。
     */
    public void setCancelledAt(final Timestamp cancelledAt) { this.cancelledAt = copy(cancelledAt); }
    /**
     * 将创建时间转换为 UTC 时间对象。
     *
     * @return 创建时间。
     */
    public Instant createdAtInstant() {
        if (createdAt == null) {
            throw new IllegalStateException("answer createdAt was not mapped");
        }
        return createdAt.toInstant();
    }
    /**
     * 将回答终态时间转换为 UTC 时间对象。
     *
     * @return 回答终态时间。
     */
    public Instant completedAtInstant() { return completedAt == null ? null : completedAt.toInstant(); }
    /**
     * 将停止请求时间转换为 UTC 时间对象。
     *
     * @return 停止请求时间。
     */
    public Instant cancelRequestedAtInstant() {
        return cancelRequestedAt == null ? null : cancelRequestedAt.toInstant();
    }
    /**
     * 将停止完成时间转换为 UTC 时间对象。
     *
     * @return 停止完成时间。
     */
    public Instant cancelledAtInstant() { return cancelledAt == null ? null : cancelledAt.toInstant(); }

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
