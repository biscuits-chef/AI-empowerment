package com.acme.intelligentqa.adapter.out.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.BusinessQueryProperties;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 验证产品代码、名称、简称和全称进入业务查询前的实体解析边界。
 */
class ProductEntityResolutionAdapterTest {

    /** 固定测试时钟，确保“今日”不会随测试运行日期变化。 */
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-08T03:00:00Z"), ZoneOffset.UTC);

    /**
     * 验证实体解析先确定最新快照，再返回数据库候选供上游消歧。
     */
    @Test
    void resolvesCandidatesFromLatestBusinessSnapshot() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);
        when(mapper.selectLatestPartitionDate("2026-09-08")).thenReturn("2026-09-07");
        when(mapper.selectProductCandidates("悦享", "2026-09-07", 9))
                .thenReturn(rows(
                        row("P001", "悦享3号"),
                        row("P002", "悦享6号")));

        final List<EntityCandidate> candidates = adapter(mapper, true, false).resolve("user-a", "悦享");

        assertEquals(2, candidates.size());
        assertEquals("P001", candidates.get(0).reference());
        assertEquals("悦享3号（P001）", candidates.get(0).label());
        assertEquals("P002", candidates.get(1).reference());
        verify(mapper).selectLatestPartitionDate("2026-09-08");
        verify(mapper).selectProductCandidates("悦享", "2026-09-07", 9);
    }

    /**
     * 验证不存在有效快照时返回空候选，且不会继续执行产品扫描。
     */
    @Test
    void stopsResolutionWhenNoSnapshotExists() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);
        when(mapper.selectLatestPartitionDate("2026-09-08")).thenReturn(null);

        assertTrue(adapter(mapper, true, false).resolve("user-a", "P001").isEmpty());

        verify(mapper).selectLatestPartitionDate("2026-09-08");
        verifyNoMoreInteractions(mapper);
    }

    /**
     * 验证非演示环境未启用业务查询时失败关闭而不是猜测产品。
     */
    @Test
    void failsClosedWhenBusinessQueryIsDisabled() {
        final BusinessQueryMapper mapper = mock(BusinessQueryMapper.class);

        final DependencyUnavailableException exception = assertThrows(
                DependencyUnavailableException.class,
                () -> adapter(mapper, false, false).resolve("user-a", "P001"));

        assertEquals("BUSINESS_QUERY_UNCONFIGURED", exception.errorCode());
        verifyNoMoreInteractions(mapper);
    }

    /**
     * 创建产品实体解析适配器。
     *
     * @param mapper 产品查询映射器。
     * @param enabled 是否启用真实业务查询。
     * @param demoMode 是否启用开发演示模式。
     * @return 产品实体解析适配器。
     */
    private ProductEntityResolutionAdapter adapter(
            final BusinessQueryMapper mapper,
            final boolean enabled,
            final boolean demoMode) {
        return new ProductEntityResolutionAdapter(
                new BusinessQueryProperties(enabled, 20, "phase1-v8"),
                qaProperties(demoMode),
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
     * 创建产品候选物理查询行。
     *
     * @param productCode 产品代码。
     * @param productName 产品名称。
     * @return 产品候选物理查询行。
     */
    private Map<String, Object> row(final String productCode, final String productName) {
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("productCode", productCode);
        row.put("productName", productName);
        return row;
    }

    /**
     * 创建可变的候选物理查询行列表。
     *
     * @param values 候选物理查询行。
     * @return 候选物理查询行列表。
     */
    @SafeVarargs
    private final List<Map<String, Object>> rows(final Map<String, Object>... values) {
        final List<Map<String, Object>> rows = new ArrayList<>();
        Collections.addAll(rows, values);
        return rows;
    }
}
