package com.acme.intelligentqa.application.workflow.node;

import com.acme.intelligentqa.application.workflow.QuestionWorkflowContext;
import com.acme.intelligentqa.application.workflow.WorkflowNode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeCode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeResult;
import com.acme.intelligentqa.domain.model.EvidenceAssessment;
import com.acme.intelligentqa.domain.port.out.EvidenceReconciliationPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 对知识库片段和 GoldenDB 业务事实执行确定性证据对账。
 */
@Component
public class EvidenceReconciliationWorkflowNode implements WorkflowNode {
    /** 确定性证据对账端口。 */
    private final EvidenceReconciliationPort evidenceReconciliation;

    /**
     * 创建证据对账工作流节点。
     *
     * @param evidenceReconciliation 确定性证据对账端口。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborator is retained and not exposed")
    public EvidenceReconciliationWorkflowNode(
            final EvidenceReconciliationPort evidenceReconciliation) {
        this.evidenceReconciliation = Objects.requireNonNull(
                evidenceReconciliation, "evidenceReconciliation must not be null");
    }

    /**
     * 返回证据对账节点稳定编码。
     *
     * @return 证据对账节点编码。
     */
    @Override
    public WorkflowNodeCode code() { return WorkflowNodeCode.EVIDENCE_RECONCILIATION; }

    /**
     * 对两个证据通道进行对账并保存结构化结论。
     *
     * @param context 当前问答工作流上下文。
     * @return 继续执行回答生成节点。
     */
    @Override
    public WorkflowNodeResult execute(final QuestionWorkflowContext context) {
        final EvidenceAssessment assessment = evidenceReconciliation.reconcile(
                context.intent(), context.knowledge(), context.businessFacts());
        context.evidenceAssessment(Objects.requireNonNull(
                assessment, "evidence assessment must not be null"));
        return WorkflowNodeResult.CONTINUE;
    }
}
