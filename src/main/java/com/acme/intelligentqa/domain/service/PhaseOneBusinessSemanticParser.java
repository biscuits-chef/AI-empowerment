package com.acme.intelligentqa.domain.service;

import com.acme.intelligentqa.domain.model.BusinessSemanticQuery;
import com.acme.intelligentqa.domain.model.QueryIntent;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 将一期已消歧问答意图转换为标准化业务语义查询，且不产生表名、字段名或 SQL。
 */
public final class PhaseOneBusinessSemanticParser {

    /** 产品实体引用的标准字段名。 */
    private static final String PRODUCT_REFERENCE = "productReference";
    /** 交易实体引用的标准字段名。 */
    private static final String TRADE_REFERENCE = "tradeReference";
    /** 业务日期实体的标准字段名。 */
    private static final String BUSINESS_DATE = "businessDate";
    /** 业务日期统一使用的时区。 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    /** 用于解析“今日”的系统时钟。 */
    private final Clock clock;

    /**
     * 使用系统时钟创建一期业务语义解析器。
     */
    public PhaseOneBusinessSemanticParser() {
        this(Clock.systemUTC());
    }

    /**
     * 创建使用指定时钟的一期业务语义解析器。
     *
     * @param clock 用于解析相对日期的系统时钟。
     */
    public PhaseOneBusinessSemanticParser(final Clock clock) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 解析一期标准化业务语义查询。
     *
     * @param question 用户问题。
     * @param intent 已完成消歧的查询意图。
     * @param limit 请求结果数量上限。
     * @return 一期标准化业务语义查询。
     */
    public BusinessSemanticQuery parse(
            final String question,
            final QueryIntent intent,
            final int limit) {
        validateQuestion(question);
        validateIntent(intent);
        final Map<String, String> entities = intent.entities();
        if (intent.type() == QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY) {
            return referenceDateList(entities.get(BUSINESS_DATE), limit);
        }
        if (hasText(entities.get(TRADE_REFERENCE))) {
            return tradeQuery(entities.get(TRADE_REFERENCE), limit);
        }
        if (hasText(entities.get(PRODUCT_REFERENCE))) {
            return productQuery(question, intent.type(), entities.get(PRODUCT_REFERENCE), limit);
        }
        throw new IllegalArgumentException("resolved query requires a product or trade reference");
    }

    /**
     * 创建查询定开基准日或产品到期日的日期列表语义查询。
     *
     * @param dateText 已识别的业务日期；为空时表示当前业务日。
     * @param limit 请求结果数量上限。
     * @return 日期列表语义查询。
     */
    private BusinessSemanticQuery referenceDateList(final String dateText, final int limit) {
        final LocalDate businessDate = hasText(dateText)
                ? LocalDate.parse(dateText) : LocalDate.now(clock.withZone(BUSINESS_ZONE));
        return BusinessSemanticQuery.referenceDateList(
                businessDate,
                EnumSet.of(
                        BusinessSemanticQuery.Field.PRODUCT_CODE,
                        BusinessSemanticQuery.Field.PRODUCT_NAME,
                        BusinessSemanticQuery.Field.PERIODIC_OPEN_BASE_DATE,
                        BusinessSemanticQuery.Field.PRODUCT_MATURITY_DATE,
                        BusinessSemanticQuery.Field.SNAPSHOT_DATE),
                limit);
    }

