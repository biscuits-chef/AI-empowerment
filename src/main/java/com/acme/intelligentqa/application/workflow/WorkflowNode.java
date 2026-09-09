package com.acme.intelligentqa.application.workflow;

/**
 * 可被不同场景执行计划复用的单一职责工作流节点。
 */
public interface WorkflowNode {
    /**
     * 返回节点稳定编码。
     *
     * @return 节点稳定编码。
     */
    WorkflowNodeCode code();

    /**
     * 根据当前数据状态判断节点是否需要执行。
     *
     * <p>节点只判断自身前置条件，不判断具体场景枚举；场景是否包含节点由执行计划决定。
     *
     * @param context 当前问答工作流上下文。
     * @return 需要执行时返回 true，否则返回 false。
     */
    default boolean shouldExecute(final QuestionWorkflowContext context) {
        return true;
    }

    /**
     * 返回节点被跳过时写入执行记录的安全原因。
     *
     * @param context 当前问答工作流上下文。
     * @return 不含业务正文的跳过原因。
     */
    default String skipReason(final QuestionWorkflowContext context) {
        return "PRECONDITION_NOT_MET";
    }

    /**
     * 执行节点并把结构化产物写入工作流上下文。
     *
     * @param context 当前问答工作流上下文。
     * @return 后续节点执行控制结果。
     */
    WorkflowNodeResult execute(QuestionWorkflowContext context);
}
