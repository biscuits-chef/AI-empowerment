package com.acme.intelligentqa.application.workflow.node;

import com.acme.intelligentqa.application.workflow.QuestionWorkflowContext;
import com.acme.intelligentqa.application.workflow.WorkflowNode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeCode;
import com.acme.intelligentqa.application.workflow.WorkflowNodeResult;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.port.out.BusinessDataQueryPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 使用结构化意图和消解后的问题执行受控 GoldenDB 业务查询。
 */
@Component
public class BusinessDataQueryWorkflowNode implements WorkflowNode {
    /** 受控业务数据查询端口。 */
    private final BusinessDataQueryPort businessDataQuery;
    /** 问答上下文容量配置。 */
    private final QaProperties properties;

    /**
     * 创建业务数据库查询工作流节点。
     *
     * @param businessDataQuery 受控业务数据查询端口。
     * @param properties 问答上下文容量配置。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public BusinessDataQueryWorkflowNode(
            final BusinessDataQueryPort businessDataQuery,
            final QaProperties properties) {
        this.businessDataQuery = Objects.requireNonNull(
                businessDataQuery, "businessDataQuery must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 返回业务数据库查询节点稳定编码。
     *
     * @return 业务数据库查询节点编码。
     */
    @Override
    public WorkflowNodeCode code() { return WorkflowNodeCode.BUSINESS_DATA_QUERY; }

    /**
     * 执行受控业务查询并保存经过边界校验的事实。
     *
     * @param context 当前问答工作流上下文。
     * @return 继续执行下一个节点。
     */
    @Override
    public WorkflowNodeResult execute(final QuestionWorkflowContext context) {
        final List<BusinessFact> facts = businessDataQuery.query(
                context.ownerId(), context.resolvedQuestion(), context.intent(), properties.maxContextItems());
        requireBounded(facts);
        context.businessFacts(facts);
        return WorkflowNodeResult.CONTINUE;
    }

    /**
     * 校验业务事实数量、空元素和总字符数上限。
     *
     * @param facts 待校验业务事实。
     */
    private void requireBounded(final List<BusinessFact> facts) {
        Objects.requireNonNull(facts, "business data context must not be null");
        if (facts.size() > properties.maxContextItems()) {
            throw new IllegalArgumentException("business data context exceeds configured item maximum");
        }
        int characters = 0;
        for (final BusinessFact fact : facts) {
            characters += Objects.requireNonNull(
                    fact, "business data context must not contain null items").content().length();
            if (characters > properties.maxBusinessDataCharacters()) {
                throw new IllegalArgumentException("business data context exceeds configured character maximum");
            }
        }
    }
}
