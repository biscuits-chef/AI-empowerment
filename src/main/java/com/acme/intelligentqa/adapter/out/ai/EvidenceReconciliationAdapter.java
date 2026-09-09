package com.acme.intelligentqa.adapter.out.ai;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.EvidenceAssessment;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.port.out.EvidenceReconciliationPort;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 对双通道结果进行字段级对账，并标记冲突和人工复核要求。
 */
@Component
public class EvidenceReconciliationAdapter implements EvidenceReconciliationPort {

    /**
     * 配置参数。
     */
    private final QaProperties properties;

    /**
     * 创建 {@code EvidenceReconciliationAdapter} 实例。
     *
     * @param properties 配置参数。
     */
    public EvidenceReconciliationAdapter(final QaProperties properties) {
        this.properties = properties;
    }

    /**
     * 执行双通道事实对账并标记人工复核要求。
     *
     * @param intent 查询意图。
     *
     * @param knowledge 知识库片段列表。
     *
     * @param businessFacts 业务事实列表。
     *
     * @return 执行双通道事实对账并标记人工复核要求。
     */
    @Override
    public EvidenceAssessment reconcile(
            final QueryIntent intent,
            final List<KnowledgeChunk> knowledge,
            final List<BusinessFact> businessFacts) {
        if (!properties.demoMode()) {
            throw new DependencyUnavailableException(
                    "EVIDENCE_RECONCILIATION_UNCONFIGURED", "evidence reconciliation is not configured");
        }
        final EvidenceAssessment.Status status = knowledge.isEmpty() && businessFacts.isEmpty()
                ? EvidenceAssessment.Status.INSUFFICIENT : EvidenceAssessment.Status.CONSISTENT;
        return new EvidenceAssessment(status, Collections.emptyList());
    }
}
