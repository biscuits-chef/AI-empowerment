package com.acme.intelligentqa.domain.service;

import com.acme.intelligentqa.domain.model.BusinessQueryPlan;
import com.acme.intelligentqa.domain.model.CompiledBusinessQuery;
import java.util.Objects;

/**
 * 将一期逻辑计划编译为固定 MyBatis 语句和绑定参数，禁止生成自由 SQL 文本。
 */
public final class PhaseOneBusinessSqlCompiler {

    /**
     * 编译受控逻辑查询计划。
     *
     * @param plan 已通过语义目录校验的逻辑查询计划。
     * @return 只包含批准语句和绑定参数的编译结果。
     */
    public CompiledBusinessQuery compile(final BusinessQueryPlan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        final CompiledBusinessQuery.Statement statement;
        if (plan.type() == BusinessQueryPlan.Type.PRODUCT_LOOKUP) {
            statement = CompiledBusinessQuery.Statement.SELECT_PRODUCT_FACTS;
        } else if (plan.type() == BusinessQueryPlan.Type.PRODUCT_REFERENCE_DATE_LIST) {
            statement = CompiledBusinessQuery.Statement.SELECT_REFERENCE_DATE_PRODUCTS;
        } else {
            statement = CompiledBusinessQuery.Statement.SELECT_TRADE_FACTS;
        }
        return new CompiledBusinessQuery(statement, plan);
    }
}
