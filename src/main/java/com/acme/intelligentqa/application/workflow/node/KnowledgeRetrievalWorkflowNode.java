package com.acme.intelligentqa.application.workflow.node;

import com.acme.intelligentqa.application.workflow.QuestionWorkflowContext;
import com.acme.intelligentqa.application.workflow.WorkflowNode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeCode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeResult;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.port.out.KnowledgeRetrievalPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 使用消解后的问题查询公司知识库并执行有界上下文校验。
 */
@Component
public class KnowledgeRetrievalWorkflowNode implements WorkflowNode {
    /** 公司知识库检索端口。 */
    private final KnowledgeRetrievalPort knowledgeRetrieval;
    /** 问答上下文容量配置。 */
    private final QaProperties properties;

    /**
     * 创建知识库检索工作流节点。
     *
     * @param knowledgeRetrieval 公司知识库检索端口。
     * @param properties 问答上下文容量配置。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public KnowledgeRetrievalWorkflowNode(
            final KnowledgeRetrievalPort knowledgeRetrieval,
            final QaProperties properties) {
        this.knowledgeRetrieval = Objects.requireNonNull(
                knowledgeRetrieval, "knowledgeRetrieval must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 返回知识库检索节点稳定编码。
     *
     * @return 知识库检索节点编码。
     */
    @Override
    public WorkflowNodeCode code() { return WorkflowNodeCode.KNOWLEDGE_RETRIEVAL; }

    /**
     * 查询公司知识库并保存经过边界校验的片段。
     *
     * @param context 当前问答工作流上下文。
     * @return 继续执行下一个节点。
     */
    @Override
    public WorkflowNodeResult execute(final QuestionWorkflowContext context) {
        final List<KnowledgeChunk> chunks = knowledgeRetrieval.search(
                context.ownerId(), context.resolvedQuestion(), properties.maxContextItems());
        requireBounded(chunks);
        context.knowledge(chunks);
        return WorkflowNodeResult.CONTINUE;
    }

    /**
     * 校验知识片段数量、空元素和总字符数上限。
     *
     * @param chunks 待校验知识片段。
     */
    private void requireBounded(final List<KnowledgeChunk> chunks) {
        Objects.requireNonNull(chunks, "knowledge context must not be null");
        if (chunks.size() > properties.maxContextItems()) {
            throw new IllegalArgumentException("knowledge context exceeds configured item maximum");
        }
        int characters = 0;
        for (final KnowledgeChunk chunk : chunks) {
            characters += Objects.requireNonNull(
                    chunk, "knowledge context must not contain null items").content().length();
            if (characters > properties.maxKnowledgeCharacters()) {
                throw new IllegalArgumentException("knowledge context exceeds configured character maximum");
            }
        }
    }
}
