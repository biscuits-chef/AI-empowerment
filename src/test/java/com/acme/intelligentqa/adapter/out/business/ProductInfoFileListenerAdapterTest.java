package com.acme.intelligentqa.adapter.out.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.acme.intelligentqa.domain.model.ProductInfoDto;
import com.acme.intelligentqa.domain.port.in.ProductSnapshotSyncUseCase;
import com.joyintech.datahub.model.FileMetadata;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 验证产品信息批量数据下发监听适配器的调度与属性行为。
 */
class ProductInfoFileListenerAdapterTest {

    /**
     * 模拟产品快照同步用例。
     */
    private ProductSnapshotSyncUseCase useCase;

    /**
     * 待测试的产品信息监听适配器。
     */
    private ProductInfoFileListenerAdapter adapter;

    /**
     * 初始化测试组件。
     */
    @BeforeEach
    void setUp() {
        useCase = mock(ProductSnapshotSyncUseCase.class);
        adapter = new ProductInfoFileListenerAdapter(useCase);
    }

    /**
     * 验证监听器返回预期的数据类、数据编码与版本。
     */
    @Test
    void assertsDataClassAndCodeAndVersion() {
        assertEquals(ProductInfoDto.class, adapter.getDataClass());
        assertEquals("ZH-0982-01-c463f8c51bb7", adapter.getDataCode());
        assertEquals("V1.0", adapter.getDataVersion());
    }

    /**
     * 验证批量下发数据成功传递至同步用例。
     */
    @Test
    void onBatchDataDelegatesToSyncUseCase() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P200");
        final List<ProductInfoDto> list = Collections.singletonList(dto);

        final FileMetadata metadata = new FileMetadata();
        metadata.setDataDate("2026-09-18");

        adapter.onBatchData(list, metadata);

        verify(useCase).syncProductBatch(list, "2026-09-18");
    }
}