    /**
     * 校验用户问题非空。
     *
     * @param question 用户问题。
     */
    private void validateQuestion(final String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("question must not be blank");
        }
    }

    /**
     * 校验查询意图已经识别且属于一期支持范围。
     *
     * @param intent 已完成消歧的查询意图。
     */
    private void validateIntent(final QueryIntent intent) {
        if (intent == null || intent.type() == QueryIntent.Type.UNSUPPORTED) {
            throw new IllegalArgumentException("unsupported intent cannot be queried");
        }
    }

    /**
     * 创建交易语义查询。
     *
     * @param reference 已确认的交易引用。
     * @param limit 请求结果数量上限。
     * @return 交易语义查询。
     */
    private BusinessSemanticQuery tradeQuery(final String reference, final int limit) {
        return new BusinessSemanticQuery(
                BusinessSemanticQuery.Subject.TRADE,
                reference,
                EnumSet.of(
                        BusinessSemanticQuery.Field.TRADE_SERIAL_NUMBER,
                        BusinessSemanticQuery.Field.PRODUCT_CODE,
                        BusinessSemanticQuery.Field.TRADER_NAME,
                        BusinessSemanticQuery.Field.TRADE_DATE),
                limit);
    }

    /**
     * 创建产品语义查询并根据业务同义词选择标准字段。
     *
     * @param question 用户问题。
     * @param type 受控查询类型。
     * @param reference 已确认的产品引用。
     * @param limit 请求结果数量上限。
     * @return 产品语义查询。
     */
    private BusinessSemanticQuery productQuery(
            final String question,
            final QueryIntent.Type type,
            final String reference,
            final int limit) {
        final Set<BusinessSemanticQuery.Field> fields = EnumSet.of(
                BusinessSemanticQuery.Field.PRODUCT_CODE,
                BusinessSemanticQuery.Field.PRODUCT_NAME,
                BusinessSemanticQuery.Field.SNAPSHOT_DATE);
        if (type == QueryIntent.Type.PRODUCT_INVESTMENT_MANAGER_QUERY) {
            fields.add(BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER);
            fields.add(BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER);
        } else if (type == QueryIntent.Type.PRODUCT_MANAGER_QUERY) {
            fields.add(BusinessSemanticQuery.Field.PRODUCT_MANAGER);
        }
        addProductFields(question, fields);
        if (type == QueryIntent.Type.PRODUCT_LATEST_DOCUMENT_INFO) {
            fields.add(BusinessSemanticQuery.Field.PRODUCT_STATUS);
            fields.add(BusinessSemanticQuery.Field.PRODUCT_TIER);
            fields.add(BusinessSemanticQuery.Field.FEE_ADJUSTMENT_PLAN);
        }
        if (fields.size() == 3) {
            addBasicProductFields(fields);
        }
        return new BusinessSemanticQuery(BusinessSemanticQuery.Subject.PRODUCT, reference, fields, limit);
    }

    /**
     * 根据用户问题中的业务词汇加入标准产品字段。
     *
     * @param question 用户问题。
     * @param fields 待补充的标准字段集合。
     */
    private void addProductFields(
            final String question,
            final Set<BusinessSemanticQuery.Field> fields) {
        addWhenContains(question, "产品经理", BusinessSemanticQuery.Field.PRODUCT_MANAGER, fields);
        if (question.contains("投资经理")) {
            fields.add(BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER);
            fields.add(BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER);
        }
        addWhenContains(question, "形式", BusinessSemanticQuery.Field.PRODUCT_FORM, fields);
        addWhenContains(question, "状态", BusinessSemanticQuery.Field.PRODUCT_STATUS, fields);
        addWhenContains(question, "费率", BusinessSemanticQuery.Field.MANAGEMENT_FEE_RATE, fields);
        addWhenContains(question, "管理费", BusinessSemanticQuery.Field.MANAGEMENT_FEE_RATE, fields);
        addWhenContains(question, "成立", BusinessSemanticQuery.Field.START_DATE, fields);
        addWhenContains(question, "起息", BusinessSemanticQuery.Field.START_DATE, fields);
        addWhenContains(question, "到期", BusinessSemanticQuery.Field.END_DATE, fields);
        addWhenContains(question, "分层", BusinessSemanticQuery.Field.PRODUCT_TIER, fields);
        addWhenContains(question, "调整计划", BusinessSemanticQuery.Field.FEE_ADJUSTMENT_PLAN, fields);
    }

    /**
     * 当用户未指定具体字段时补齐一期产品基础信息字段。
     *
     * @param fields 待补充的标准字段集合。
     */
    private void addBasicProductFields(final Set<BusinessSemanticQuery.Field> fields) {
        fields.add(BusinessSemanticQuery.Field.PRODUCT_MANAGER);
        fields.add(BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER);
        fields.add(BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER);
        fields.add(BusinessSemanticQuery.Field.PRODUCT_FORM);
        fields.add(BusinessSemanticQuery.Field.PRODUCT_STATUS);
        fields.add(BusinessSemanticQuery.Field.START_DATE);
        fields.add(BusinessSemanticQuery.Field.END_DATE);
        fields.add(BusinessSemanticQuery.Field.MANAGEMENT_FEE_RATE);
    }

    /**
     * 当文本包含业务词汇时加入对应标准字段。
     *
     * @param question 用户问题。
     * @param keyword 业务词汇。
     * @param field 标准字段。
     * @param fields 待补充的标准字段集合。
     */
    private void addWhenContains(
            final String question,
            final String keyword,
            final BusinessSemanticQuery.Field field,
            final Set<BusinessSemanticQuery.Field> fields) {
        if (question.contains(keyword)) {
            fields.add(field);
        }
    }

    /**
     * 判断文本是否包含非空白内容。
     *
     * @param value 待判断文本。
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean hasText(final String value) {
        return value != null && !value.trim().isEmpty();
    }
}
