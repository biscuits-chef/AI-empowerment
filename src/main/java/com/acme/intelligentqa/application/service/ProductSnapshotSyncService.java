package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.domain.model.ProductInfoDto;
import com.acme.intelligentqa.domain.port.in.ProductSnapshotSyncUseCase;
import com.acme.intelligentqa.domain.port.out.ProductSnapshotRepositoryPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产品快照同步服务，编排产品快照批量校验与持久化。
 */
@Service
public class ProductSnapshotSyncService implements ProductSnapshotSyncUseCase {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(ProductSnapshotSyncService.class);

    /**
     * 产品快照持久化出站端口。
     */
    private final ProductSnapshotRepositoryPort productSnapshotRepositoryPort;

    /**
     * 构造产品快照同步服务。
     *
     * @param productSnapshotRepositoryPort 产品快照持久化出站端口。
     */
    public ProductSnapshotSyncService(final ProductSnapshotRepositoryPort productSnapshotRepositoryPort) {
        this.productSnapshotRepositoryPort = productSnapshotRepositoryPort;
    }

    /**
     * 同步批量产品信息快照至数据库。
     *
     * @param productList 产品信息列表。
     * @param fallbackSnapshotDate 兜底快照分区日期（格式 yyyy-MM-dd）。
     * @return 实际持久化或更新影响的行数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncProductBatch(final List<ProductInfoDto> productList, final String fallbackSnapshotDate) {
        if (productList == null || productList.isEmpty()) {
            log.info("接收到的产品列表为空，跳过持久化处理");
            return 0;
        }
        log.info("开始同步产品快照数据，记录数：{}，兜底日期：{}", productList.size(), fallbackSnapshotDate);
        final int affected = productSnapshotRepositoryPort.saveBatch(productList, fallbackSnapshotDate);
        log.info("产品快照数据同步完成，持久化/更新影响行数：{}", affected);
        return affected;
    }
}
