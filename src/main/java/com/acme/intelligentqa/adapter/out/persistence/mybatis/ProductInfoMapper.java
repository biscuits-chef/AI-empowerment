package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 产品信息快照持久化映射器，提供基于 {@code dws_product_info_d} 的单表及批量入库能力。
 */
@Mapper
public interface ProductInfoMapper extends BaseMapper<ProductInfoPersistenceRecord> {

    /**
     * 批量插入或按唯一键更新产品快照记录。
     *
     * @param list 待插入或更新的产品快照持久化记录列表。
     * @return 数据库受影响行数。
     */
    int upsertBatch(@Param("list") List<ProductInfoPersistenceRecord> list);
}
