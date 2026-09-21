package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.ProductInfoMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ProductInfoPersistenceRecord;
import com.acme.intelligentqa.domain.model.ProductInfoDto;
import com.acme.intelligentqa.domain.port.out.ProductSnapshotRepositoryPort;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 基于 MyBatis 的产品快照持久化仓储实现，执行分批入库与幂等冲突覆盖。
 */
@Repository
public class MybatisProductSnapshotRepository implements ProductSnapshotRepositoryPort {

    /**
     * 单批次入库最大记录数，防止超大 SQL 导致连接超时或数据包溢出。
     */
    private static final int BATCH_SIZE = 500;

    /**
     * 产品快照 MyBatis 映射器。
     */
    private final ProductInfoMapper productInfoMapper;

    /**
     * 产品记录转换映射器。
     */
    private final ProductInfoRecordMapper productInfoRecordMapper;

    /**
     * 构造基于 MyBatis 的产品快照持久化仓储。
     *
     * @param productInfoMapper 产品快照 MyBatis 映射器。
     * @param productInfoRecordMapper 产品记录转换映射器。
     */
    public MybatisProductSnapshotRepository(
            final ProductInfoMapper productInfoMapper,
            final ProductInfoRecordMapper productInfoRecordMapper) {
        this.productInfoMapper = productInfoMapper;
        this.productInfoRecordMapper = productInfoRecordMapper;
    }

    /**
     * 批量持久化产品信息快照，支持冲突覆盖更新。
     *
     * @param productList 待持久化的产品信息列表。
     * @param fallbackSnapshotDate 兜底快照分区日期（格式 yyyy-MM-dd）。
     * @return 实际影响行数。
     */
    @Override
    public int saveBatch(final List<ProductInfoDto> productList, final String fallbackSnapshotDate) {
        if (productList == null || productList.isEmpty()) {
            return 0;
        }

        final List<ProductInfoPersistenceRecord> records =
                productInfoRecordMapper.toRecords(productList, fallbackSnapshotDate);
        if (records.isEmpty()) {
            return 0;
        }

        int totalAffected = 0;
        final int totalSize = records.size();
        for (int fromIndex = 0; fromIndex < totalSize; fromIndex += BATCH_SIZE) {
            final int toIndex = Math.min(fromIndex + BATCH_SIZE, totalSize);
            final List<ProductInfoPersistenceRecord> subList = records.subList(fromIndex, toIndex);
            totalAffected += productInfoMapper.upsertBatch(subList);
        }
        return totalAffected;
    }
}
