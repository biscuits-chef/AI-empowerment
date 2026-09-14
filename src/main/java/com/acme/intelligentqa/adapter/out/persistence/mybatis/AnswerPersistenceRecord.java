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

    /** 数据库内部自增主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 对外稳定 UUID 标识。
     */
    @TableField("public_id")
    private String publicId;
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
     * 公司 HiAgent 应用会话 ID。
     */
    @TableField("app_conversation_id")
    private String appConversationId;
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
    @TableField("message_id")
    private String messageId;
    /**
     * 公司 HiAgent 查询 ID。
     */
    @TableField("query_id")
    private String queryId;
    /**
     * 公司 HiAgent 任务 ID。
     */
    @TableField("task_id")
    private String taskId;
    /**
     * 公司 HiAgent 回答消耗的令牌总数。
     */
    @TableField("total_tokens")
    private Integer totalTokens;
    /**
     * 公司 HiAgent 回答耗时，单位为秒。
     */
    private Double latency;
    /**
     * 公司 HiAgent 链路追踪 JSON 字符串。
     */
    @TableField("tracing_json_str")
    private String tracingJsonStr;
    /**
     * 公司 HiAgent 意图识别 JSON 字符串。
     */
    @TableField("intention_json_str")
    private String intentionJsonStr;
    /**
     * 公司 HiAgent 回答是否使用了检索资源。
     */
    @TableField("retriever_resource")
    private Boolean retrieverResource;
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

    /** @return 数据库内部自增主键。 */
    public Long getId() { return id; }
    /** @param value 数据库内部自增主键。 */
    public void setId(final Long value) { this.id = value; }
    /**
     * 返回对外稳定 UUID 标识。
     *
     * @return 对外稳定 UUID 标识。
     */
    public String getPublicId() { return publicId; }
    /**
     * 设置对外稳定 UUID 标识。
     *
     * @param value 对外稳定 UUID 标识。
     */
    public void setPublicId(final String value) { this.publicId = value; }
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
     * 返回公司 HiAgent 应用会话 ID。
     *
     * @return 公司 HiAgent 应用会话 ID。
     */
    public String getAppConversationId() { return appConversationId; }
    /**
     * 设置公司 HiAgent 应用会话 ID。
     *
     * @param value 公司 HiAgent 应用会话 ID。
     */
    public void setAppConversationId(final String value) { this.appConversationId = value; }
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
    public String getMessageId() { return messageId; }
    /**
     * 设置公司模型侧消息 ID。
     *
     * @param value 输入值。
     */
    public void setMessageId(final String value) { this.messageId = value; }
    /**
     * 返回公司 HiAgent 查询 ID。
     *
     * @return 公司 HiAgent 查询 ID。
     */
    public String getQueryId() { return queryId; }
    /**
     * 设置公司 HiAgent 查询 ID。
     *
     * @param value 公司 HiAgent 查询 ID。
     */
    public void setQueryId(final String value) { this.queryId = value; }
    /**
     * 返回公司 HiAgent 任务 ID。
     *
     * @return 公司 HiAgent 任务 ID。
     */
    public String getTaskId() { return taskId; }
    /**
     * 设置公司 HiAgent 任务 ID。
     *
     * @param value 公司 HiAgent 任务 ID。
     */
    public void setTaskId(final String value) { this.taskId = value; }
    /**
     * 返回公司 HiAgent 回答消耗的令牌总数。
     *
     * @return 公司 HiAgent 回答消耗的令牌总数。
     */
    public Integer getTotalTokens() { return totalTokens; }
    /**
     * 设置公司 HiAgent 回答消耗的令牌总数。
     *
     * @param value 公司 HiAgent 回答消耗的令牌总数。
     */
    public void setTotalTokens(final Integer value) { this.totalTokens = value; }
    /**
     * 返回公司 HiAgent 回答耗时。
     *
     * @return 公司 HiAgent 回答耗时，单位为秒。
     */
    public Double getLatency() { return latency; }
    /**
     * 设置公司 HiAgent 回答耗时。
     *
     * @param value 公司 HiAgent 回答耗时，单位为秒。
     */
    public void setLatency(final Double value) { this.latency = value; }
    /**
     * 返回公司 HiAgent 链路追踪 JSON 字符串。
     *
     * @return 公司 HiAgent 链路追踪 JSON 字符串。
     */
    public String getTracingJsonStr() { return tracingJsonStr; }
    /**
     * 设置公司 HiAgent 链路追踪 JSON 字符串。
     *
     * @param value 公司 HiAgent 链路追踪 JSON 字符串。
     */
    public void setTracingJsonStr(final String value) { this.tracingJsonStr = value; }
    /**
     * 返回公司 HiAgent 意图识别 JSON 字符串。
     *
     * @return 公司 HiAgent 意图识别 JSON 字符串。
     */
    public String getIntentionJsonStr() { return intentionJsonStr; }
    /**
     * 设置公司 HiAgent 意图识别 JSON 字符串。
     *
     * @param value 公司 HiAgent 意图识别 JSON 字符串。
     */
    public void setIntentionJsonStr(final String value) { this.intentionJsonStr = value; }
    /**
     * 返回公司 HiAgent 回答是否使用了检索资源。
     *
     * @return 公司 HiAgent 回答是否使用了检索资源。
     */
    public Boolean getRetrieverResource() { return retrieverResource; }
    /**
     * 设置公司 HiAgent 回答是否使用了检索资源。
     *
     * @param value 公司 HiAgent 回答是否使用了检索资源。
     */
    public void setRetrieverResource(final Boolean value) { this.retrieverResource = value; }
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
