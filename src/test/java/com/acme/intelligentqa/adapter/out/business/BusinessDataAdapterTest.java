package com.acme.intelligentqa.adapter.out.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.BusinessQueryProperties;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessQueryPlanner;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessSemanticCatalog;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessSemanticParser;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessSqlCompiler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;

/**
 * 验证一期业务数据适配器的权限参数、固定语句选择、事实输出和失败关闭行为。
 */
class BusinessDataAdapterTest {

    /** 固定测试时钟，确保最新快照查询使用稳定业务日期。 */
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-08T03:00:00Z"), ZoneOffset.UTC);

    /**
     * 验证物理视图未映射管理费率时失败关闭且不执行数据库查询。
     */
    @Test
    void failsClosedWhenRequestedFieldHasNoApprovedPhysicalMapping() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);

        final DependencyUnavailableException exception = assertThrows(
                DependencyUnavailableException.class,
                () -> adapter(mapper, true, false).query(
                        "user-a",
                        "P001的管理费率",
                        intent("productReference", "P001"),
                        8));

        assertEquals("BUSINESS_FIELD_MAPPING_UNCONFIGURED", exception.errorCode());
        verifyNoInteractions(mapper);
    }

    /**
     * 验证投资经理查询同时输出监管和产品部两个口径及数据快照日期。
     */
    @Test
    void returnsBothInvestmentManagerScopes() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("productCode", "P001");
        row.put("productName", "悦享3号");
        row.put("regulatoryInvestmentManager", "监管投资经理甲");
        row.put("departmentInvestmentManager", "产品部投资经理甲");
        row.put("snapshotDate", "2026-09-07");
        when(mapper.selectLatestPartitionDate("2026-09-08")).thenReturn("2026-09-07");
        when(mapper.selectProductFacts("P001", "2026-09-07", 1))
                .thenReturn(Collections.singletonList(row));

        final List<BusinessFact> facts = adapter(mapper, true, false).query(
                "user-a",
                "P001的投资经理是谁",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_INVESTMENT_MANAGER_QUERY),
                1);

        assertEquals(1, facts.size());
        assertTrue(facts.get(0).content().contains("监管口径投资经理=监管投资经理甲"));
        assertTrue(facts.get(0).content().contains("产品部口径投资经理=产品部投资经理甲"));
        assertTrue(facts.get(0).content().contains("数据日期=2026-09-07"));
    }

    /**
     * 验证缺失某个投资经理口径时明确输出“暂未维护”而不是省略口径。
     */
    @Test
    void marksMissingInvestmentManagerScopeAsNotMaintained() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("productCode", "P001");
        row.put("productName", "悦享3号");
        row.put("regulatoryInvestmentManager", "监管投资经理甲");
        row.put("departmentInvestmentManager", null);
        row.put("snapshotDate", "2026-09-07");
        when(mapper.selectLatestPartitionDate("2026-09-08")).thenReturn("2026-09-07");
        when(mapper.selectProductFacts("P001", "2026-09-07", 1))
                .thenReturn(Collections.singletonList(row));

        final List<BusinessFact> facts = adapter(mapper, true, false).query(
                "user-a",
                "P001的投资经理是谁",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_INVESTMENT_MANAGER_QUERY),
                1);

        assertTrue(facts.get(0).content().contains("产品部口径投资经理=暂未维护"));
    }

    /**
     * 验证产品经理问题只输出产品经理及必要标识，不混入两种投资经理口径。
     */
    @Test
    void returnsOnlyRequestedProductManager() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("productCode", "P001");
        row.put("productName", "悦享3号");
        row.put("productManager", "产品经理甲");
        row.put("regulatoryInvestmentManager", "不应输出");
        row.put("departmentInvestmentManager", "不应输出");
        row.put("snapshotDate", "2026-09-07");
        when(mapper.selectLatestPartitionDate("2026-09-08")).thenReturn("2026-09-07");
        when(mapper.selectProductFacts("P001", "2026-09-07", 1))
                .thenReturn(Collections.singletonList(row));

        final List<BusinessFact> facts = adapter(mapper, true, false).query(
                "user-a",
                "P001的产品经理是谁",
                intent("productReference", "P001", QueryIntent.Type.PRODUCT_MANAGER_QUERY),
                1);

        assertTrue(facts.get(0).content().contains("产品经理=产品经理甲"));
        assertTrue(!facts.get(0).content().contains("不应输出"));
    }

    /**
     * 验证日期列表查询绑定显式业务日期、最新快照并保留双命中原因。
     */
    @Test
    void returnsBothReferenceDateMatchReasons() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("productCode", "P004");
        row.put("productName", "双命中产品");
        row.put("periodicOpenBaseDate", "2026-09-08");
        row.put("productMaturityDate", "2026-09-08");
        row.put("snapshotDate", "2026-09-07");
        row.put("periodicOpenBaseDateMatched", 1);
        row.put("productMaturityDateMatched", 1);
        when(mapper.selectLatestPartitionDate("2026-09-08")).thenReturn("2026-09-07");
        when(mapper.selectReferenceDateProducts("2026-09-08", "2026-09-07", 5))
                .thenReturn(Collections.singletonList(row));

        final List<BusinessFact> facts = adapter(mapper, true, false).query(
                "user-a",
                "2026-09-08有哪些基准日或到期产品",
                intent("businessDate", "2026-09-08", QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY),
                5);

        verify(mapper).selectReferenceDateProducts("2026-09-08", "2026-09-07", 5);
        assertEquals(1, facts.size());
        assertTrue(facts.get(0).content().contains("命中原因=定开基准日、产品到期日"));
    }

    /**
     * 验证交易查询选择固定交易语句并绑定认证用户和交易流水号。
     */
    @Test
    void queriesApprovedTradeViewWithAuthenticatedOwner() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("tradeSerialNumber", "T001");
        row.put("productCode", "P001");
        row.put("traderName", "交易员甲");
        when(mapper.selectTradeFacts("user-a", "T001", 8)).thenReturn(Collections.singletonList(row));

        final List<BusinessFact> facts = adapter(mapper, true, false).query(
                "user-a",
                "交易流水号T001对应的交易员",
                intent("tradeReference", "T001"),
                8);

        verify(mapper).selectTradeFacts("user-a", "T001", 8);
        assertEquals(1, facts.size());
        assertTrue(facts.get(0).content().contains("交易员=交易员甲"));
    }

    /**
     * 验证开发演示模式在通道未配置时返回空结果，非演示环境则失败关闭。
     */
    @Test
    void failsClosedWhenBusinessQueryIsDisabledOutsideDemo() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);

        assertTrue(adapter(mapper, false, true).query(
                "user-a", "问题", intent("productReference", "P001"), 1).isEmpty());
        final DependencyUnavailableException exception = assertThrows(
                DependencyUnavailableException.class,
                () -> adapter(mapper, false, false).query(
                        "user-a", "问题", intent("productReference", "P001"), 1));
        assertEquals("BUSINESS_QUERY_UNCONFIGURED", exception.errorCode());
    }

    /**
     * 验证数据库访问异常会转换为稳定依赖错误而不会泄露 SQL。
     */
    @Test
    void convertsDatabaseFailureIntoStableDependencyError() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);
        when(mapper.selectLatestPartitionDate(anyString()))
                .thenThrow(new TransientDataAccessResourceException("database detail"));

        final DependencyUnavailableException exception = assertThrows(
                DependencyUnavailableException.class,
                () -> adapter(mapper, true, false).query(
                        "user-a", "P001产品经理", intent(
                                "productReference", "P001", QueryIntent.Type.PRODUCT_MANAGER_QUERY), 1));

        assertEquals("BUSINESS_QUERY_FAILED", exception.errorCode());
        assertEquals("approved business query execution failed", exception.getMessage());
    }

    /**
     * 创建业务数据适配器。
     *
     * @param mapper 一期业务语义视图映射器。
     * @param enabled 是否启用真实业务查询通道。
     * @param demoMode 是否启用开发演示模式。
     * @return 业务数据适配器。
     */
    private BusinessDataAdapter adapter(
            final BusinessQueryMapper mapper,
            final boolean enabled,
            final boolean demoMode) {
        final PhaseOneBusinessSemanticCatalog catalog = new PhaseOneBusinessSemanticCatalog();
        return new BusinessDataAdapter(
                qaProperties(demoMode),
                new BusinessQueryProperties(enabled, 20, "phase1-v1"),
                new PhaseOneBusinessSemanticParser(),
                new PhaseOneBusinessQueryPlanner(catalog),
                new PhaseOneBusinessSqlCompiler(),
                mapper,
                FIXED_CLOCK);
    }

    /**
     * 创建问答流程测试配置。
     *
     * @param demoMode 是否启用开发演示模式。
     * @return 问答流程测试配置。
     */
    private QaProperties qaProperties(final boolean demoMode) {
        return new QaProperties(demoMode, 4000, 30000, 8, 40000, 20000, 20, 0.85D, 512, 1, 2, 10);
    }

    /**
     * 创建包含一个已确认实体的基础信息查询意图。
     *
     * @param key 实体字段名。
     * @param value 实体字段值。
     * @return 包含一个已确认实体的查询意图。
     */
    private QueryIntent intent(final String key, final String value) {
        return intent(key, value, QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO);
    }

    /**
     * 创建包含一个已确认实体的指定类型查询意图。
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
