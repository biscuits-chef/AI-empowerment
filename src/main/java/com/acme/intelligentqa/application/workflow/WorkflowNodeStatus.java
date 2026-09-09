package com.acme.intelligentqa.application.workflow;

/**
 * 工作流上下文中记录的节点执行状态。
 */
public enum WorkflowNodeStatus {
    /** 节点已经开始执行。 */
    STARTED,
    /** 节点已经成功完成。 */
    SUCCEEDED,
    /** 节点因前置条件不满足而被跳过。 */
    SKIPPED,
    /** 节点执行失败并向上抛出异常。 */
    FAILED
}
