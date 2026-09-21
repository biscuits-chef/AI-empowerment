package com.acme.intelligentqa.domain.port.in;

import com.acme.intelligentqa.domain.model.ProductInfoDto;
import java.util.List;

/**
 * 产品快照同步用例，定义接收并持久化外部订阅或文件下发的产品信息的业务入口。
 */
public interface ProductSnapshotSyncUseCase {

    /**
     * 同步批量产品信息快照至数据库。
     *
     * @param productList 产品信息列表。
     * @param fallbackSnapshotDate 兜底快照分区日期（格式 yyyy-MM-dd）。
     * @return 实际同步处理的影响行数或记录数。
     */
    int syncProductBatch(List<ProductInfoDto> productList, String fallbackSnapshotDate);
}
