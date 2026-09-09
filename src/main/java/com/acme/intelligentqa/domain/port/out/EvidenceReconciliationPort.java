package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.EvidenceAssessment;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.model.QueryIntent;
import java.util.List;

/**
 * 对知识库和业务数据库证据执行确定性对账的出站端口。
 */
public interface EvidenceReconciliationPort {

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
    EvidenceAssessment reconcile(
            QueryIntent intent,
            List<KnowledgeChunk> knowledge,
            List<BusinessFact> businessFacts);
}
