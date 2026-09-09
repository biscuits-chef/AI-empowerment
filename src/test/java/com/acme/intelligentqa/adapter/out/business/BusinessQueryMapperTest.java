package com.acme.intelligentqa.adapter.out.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

/**
 * 验证产品快照表固定 MyBatis 语句的快照、产品匹配、日期条件和参数绑定行为。
 */
@ActiveProfiles("test")
@MybatisPlusTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:business-query;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml",
        "mybatis-plus.configuration.map-underscore-to-camel-case=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/db/business-query-test-schema.sql", config = @SqlConfig(encoding = "UTF-8"))
class BusinessQueryMapperTest {

    /** 产品和交易受控查询映射器。 */
    @Autowired
    private BusinessQueryMapper mapper;

    /**
     * 验证最新分区不会读取晚于业务日期的未来快照。
     */
    @Test
    void selectsLatestPartitionNotAfterBusinessDate() {
        assertEquals("2026-09-07", mapper.selectLatestPartitionDate("2026-09-08"));
        assertEquals("2026-09-01", mapper.selectLatestPartitionDate("2026-09-01"));
    }

    /**
     * 验证产品查询固定在指定快照并同时返回监管口径和产品部口径投资经理。
     */
    @Test
    void selectsBothInvestmentManagerScopesFromLatestSnapshot() {
        final String partitionDate = mapper.selectLatestPartitionDate("2026-09-08");
        final List<Map<String, Object>> rows = mapper.selectProductFacts("P001", partitionDate, 10);

        assertEquals(1, rows.size());
        assertEquals("产品经理甲", value(rows.get(0), "productManager"));
        assertEquals("监管投资经理甲", value(rows.get(0), "regulatoryInvestmentManager"));
        assertEquals("产品部投资经理甲", value(rows.get(0), "departmentInvestmentManager"));
        assertEquals("2026-09-07", value(rows.get(0), "snapshotDate"));
    }

    /**
     * 验证产品代码、名称、简称和全称均可作为已消歧产品的精确查询条件。
     */
    @Test
    void selectsProductByEveryRegisteredIdentityField() {
        final String partitionDate = "2026-09-07";

        assertEquals("P001", productCode(mapper.selectProductFacts("P001", partitionDate, 1)));
        assertEquals("P001", productCode(mapper.selectProductFacts("悦享3号", partitionDate, 1)));
        assertEquals("P001", productCode(mapper.selectProductFacts("悦享三号", partitionDate, 1)));
        assertEquals(
                "P001",
                productCode(mapper.selectProductFacts("悦享3号固定收益类理财产品", partitionDate, 1)));
    }

    /**
     * 验证模糊产品引用返回有界候选而不在数据库层静默选择第一条。
     */
    @Test
    void returnsBoundedProductCandidatesForAmbiguousReference() {
        final List<Map<String, Object>> rows = mapper.selectProductCandidates("悦享", "2026-09-07", 1);

        assertEquals(1, rows.size());
        assertTrue(String.valueOf(value(rows.get(0), "productName")).startsWith("悦享"));
    }

    /**
     * 验证恶意产品引用始终作为绑定值处理，不能改变固定查询结构。
     */
    @Test
    void bindsProductReferenceWithoutSqlInjection() {
        final String maliciousReference = "P001' OR '1'='1";

        assertTrue(mapper.selectProductCandidates(
                maliciousReference, "2026-09-07", 10).isEmpty());
        assertTrue(mapper.selectProductFacts(
                maliciousReference, "2026-09-07", 10).isEmpty());
    }

    /**
     * 验证日期列表使用“定开基准日或产品到期日”条件，并保留两个独立命中标志。
     */
    @Test
    void selectsReferenceDateProductsByOpenBaseOrMaturityDate() {
        final List<Map<String, Object>> rows = mapper.selectReferenceDateProducts(
                "2026-09-08", "2026-09-07", 10);

        assertEquals(3, rows.size());
        assertMatched(rows, "P002", 1, 0);
        assertMatched(rows, "P003", 0, 1);
        assertMatched(rows, "P004", 1, 1);
    }

    /**
     * 验证交易兼容查询仍绑定认证用户并遵守结果上限。
     */
    @Test
    void filtersTradeRowsByAuthenticatedOwnerAndLimit() {
        final List<Map<String, Object>> rows = mapper.selectTradeFacts("user-a", "T001", 1);

        assertEquals(1, rows.size());
        assertEquals("交易员甲", value(rows.get(0), "traderName"));
        assertTrue(mapper.selectTradeFacts("user-c", "T001", 1).isEmpty());
    }

    /**
     * 断言指定产品的两个日期命中标志。
     *
     * @param rows 日期产品查询结果。
     * @param productCode 产品代码。
     * @param openMatched 定开基准日期命中标志。
     * @param maturityMatched 产品到期日期命中标志。
     */
    private void assertMatched(
            final List<Map<String, Object>> rows,
            final String productCode,
            final int openMatched,
            final int maturityMatched) {
        final Map<String, Object> row = rowByProductCode(rows, productCode);
        assertEquals(openMatched, ((Number) value(row, "periodicOpenBaseDateMatched")).intValue());
        assertEquals(maturityMatched, ((Number) value(row, "productMaturityDateMatched")).intValue());
    }

    /**
     * 从单条产品查询结果读取产品代码。
     *
     * @param rows 产品查询结果。
     * @return 产品代码。
     */
    private String productCode(final List<Map<String, Object>> rows) {
        assertEquals(1, rows.size());
        return String.valueOf(value(rows.get(0), "productCode"));
    }

    /**
     * 按产品代码查找查询结果行。
     *
     * @param rows 查询结果。
     * @param productCode 产品代码。
     * @return 匹配的查询结果行。
     */
    private Map<String, Object> rowByProductCode(
            final List<Map<String, Object>> rows,
            final String productCode) {
        for (final Map<String, Object> row : rows) {
            if (productCode.equals(String.valueOf(value(row, "productCode")))) {
                return row;
            }
        }
        throw new AssertionError("未找到预期产品：" + productCode);
    }

    /**
     * 兼容测试数据库对结果别名大小写的处理并读取字段值。
     *
     * @param row 查询结果行。
     * @param column 结果别名。
     * @return 对应字段值，不存在时返回 null。
     */
    private Object value(final Map<String, Object> row, final String column) {
        for (final Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(column)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
