package com.acme.intelligentqa.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.intelligentqa.domain.model.ProductInfoDto;
import com.acme.intelligentqa.domain.port.out.ProductSnapshotRepositoryPort;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 验证产品快照同步服务的编排调度与异常防御行为。
 */
class ProductSnapshotSyncServiceTest {

    /**
     * 模拟产品快照持久化出站端口。
     */
    private ProductSnapshotRepositoryPort repositoryPort;

    /**
     * 待测试的产品快照同步服务。
     */
    private ProductSnapshotSyncService service;

    /**
     * 初始化测试用例模拟对象。
     */
    @BeforeEach
    void setUp() {
        repositoryPort = mock(ProductSnapshotRepositoryPort.class);
        service = new ProductSnapshotSyncService(repositoryPort);
    }

    /**
     * 验证批量同步正常委派给仓储出站端口并返回影响行数。
     */
    @Test
    void syncsBatchSuccessfully() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P100");
        dto.setACCT_DT("2026-09-18");
        final List<ProductInfoDto> list = Collections.singletonList(dto);

        when(repositoryPort.saveBatch(eq(list), eq("2026-09-18"))).thenReturn(1);

        final int result = service.syncProductBatch(list, "2026-09-18");
        assertEquals(1, result);
        verify(repositoryPort).saveBatch(list, "2026-09-18");
    }

    /**
     * 验证输入为空列表或 null 时直接返回 0 且不调用仓储层。
     */
    @Test
    void returnsZeroForNullOrEmptyList() {
        assertEquals(0, service.syncProductBatch(null, "2026-09-18"));
        assertEquals(0, service.syncProductBatch(Collections.emptyList(), "2026-09-18"));
        verify(repositoryPort, never()).saveBatch(anyList(), eq("2026-09-18"));
    }
}
