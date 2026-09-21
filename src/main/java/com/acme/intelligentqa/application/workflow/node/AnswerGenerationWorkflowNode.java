package com.acme.intelligentqa.application.workflow.node;

import com.acme.intelligentqa.application.workflow.QuestionWorkflowContext;
import com.acme.intelligentqa.application.workflow.WorkflowNode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeCode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeResult;
import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.EvidenceAssessment;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 根据证据充分性生成确定性拒答、冲突提示或受证据约束的大模型回答。
 */
@Component
public class AnswerGenerationWorkflowNode implements WorkflowNode {
    /** 证据不足时使用的确定性拒答文本。 */
    private static final String INSUFFICIENT_EVIDENCE_MESSAGE =
            "未检索到足够且可核验的证据，系统不会推测答案，请补充产品或交易标识后重试。";
    /** 公司大模型生成端口。 */
    private final LanguageModelPort languageModel;

    /**
     * 创建回答生成工作流节点。
     *
     * @param languageModel 公司大模型生成端口。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborator is retained and not exposed")
    public AnswerGenerationWorkflowNode(final LanguageModelPort languageModel) {
        this.languageModel = Objects.requireNonNull(languageModel, "languageModel must not be null");
    }

    /**
     * 返回回答生成节点稳定编码。
     *
     * @return 回答生成节点编码。
     */
    @Override
    public WorkflowNodeCode code() { return WorkflowNodeCode.ANSWER_GENERATION; }

    /**
     * 按证据结论输出确定性文本或调用公司大模型流式生成。
     *
     * @param context 当前问答工作流上下文。
     * @return 回答生成完成后继续结束执行计划。
     */
    @Override
    public WorkflowNodeResult execute(final QuestionWorkflowContext context) {
        if (context.evidenceAssessment().status() == EvidenceAssessment.Status.INSUFFICIENT) {
            context.emit(INSUFFICIENT_EVIDENCE_MESSAGE);
            context.finishReason("insufficient_evidence");
            return WorkflowNodeResult.CONTINUE;
        }
        if (context.evidenceAssessment().manualReviewRequired()) {
            context.emit(conflictNotice(
                    context.knowledge(), context.businessFacts(), context.evidenceAssessment()));
        }
        final LanguageModelPort.GenerationResult result = languageModel.generate(
                new LanguageModelPort.GenerationRequest(
                        context.ownerId(),
                        context.answer().conversationId(),
                        context.appConversationId(),
                        context.resolvedQuestion(),
                        context.intent(),
                        context.entitySourceMessageIds(),
                        context.evidenceAssessment(),
                        context.history(),
                        context.knowledge(),
                        context.businessFacts()),
                context::emit,
                context.generationControl());
        context.finishReason(Objects.requireNonNull(result, "generation result must not be null").finishReason());
        return WorkflowNodeResult.CONTINUE;
    }

    /**
     * 构造必须先于模型整理结果展示的双通道冲突说明。
     *
     * @param knowledge 知识库片段列表。
     * @param facts 受控查询返回的业务事实列表。
     * @param assessment 双通道证据对账结果。
     * @return 可直接流式展示的人工复核提示。
     */
    private String conflictNotice(
            final List<KnowledgeChunk> knowledge,
            final List<BusinessFact> facts,
            final EvidenceAssessment assessment) {
        final StringBuilder notice = new StringBuilder();
        notice.append("【人工复核提示】知识库与数据库证据不一致，本回答不得直接作为业务依据。\n")
                .append("冲突字段：").append(String.join("、", assessment.conflictFields())).append("\n")
                .append("【知识库通道】\n");
        for (final KnowledgeChunk chunk : knowledge) {
            notice.append("- ").append(chunk.title()).append("（").append(chunk.sourceId()).append("）：")
                    .append(chunk.content()).append("\n");
        }
        notice.append("【数据库通道】\n");
        for (final BusinessFact fact : facts) {
            notice.append("- ").append(fact.sourceCode()).append("：").append(fact.content()).append("\n");
        }
        return notice.append("【模型整理结果】\n").toString();
    }
}
