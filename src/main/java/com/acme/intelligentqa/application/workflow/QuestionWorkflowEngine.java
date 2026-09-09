package com.acme.intelligentqa.application.workflow;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

/**
 * 按带版本的场景计划执行节点，并统一处理停止检查、跳过和执行记录。
 */
@Component
public class QuestionWorkflowEngine {
    /** 场景计划和节点注册表。 */
    private final ScenarioPlanRegistry registry;
    /** 节点执行记录使用的系统时钟。 */
    private final Clock clock;

    /**
     * 创建轻量问答工作流执行器。
     *
     * @param registry 场景计划和节点注册表。
     * @param clock 系统时钟。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public QuestionWorkflowEngine(final ScenarioPlanRegistry registry, final Clock clock) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 执行上下文指定场景的已批准节点计划。
     *
     * @param context 单次问答工作流上下文。
     * @param beforeNode 节点执行前的回答状态和事件协调回调。
     * @param afterNode 节点成功后的回答状态和事件协调回调。
     */
    public void execute(
            final QuestionWorkflowContext context,
            final BiConsumer<WorkflowNodeCode, QuestionWorkflowContext> beforeNode,
            final BiConsumer<WorkflowNodeCode, QuestionWorkflowContext> afterNode) {
        final ScenarioExecutionPlan plan = registry.planFor(
                Objects.requireNonNull(context, "context must not be null").scenario());
        context.bindPlanVersion(plan.version());
        for (final WorkflowNodeCode nodeCode : plan.nodeCodes()) {
            if (executeNode(context, registry.node(nodeCode), beforeNode, afterNode)
                    == WorkflowNodeResult.STOP) {
                return;
            }
        }
    }

    /**
     * 执行单个节点并写入开始、跳过、成功或失败记录。
     *
     * @param context 单次问答工作流上下文。
     * @param node 待执行节点。
     * @param beforeNode 节点执行前协调回调。
     * @param afterNode 节点成功后协调回调。
     * @return 后续节点执行控制结果。
     */
    private WorkflowNodeResult executeNode(
            final QuestionWorkflowContext context,
            final WorkflowNode node,
            final BiConsumer<WorkflowNodeCode, QuestionWorkflowContext> beforeNode,
            final BiConsumer<WorkflowNodeCode, QuestionWorkflowContext> afterNode) {
        context.ensureActive();
        if (!node.shouldExecute(context)) {
            context.recordExecution(
                    node.code(), WorkflowNodeStatus.SKIPPED, Instant.now(clock), node.skipReason(context));
            return WorkflowNodeResult.CONTINUE;
        }
        context.recordExecution(node.code(), WorkflowNodeStatus.STARTED, Instant.now(clock), "");
        boolean succeeded = false;
        try {
            Objects.requireNonNull(beforeNode, "beforeNode must not be null").accept(node.code(), context);
            final WorkflowNodeResult result = Objects.requireNonNull(
                    node.execute(context), "workflow node result must not be null");
            Objects.requireNonNull(afterNode, "afterNode must not be null").accept(node.code(), context);
            context.recordExecution(node.code(), WorkflowNodeStatus.SUCCEEDED, Instant.now(clock), "");
            succeeded = true;
            return result;
        } finally {
            // 不捕获或改写节点异常，只在异常继续向上传播前补充不含业务正文的失败轨迹。
            if (!succeeded) {
                context.recordExecution(
                        node.code(), WorkflowNodeStatus.FAILED, Instant.now(clock), "NODE_EXECUTION_FAILED");
            }
        }
    }
}
