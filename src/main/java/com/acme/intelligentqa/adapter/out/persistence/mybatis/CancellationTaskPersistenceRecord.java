package com.acme.intelligentqa.adapter.out.persistence.mybatis;

/** 与回答停止任务表查询结果对应的持久化记录。 */
public class CancellationTaskPersistenceRecord {

    /**
     * 回答 ID。
     */
    private String answerId;
    /**
     * 用户所有者 ID。
     */
    private String ownerId;
    /**
     * 公司模型侧消息 ID。
     */
    private String messageId;
    /**
     * 停止时所处阶段。
     */
    private String cancelledStage;
    /**
     * 已执行的停止尝试次数。
     */
    private int attemptCount;

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
     * 返回已执行的停止尝试次数。
     *
     * @return 已执行的停止尝试次数。
     */
    public int getAttemptCount() { return attemptCount; }
    /**
     * 设置已执行的停止尝试次数。
     *
     * @param attemptCount 已执行的停止尝试次数。
     */
    public void setAttemptCount(final int attemptCount) { this.attemptCount = attemptCount; }
}
