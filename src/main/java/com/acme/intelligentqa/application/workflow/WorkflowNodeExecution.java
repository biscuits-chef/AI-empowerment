package com.acme.intelligentqa.application.workflow;

import java.time.Instant;
import java.util.Objects;

/**
 * 保存在单次工作流上下文中的节点执行记录。
 */
public final class WorkflowNodeExecution {
    /** 节点稳定编码。 */
    private final WorkflowNodeCode nodeCode;
    /** 本次记录对应的执行状态。 */
    private final WorkflowNodeStatus status;
    /** 状态记录时间。 */
    private final Instant recordedAt;
    /** 跳过或失败的安全原因，不包含原始业务数据。 */
    private final String reason;

    /**
     * 创建节点执行记录。
     *
     * @param nodeCode 节点稳定编码。
     * @param status 本次执行状态。
     * @param recordedAt 状态记录时间。
     * @param reason 跳过或失败的安全原因。
     */
    public WorkflowNodeExecution(
            final WorkflowNodeCode nodeCode,
            final WorkflowNodeStatus status,
            final Instant recordedAt,
            final String reason) {
        this.nodeCode = Objects.requireNonNull(nodeCode, "nodeCode must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        this.reason = reason == null ? "" : reason;
    }

    /**
     * 返回节点稳定编码。
     *
     * @return 节点稳定编码。
     */
    public WorkflowNodeCode nodeCode() { return nodeCode; }

    /**
     * 返回节点执行状态。
     *
     * @return 节点执行状态。
     */
    public WorkflowNodeStatus status() { return status; }

    /**
     * 返回状态记录时间。
     *
     * @return 状态记录时间。
     */
    public Instant recordedAt() { return recordedAt; }

    /**
     * 返回不含业务正文的安全原因。
     *
     * @return 跳过或失败原因；没有原因时为空字符串。
     */
    public String reason() { return reason; }
}
