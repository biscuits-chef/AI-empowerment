package com.acme.intelligentqa.application.workflow;

/**
 * 单个工作流节点完成后的执行控制结果。
 */
public enum WorkflowNodeResult {
    /** 当前节点完成后继续执行计划中的下一个节点。 */
    CONTINUE,
    /** 当前节点产生追问等合法短路结果，不再执行后续节点。 */
    STOP
}
