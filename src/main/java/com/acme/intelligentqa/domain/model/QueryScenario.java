package com.acme.intelligentqa.domain.model;

/**
 * 一次问答采用的后端执行场景。
 *
 * <p>场景描述证据获取和回答生成策略，不等同于前端展示的 Agent 入口。
 */
public enum QueryScenario {
    /** 第一阶段唯一开放的知识库与业务数据库双通道查询场景。 */
    DUAL_CHANNEL_QA
}
