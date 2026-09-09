package com.acme.intelligentqa.domain.service;

import com.acme.intelligentqa.domain.model.BusinessSemanticQuery;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 一期可计算业务语义目录，集中维护实体可用字段和唯一批准关系。
 */
public final class PhaseOneBusinessSemanticCatalog {

    /** 产品实体允许读取的字段。 */
    private static final Set<BusinessSemanticQuery.Field> PRODUCT_FIELDS = Collections.unmodifiableSet(
            EnumSet.of(
                    BusinessSemanticQuery.Field.PRODUCT_CODE,
                    BusinessSemanticQuery.Field.PRODUCT_NAME,
                    BusinessSemanticQuery.Field.PRODUCT_MANAGER,
                    BusinessSemanticQuery.Field.INVESTMENT_MANAGER,
                    BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER,
                    BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER,
                    BusinessSemanticQuery.Field.PRODUCT_FORM,
                    BusinessSemanticQuery.Field.PRODUCT_STATUS,
                    BusinessSemanticQuery.Field.START_DATE,
                    BusinessSemanticQuery.Field.END_DATE,
                    BusinessSemanticQuery.Field.MANAGEMENT_FEE_RATE,
                    BusinessSemanticQuery.Field.PRODUCT_TIER,
                    BusinessSemanticQuery.Field.FEE_ADJUSTMENT_PLAN,
                    BusinessSemanticQuery.Field.PRODUCT_MATURITY_DATE,
                    BusinessSemanticQuery.Field.PERIODIC_OPEN_BASE_DATE,
                    BusinessSemanticQuery.Field.SNAPSHOT_DATE));
    /** 交易实体允许读取的字段。 */
    private static final Set<BusinessSemanticQuery.Field> TRADE_FIELDS = Collections.unmodifiableSet(
            EnumSet.of(
                    BusinessSemanticQuery.Field.TRADE_SERIAL_NUMBER,
                    BusinessSemanticQuery.Field.PRODUCT_CODE,
                    BusinessSemanticQuery.Field.TRADER_NAME,
                    BusinessSemanticQuery.Field.TRADE_DATE));

    /**
     * 校验字段是否属于指定业务实体。
     *
     * @param subject 查询主体。
     * @param fields 待校验字段集合。
     */
    public void validateFields(
            final BusinessSemanticQuery.Subject subject,
            final Set<BusinessSemanticQuery.Field> fields) {
        final Set<BusinessSemanticQuery.Field> allowed = subject == BusinessSemanticQuery.Subject.PRODUCT
                ? PRODUCT_FIELDS : TRADE_FIELDS;
        if (fields == null || fields.isEmpty() || !allowed.containsAll(fields)) {
            throw new IllegalArgumentException("query contains fields outside the approved semantic catalog");
        }
    }

    /**
     * 判断一期关系图谱是否允许交易追溯到所属产品。
     *
     * @param source 起点业务实体。
     * @param target 终点业务实体。
     * @return 条件成立时返回 true，否则返回 false。
     */
    public boolean supportsRelationship(
            final BusinessSemanticQuery.Subject source,
            final BusinessSemanticQuery.Subject target) {
        return source == BusinessSemanticQuery.Subject.TRADE
                && target == BusinessSemanticQuery.Subject.PRODUCT;
    }
}
