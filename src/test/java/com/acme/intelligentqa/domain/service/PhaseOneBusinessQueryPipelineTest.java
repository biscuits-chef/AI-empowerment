package com.acme.intelligentqa.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.domain.model.BusinessQueryPlan;
import com.acme.intelligentqa.domain.model.BusinessSemanticQuery;
import com.acme.intelligentqa.domain.model.CompiledBusinessQuery;
import com.acme.intelligentqa.domain.model.QueryIntent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 验证一期业务语义解析、关系约束、查询规划和固定语句编译管线。
 */
class PhaseOneBusinessQueryPipelineTest {

    /** 一期业务语义解析器。 */
    private final PhaseOneBusinessSemanticParser parser = new PhaseOneBusinessSemanticParser(
            Clock.fixed(Instant.parse("2026-09-08T03:00:00Z"), ZoneOffset.UTC));
    /** 一期可计算业务语义目录。 */
    private final PhaseOneBusinessSemanticCatalog catalog = new PhaseOneBusinessSemanticCatalog();
    /** 一期确定性业务查询规划器。 */
    private final PhaseOneBusinessQueryPlanner planner = new PhaseOneBusinessQueryPlanner(catalog);
    /** 一期固定语句 SQL 编译器。 */
    private final PhaseOneBusinessSqlCompiler compiler = new PhaseOneBusinessSqlCompiler();

    /**
     * 验证产品费率问题会转换为标准产品字段且不会产生物理 Schema 名称。
     */
    @Test
    void parsesProductFeeIntoApprovedSemanticFields() {
        final BusinessSemanticQuery query = parser.parse(
                "悦享3号的管理费率是多少",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO),
                8);

