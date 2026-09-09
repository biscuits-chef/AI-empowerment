package com.acme.intelligentqa.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 由一期 SQL 编译器产生的批准语句及逻辑计划，不包含可执行的自由 SQL 文本。
 */
public final class CompiledBusinessQuery {

    /** 一期允许执行的物理语句。 */
    public enum Statement {
        /** 查询受权产品语义视图。 */
        SELECT_PRODUCT_FACTS,
        /** 查询受权交易语义视图。 */
        SELECT_TRADE_FACTS,
        /** 查询指定业务日期命中定开基准日或到期日的产品。 */
        SELECT_REFERENCE_DATE_PRODUCTS
    }

    /** 批准的物理语句。 */
    private final Statement statement;
    /** 已通过语义目录校验的逻辑计划。 */
    private final BusinessQueryPlan plan;

    /**
     * 创建编译后的受控业务查询。
     *
     * @param statement 批准的物理语句。
     * @param plan 已通过语义目录校验的逻辑计划。
     */
    public CompiledBusinessQuery(final Statement statement, final BusinessQueryPlan plan) {
        this.statement = Objects.requireNonNull(statement, "statement must not be null");
        this.plan = Objects.requireNonNull(plan, "plan must not be null");
    }

    /**
     * 返回批准的物理语句。
     *
     * @return 批准的物理语句。
     */
    public Statement statement() { return statement; }

    /**
     * 返回逻辑计划标识。
     *
     * @return 逻辑计划标识。
     */
    public String planId() { return plan.planId(); }

    /**
     * 返回语义模型版本。
     *
     * @return 语义模型版本。
     */
    public String semanticModelVersion() { return plan.semanticModelVersion(); }

    /**
     * 返回实体引用绑定值。
     *
     * @return 实体引用绑定值。
     */
    public Optional<String> entityReference() { return plan.entityReference(); }

    /**
     * 返回日期列表查询使用的业务日期。
     *
     * @return 业务日期；非日期列表查询时为空。
     */
    public Optional<LocalDate> businessDate() { return plan.businessDate(); }

    /**
     * 返回允许输出的字段集合。
     *
     * @return 允许输出的字段集合。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Plan exposes an unmodifiable defensive copy")
    public Set<BusinessSemanticQuery.Field> fields() { return plan.fields(); }

    /**
     * 返回结果数量上限。
     *
     * @return 结果数量上限。
     */
    public int limit() { return plan.limit(); }
}
