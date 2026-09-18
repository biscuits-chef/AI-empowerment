package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.ProductInfoDto;
import java.util.List;

/**
 * 产品快照持久化出站端口，定义将产品信息快照保存或批量更新到数据存储的契约。
 */
public interface ProductSnapshotRepositoryPort {

    /**
     * 批量持久化产品信息快照，支持冲突覆盖更新。
     *
     * @param productList 待持久化的产品信息列表。
     * @param fallbackSnapshotDate 兜底快照分区日期（格式 yyyy-MM-dd），在 DTO 缺少分区日期时使用。
     * @return 实际影响行数或持久化记录数。
     */
    int saveBatch(List<ProductInfoDto> productList, String fallbackSnapshotDate);
}
