package com.acme.intelligentqa.application.workflow;

import com.acme.intelligentqa.domain.model.QueryScenario;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 集中注册场景执行计划及其可复用节点，避免节点反向依赖场景枚举。
 */
@Component
public class ScenarioPlanRegistry {
    /** 已注册节点的稳定编码索引。 */
    private final Map<WorkflowNodeCode, WorkflowNode> nodes;
    /** 当前允许执行的场景计划。 */
    private final Map<QueryScenario, ScenarioExecutionPlan> plans;

    /**
     * 创建第一阶段场景计划注册表。
     *
     * @param registeredNodes Spring 收集的全部工作流节点。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Nodes are copied into a private immutable map")
    public ScenarioPlanRegistry(final List<WorkflowNode> registeredNodes) {
        nodes = indexNodes(registeredNodes);
        final Map<QueryScenario, ScenarioExecutionPlan> configured = new EnumMap<>(QueryScenario.class);
        configured.put(QueryScenario.DUAL_CHANNEL_QA, new ScenarioExecutionPlan(
                QueryScenario.DUAL_CHANNEL_QA,
                "1",
                Arrays.asList(
                        WorkflowNodeCode.QUESTION_UNDERSTANDING,
                        WorkflowNodeCode.KNOWLEDGE_RETRIEVAL,
                        WorkflowNodeCode.BUSINESS_DATA_QUERY,
                        WorkflowNodeCode.EVIDENCE_RECONCILIATION,
                        WorkflowNodeCode.ANSWER_GENERATION)));
        validatePlans(configured, nodes);
        plans = Collections.unmodifiableMap(configured);
    }

    /**
     * 返回指定查询场景的已批准执行计划。
     *
     * @param scenario 后端查询场景。
     * @return 已批准且带版本的执行计划。
     */
    public ScenarioExecutionPlan planFor(final QueryScenario scenario) {
        final ScenarioExecutionPlan plan = plans.get(Objects.requireNonNull(scenario, "scenario must not be null"));
        if (plan == null) {
            throw new IllegalArgumentException("query scenario is not available: " + scenario);
        }
        return plan;
    }

    /**
     * 返回指定稳定编码对应的工作流节点。
     *
     * @param nodeCode 节点稳定编码。
     * @return 已注册工作流节点。
     */
    public WorkflowNode node(final WorkflowNodeCode nodeCode) {
        final WorkflowNode workflowNode = nodes.get(Objects.requireNonNull(nodeCode, "nodeCode must not be null"));
        if (workflowNode == null) {
            throw new IllegalStateException("workflow node is not registered: " + nodeCode);
        }
        return workflowNode;
    }

    /**
     * 按稳定编码索引节点并拒绝重复注册。
     *
     * @param registeredNodes Spring 收集的工作流节点。
     * @return 不可变节点索引。
     */
    private static Map<WorkflowNodeCode, WorkflowNode> indexNodes(
            final List<WorkflowNode> registeredNodes) {
        final Map<WorkflowNodeCode, WorkflowNode> indexed = new EnumMap<>(WorkflowNodeCode.class);
        for (final WorkflowNode node : Objects.requireNonNull(
                registeredNodes, "registeredNodes must not be null")) {
            final WorkflowNode safeNode = Objects.requireNonNull(node, "registeredNodes must not contain null");
            if (indexed.put(safeNode.code(), safeNode) != null) {
                throw new IllegalStateException("duplicate workflow node: " + safeNode.code());
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    /**
     * 启动时校验计划引用的节点已经全部注册。
     *
     * @param configured 待启用场景计划。
     * @param indexed 已注册节点索引。
     */
    private static void validatePlans(
            final Map<QueryScenario, ScenarioExecutionPlan> configured,
            final Map<WorkflowNodeCode, WorkflowNode> indexed) {
        for (final ScenarioExecutionPlan plan : configured.values()) {
            for (final WorkflowNodeCode nodeCode : plan.nodeCodes()) {
                if (!indexed.containsKey(nodeCode)) {
                    throw new IllegalStateException("plan references missing workflow node: " + nodeCode);
                }
            }
        }
    }
}