        assertEquals(BusinessSemanticQuery.Subject.PRODUCT, query.subject());
        assertEquals("P001", query.entityReference().orElse(null));
        assertEquals(8, query.limit());
        assertEquals(EnumSet.of(
                BusinessSemanticQuery.Field.PRODUCT_CODE,
                BusinessSemanticQuery.Field.PRODUCT_NAME,
                BusinessSemanticQuery.Field.MANAGEMENT_FEE_RATE,
                BusinessSemanticQuery.Field.SNAPSHOT_DATE), query.fields());
    }

    /**
     * 验证未指定字段的产品问题会落入一期基础信息字段集合。
     */
    @Test
    void suppliesApprovedBasicProductFieldsWhenMetricIsMissing() {
        final BusinessSemanticQuery query = parser.parse(
                "查询产品P001",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO),
                5);

        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.PRODUCT_MANAGER));
        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER));
        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER));
        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.START_DATE));
        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.END_DATE));
    }

    /**
     * 验证最新产品信息意图会加入状态、分层和费率计划字段。
     */
    @Test
    void enrichesLatestProductIntentWithCurrentInformationFields() {
        final BusinessSemanticQuery query = parser.parse(
                "P001最新说明书和调整计划",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_LATEST_DOCUMENT_INFO),
                4);

        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.PRODUCT_STATUS));
        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.PRODUCT_TIER));
        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.FEE_ADJUSTMENT_PLAN));
    }

    /**
     * 验证投资经理问题始终同时选择监管口径和产品部口径，并携带快照日期。
     */
    @Test
    void selectsBothInvestmentManagerScopes() {
        final BusinessSemanticQuery query = parser.parse(
                "悦享3号的投资经理是谁",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_INVESTMENT_MANAGER_QUERY),
                1);

        assertEquals(BusinessSemanticQuery.Operation.ENTITY_LOOKUP, query.operation());
        assertEquals(EnumSet.of(
                BusinessSemanticQuery.Field.PRODUCT_CODE,
                BusinessSemanticQuery.Field.PRODUCT_NAME,
                BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER,
                BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER,
                BusinessSemanticQuery.Field.SNAPSHOT_DATE), query.fields());
    }

    /**
     * 验证产品经理问题只选择产品经理及必要的产品和快照标识字段。
     */
    @Test
    void selectsProductManagerAndSnapshotFields() {
        final BusinessSemanticQuery query = parser.parse(
                "P001的产品经理是谁",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_MANAGER_QUERY),
                1);

        assertEquals(EnumSet.of(
                BusinessSemanticQuery.Field.PRODUCT_CODE,
                BusinessSemanticQuery.Field.PRODUCT_NAME,
                BusinessSemanticQuery.Field.PRODUCT_MANAGER,
                BusinessSemanticQuery.Field.SNAPSHOT_DATE), query.fields());
    }

    /**
     * 验证“今日”列表问题按上海时区解析固定业务日期并生成日期集合查询。
     */
    @Test
    void parsesTodayReferenceDateUsingShanghaiBusinessDate() {
        final BusinessSemanticQuery query = parser.parse(
                "今日基准日产品有哪些",
                new QueryIntent(
                        QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY,
                        1.0D,
                        Collections.emptyMap()),
                100);

        assertEquals(BusinessSemanticQuery.Operation.REFERENCE_DATE_LIST, query.operation());
        assertEquals(LocalDate.of(2026, 9, 8), query.businessDate().orElse(null));
        assertFalse(query.entityReference().isPresent());
        assertEquals(EnumSet.of(
                BusinessSemanticQuery.Field.PRODUCT_CODE,
                BusinessSemanticQuery.Field.PRODUCT_NAME,
                BusinessSemanticQuery.Field.PERIODIC_OPEN_BASE_DATE,
                BusinessSemanticQuery.Field.PRODUCT_MATURITY_DATE,
                BusinessSemanticQuery.Field.SNAPSHOT_DATE), query.fields());
    }

    /**
     * 验证显式业务日期只接受严格的 yyyy-MM-dd 格式和真实日历日期。
     */
    @Test
    void rejectsInvalidBusinessDateFormatAndValue() {
        assertThrows(DateTimeParseException.class, () -> parser.parse(
                "20260908有哪些基准日产品",
                intent("businessDate", "20260908", QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY),
                10));
        assertThrows(DateTimeParseException.class, () -> parser.parse(
                "2026-02-30有哪些到期产品",
                intent("businessDate", "2026-02-30", QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY),
                10));
    }

    /**
     * 验证交易问题只产生一期批准的交易计划字段。
     */
    @Test
    void parsesTradeReferenceIntoTradeSemanticQuery() {
        final BusinessSemanticQuery query = parser.parse(
                "交易流水号T001对应的交易员是谁",
                intent("tradeReference", "T001", QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO),
                3);

        assertEquals(BusinessSemanticQuery.Subject.TRADE, query.subject());
        assertEquals("T001", query.entityReference().orElse(null));
        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.TRADER_NAME));
        assertTrue(query.fields().contains(BusinessSemanticQuery.Field.PRODUCT_CODE));
    }

    /**
     * 验证解析器会拒绝空问题、不支持意图和缺少已消歧实体的请求。
     */
    @Test
    void rejectsQueriesThatCannotBecomeDeterministicPlans() {
        final QueryIntent product = intent(
                "productReference", "P001", QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO);
        assertThrows(IllegalArgumentException.class, () -> parser.parse(" ", product, 1));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "问题", new QueryIntent(QueryIntent.Type.UNSUPPORTED, 1.0D, Collections.emptyMap()), 1));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "问题", new QueryIntent(
                        QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap()), 1));
    }

    /**
     * 验证一期语义目录会拒绝实体不允许的字段并仅开放交易到产品关系。
     */
    @Test
    void enforcesSemanticFieldAndRelationshipCatalog() {
        assertThrows(IllegalArgumentException.class, () -> catalog.validateFields(
                BusinessSemanticQuery.Subject.PRODUCT,
                EnumSet.of(BusinessSemanticQuery.Field.TRADER_NAME)));
        assertTrue(catalog.supportsRelationship(
                BusinessSemanticQuery.Subject.TRADE, BusinessSemanticQuery.Subject.PRODUCT));
        assertFalse(catalog.supportsRelationship(
                BusinessSemanticQuery.Subject.PRODUCT, BusinessSemanticQuery.Subject.TRADE));
    }

    /**
     * 验证规划器限制结果数量并生成可追溯计划，编译器只选择批准语句。
     */
    @Test
    void plansAndCompilesOnlyApprovedPhysicalStatement() {
        final BusinessSemanticQuery query = parser.parse(
                "P001的费率",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO),
                100);
        final BusinessQueryPlan plan = planner.plan(query, "phase1-v7", 20);
        final CompiledBusinessQuery compiled = compiler.compile(plan);

        assertEquals(BusinessQueryPlan.Type.PRODUCT_LOOKUP, plan.type());
        assertEquals("phase1-v7:PRODUCT_LOOKUP", plan.planId());
        assertEquals(20, plan.limit());
        assertEquals(CompiledBusinessQuery.Statement.SELECT_PRODUCT_FACTS, compiled.statement());
        assertEquals("P001", compiled.entityReference().orElse(null));
        assertEquals(plan.fields(), compiled.fields());
    }

    /**
     * 验证日期列表查询会应用结果上限并编译为唯一批准的日期产品语句。
     */
    @Test
    void plansAndCompilesReferenceDateList() {
        final BusinessSemanticQuery query = parser.parse(
                "2026-09-08有哪些定开基准日或到期产品",
                intent("businessDate", "2026-09-08", QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY),
                100);
        final BusinessQueryPlan plan = planner.plan(query, "phase1-v8", 20);
        final CompiledBusinessQuery compiled = compiler.compile(plan);

        assertEquals(BusinessQueryPlan.Type.PRODUCT_REFERENCE_DATE_LIST, plan.type());
        assertEquals("phase1-v8:PRODUCT_REFERENCE_DATE_LIST", plan.planId());
        assertEquals(LocalDate.of(2026, 9, 8), plan.businessDate().orElse(null));
        assertEquals(20, plan.limit());
        assertEquals(
                CompiledBusinessQuery.Statement.SELECT_REFERENCE_DATE_PRODUCTS,
                compiled.statement());
        assertFalse(compiled.entityReference().isPresent());
    }

    /**
     * 验证交易计划编译为唯一批准的交易查询语句。
     */
    @Test
    void compilesTradePlanIntoApprovedTradeStatement() {
        final BusinessSemanticQuery query = parser.parse(
                "T001交易员",
                intent("tradeReference", "T001", QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO),
                10);
        final BusinessQueryPlan plan = planner.plan(query, "phase1-v1", 10);
        final CompiledBusinessQuery compiled = compiler.compile(plan);

        assertEquals(BusinessQueryPlan.Type.TRADE_LOOKUP, plan.type());
        assertEquals(CompiledBusinessQuery.Statement.SELECT_TRADE_FACTS, compiled.statement());
        assertEquals("phase1-v1", compiled.semanticModelVersion());
    }

    /**
     * 创建包含一个已确认实体的查询意图。
     *
     * @param key 实体字段名。
     * @param value 实体字段值。
     * @param type 查询意图类型。
     * @return 包含一个已确认实体的查询意图。
     */
    private QueryIntent intent(final String key, final String value, final QueryIntent.Type type) {
        final Map<String, String> entities = new LinkedHashMap<>();
        entities.put(key, value);
        return new QueryIntent(type, 1.0D, entities);
    }
}
