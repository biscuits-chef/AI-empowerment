package com.acme.intelligentqa.domain.model;

/**
 * 前端随提问提交、后端用于选择处理链路的 Agent 类型。
 */
public enum AgentType {
    /** 智能问数，第一阶段唯一开放的执行类型。 */
    SMART_DATA,
    /** 通用智能问答，当前尚未开放。 */
    SMART_QA,
    /** 合同智能审核，当前尚未开放。 */
    CONTRACT_REVIEW,
    /** 合同差异比对，当前尚未开放。 */
    CONTRACT_COMPARE,
    /** 申赎确认单处理，当前尚未开放。 */
    CONFIRMATION_CHECK;

    /**
     * 校验当前类型是否已经开放执行。
     *
     * @throws IllegalArgumentException 当前类型尚未开放时抛出。
     */
    public void requireAvailable() {
        if (this != SMART_DATA) {
            throw new IllegalArgumentException("agent type is not available: " + this);
        }
    }

    /**
     * 将已开放的 Agent 路由为后端查询场景。
     *
     * @return 第一阶段双通道问答场景。
     *
     * @throws IllegalArgumentException 当前 Agent 尚未开放时抛出。
     */
    public QueryScenario queryScenario() {
        requireAvailable();
        return QueryScenario.DUAL_CHANNEL_QA;
    }
}
