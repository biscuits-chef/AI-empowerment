package com.acme.intelligentqa.application.workflow;

/**
 * 问答工作流中可复用节点的稳定编码。
 */
public enum WorkflowNodeCode {
    /** 意图识别、实体解析、指代消解和追问判断节点。 */
    QUESTION_UNDERSTANDING,
    /** 公司知识库检索节点。 */
    KNOWLEDGE_RETRIEVAL,
    /** GoldenDB 受控业务数据查询节点。 */
    BUSINESS_DATA_QUERY,
    /** 多通道证据确定性对账节点。 */
    EVIDENCE_RECONCILIATION,
    /** 证据约束下的回答生成节点。 */
    ANSWER_GENERATION
}
