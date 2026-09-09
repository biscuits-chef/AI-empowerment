package com.acme.intelligentqa.application.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.QueryScenario;
import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 验证场景计划驱动的轻量问答工作流执行器。
 */
class QuestionWorkflowEngineTest {
    /** 固定测试时间。 */
    private static final Instant NOW = Instant.parse("2026-09-03T08:00:00Z");

    /**
     * 验证双通道计划按集中声明的顺序执行，并把节点生命周期保存到上下文。
     */
    @Test
    void executesDualChannelPlanAndRecordsNodeLifecycle() {
        final List<WorkflowNodeCode> calls = new ArrayList<>();
        final QuestionWorkflowEngine engine = engine(calls, null, null);
        final QuestionWorkflowContext context = context();

        engine.execute(context, (code, value) -> { }, (code, value) -> { });

        assertEquals(Arrays.asList(
                WorkflowNodeCode.QUESTION_UNDERSTANDING,
                WorkflowNodeCode.KNOWLEDGE_RETRIEVAL,
                WorkflowNodeCode.BUSINESS_DATA_QUERY,
                WorkflowNodeCode.EVIDENCE_RECONCILIATION,
                WorkflowNodeCode.ANSWER_GENERATION), calls);
        assertEquals("1", context.planVersion());
        assertEquals(10, context.executions().size());
        assertEquals(WorkflowNodeStatus.STARTED, context.executions().get(0).status());
        assertEquals(WorkflowNodeStatus.SUCCEEDED, context.executions().get(1).status());
    }

    /**
     * 验证前置条件不满足的节点被记录为跳过，且执行器自动进入下一个节点。
     */
    @Test
    void skipsNodeAndContinuesWithNextNode() {
        final List<WorkflowNodeCode> calls = new ArrayList<>();
        final QuestionWorkflowEngine engine = engine(
                calls, WorkflowNodeCode.KNOWLEDGE_RETRIEVAL, null);
        final QuestionWorkflowContext context = context();

        engine.execute(context, (code, value) -> { }, (code, value) -> { });

        assertEquals(4, calls.size());
        assertTrue(!calls.contains(WorkflowNodeCode.KNOWLEDGE_RETRIEVAL));
        assertTrue(context.executions().stream().anyMatch(execution ->
                execution.nodeCode() == WorkflowNodeCode.KNOWLEDGE_RETRIEVAL
                        && execution.status() == WorkflowNodeStatus.SKIPPED
                        && "TEST_PRECONDITION".equals(execution.reason())));
        assertEquals(WorkflowNodeCode.ANSWER_GENERATION, calls.get(calls.size() - 1));
    }

    /**
     * 验证追问等合法短路结果会阻止后续证据节点执行。
     */
    @Test
    void stopsPlanAfterNodeRequestsShortCircuit() {
        final List<WorkflowNodeCode> calls = new ArrayList<>();
        final QuestionWorkflowEngine engine = engine(
                calls, null, WorkflowNodeCode.QUESTION_UNDERSTANDING);
        final QuestionWorkflowContext context = context();

        engine.execute(context, (code, value) -> { }, (code, value) -> { });

        assertEquals(Collections.singletonList(WorkflowNodeCode.QUESTION_UNDERSTANDING), calls);
        assertEquals(2, context.executions().size());
    }

    /**
     * 验证注册表在启动阶段拒绝缺少计划必需节点的配置。
     */
    @Test
    void rejectsPlanWhenRequiredNodesAreMissing() {
        final List<WorkflowNode> incomplete = Collections.singletonList(
                new FakeWorkflowNode(
                        WorkflowNodeCode.QUESTION_UNDERSTANDING,
                        new ArrayList<>(),
                        true,
                        WorkflowNodeResult.CONTINUE));

        assertThrows(IllegalStateException.class, () -> new ScenarioPlanRegistry(incomplete));
    }

    /**
     * 使用可控节点组装双通道工作流执行器。
     *
     * @param calls 节点实际调用顺序。
     * @param skippedNode 需要模拟跳过的节点；没有时为空。
     * @param stoppingNode 需要模拟合法短路的节点；没有时为空。
     * @return 测试工作流执行器。
     */
    private QuestionWorkflowEngine engine(
            final List<WorkflowNodeCode> calls,
            final WorkflowNodeCode skippedNode,
            final WorkflowNodeCode stoppingNode) {
        final List<WorkflowNode> nodes = new ArrayList<>();
        for (final WorkflowNodeCode code : WorkflowNodeCode.values()) {
            nodes.add(new FakeWorkflowNode(
                    code,
                    calls,
                    code != skippedNode,
                    code == stoppingNode ? WorkflowNodeResult.STOP : WorkflowNodeResult.CONTINUE));
        }
        return new QuestionWorkflowEngine(
                new ScenarioPlanRegistry(nodes), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * 创建测试使用的最小双通道执行上下文。
     *
     * @return 测试工作流上下文。
     */
    private QuestionWorkflowContext context() {
        final AnswerSnapshot answer = new AnswerSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                AnswerSnapshot.Status.PENDING,
                "",
                null,
                NOW,
                null);
        return new QuestionWorkflowContext(
                "owner-1",
                answer,
                "查询产品信息",
                Collections.emptyList(),
                null,
                QueryScenario.DUAL_CHANNEL_QA,
                value -> { },
                LanguageModelPort.NO_CANCELLATION,
                () -> { });
    }

    /**
     * 测试使用的可控工作流节点。
     */
    private static final class FakeWorkflowNode implements WorkflowNode {
        /** 节点稳定编码。 */
        private final WorkflowNodeCode code;
        /** 实际调用顺序。 */
        private final List<WorkflowNodeCode> calls;
        /** 是否满足节点前置条件。 */
        private final boolean executable;
        /** 节点完成后的控制结果。 */
        private final WorkflowNodeResult result;

        /**
         * 创建可控工作流节点。
         *
         * @param code 节点稳定编码。
         * @param calls 实际调用顺序。
         * @param executable 是否满足节点前置条件。
         * @param result 节点完成后的控制结果。
         */
        FakeWorkflowNode(
                final WorkflowNodeCode code,
                final List<WorkflowNodeCode> calls,
                final boolean executable,
                final WorkflowNodeResult result) {
            this.code = code;
            this.calls = calls;
            this.executable = executable;
            this.result = result;
        }

        /**
         * 返回测试节点稳定编码。
         *
         * @return 节点稳定编码。
         */
        @Override
        public WorkflowNodeCode code() { return code; }

        /**
         * 返回测试配置的前置条件判断结果。
         *
         * @param context 当前问答工作流上下文。
         * @return 配置的节点可执行状态。
         */
        @Override
        public boolean shouldExecute(final QuestionWorkflowContext context) { return executable; }

        /**
         * 返回测试使用的固定跳过原因。
         *
         * @param context 当前问答工作流上下文。
         * @return 固定安全跳过原因。
         */
        @Override
        public String skipReason(final QuestionWorkflowContext context) { return "TEST_PRECONDITION"; }

        /**
         * 记录节点调用并返回测试配置的控制结果。
         *
         * @param context 当前问答工作流上下文。
         * @return 配置的后续节点控制结果。
         */
        @Override
        public WorkflowNodeResult execute(final QuestionWorkflowContext context) {
            calls.add(code);
            return result;
        }
    }
}
