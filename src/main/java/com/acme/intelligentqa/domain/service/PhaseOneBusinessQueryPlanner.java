package com.acme.intelligentqa.domain.service;

import com.acme.intelligentqa.domain.model.BusinessQueryPlan;
import com.acme.intelligentqa.domain.model.BusinessSemanticQuery;
import java.util.Objects;

/**
 * 基于一期语义目录生成确定性逻辑查询计划，不执行自由 Schema 搜索或自由 Join。
 */
public final class PhaseOneBusinessQueryPlanner {

    /** 一期业务语义目录。 */
    private final PhaseOneBusinessSemanticCatalog catalog;

    /**
     * 创建一期确定性查询规划器。
     *
     * @param catalog 一期业务语义目录。
     */
    public PhaseOneBusinessQueryPlanner(final PhaseOneBusinessSemanticCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    /**
     * 将标准语义查询转换为受控逻辑查询计划。
     *
     * @param query 标准化业务语义查询。
     * @param semanticModelVersion 已发布语义模型版本。
     * @param maximumRows 系统允许的最大结果数量。
     * @return 受控逻辑查询计划。
     */
    public BusinessQueryPlan plan(
            final BusinessSemanticQuery query,
            final String semanticModelVersion,
            final int maximumRows) {
        Objects.requireNonNull(query, "query must not be null");
        catalog.validateFields(query.subject(), query.fields());
        final BusinessQueryPlan.Type type = planType(query);
        final int effectiveLimit = Math.min(query.limit(), maximumRows);
        final String planId = semanticModelVersion + ':' + type.name();
        return new BusinessQueryPlan(
                type,
                planId,
                semanticModelVersion,
                query.entityReference().orElse(null),
                query.businessDate().orElse(null),
                query.fields(),
                effectiveLimit);
    }

    /**
     * 根据标准语义操作和主体选择一期批准的逻辑计划类型。
     *
     * @param query 标准化业务语义查询。
     * @return 一期批准的查询计划类型。
     */
    private BusinessQueryPlan.Type planType(final BusinessSemanticQuery query) {
        if (query.operation() == BusinessSemanticQuery.Operation.REFERENCE_DATE_LIST) {
            return BusinessQueryPlan.Type.PRODUCT_REFERENCE_DATE_LIST;
        }
        return query.subject() == BusinessSemanticQuery.Subject.PRODUCT
                ? BusinessQueryPlan.Type.PRODUCT_LOOKUP : BusinessQueryPlan.Type.TRADE_LOOKUP;
    }
}
