package com.acme.intelligentqa.adapter.out.business;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 执行一期批准的产品快照表和交易语义视图查询的 MyBatis 映射器。
 */
@Mapper
public interface BusinessQueryMapper {

    /**
     * 查询不晚于业务日期的最新产品快照分区日期。
     *
     * @param businessDate 业务日期，格式为 yyyy-MM-dd。
     * @return 最新分区日期；没有可用快照时返回 null。
     */
    String selectLatestPartitionDate(@Param("businessDate") String businessDate);

    /**
     * 按产品代码、名称、简称或全称检索产品候选项。
     *
     * @param entityReference 用户输入的产品引用。
     * @param partitionDate 已确定的快照分区日期，格式为 yyyy-MM-dd。
     * @param limit 结果数量上限。
     * @return 按精确匹配优先级排列的产品候选记录。
     */
    List<Map<String, Object>> selectProductCandidates(
            @Param("entityReference") String entityReference,
            @Param("partitionDate") String partitionDate,
            @Param("limit") int limit);

    /**
     * 按产品代码、名称、简称或全称精确查询产品事实。
     *
     * @param entityReference 已完成消歧的产品代码、名称、简称或全称。
     * @param partitionDate 已确定的快照分区日期，格式为 yyyy-MM-dd。
     * @param limit 结果数量上限。
     * @return 包含两种投资经理口径和日期字段的产品事实记录。
     */
    List<Map<String, Object>> selectProductFacts(
            @Param("entityReference") String entityReference,
            @Param("partitionDate") String partitionDate,
            @Param("limit") int limit);

    /**
     * 查询指定业务日期为定开基准日或产品到期日的产品。
     *
     * @param businessDate 业务日期，格式为 yyyy-MM-dd。
     * @param partitionDate 已确定的快照分区日期，格式为 yyyy-MM-dd。
     * @param limit 结果数量上限。
     * @return 命中日期条件的产品记录及对应命中标志。
     */
    List<Map<String, Object>> selectReferenceDateProducts(
            @Param("businessDate") String businessDate,
            @Param("partitionDate") String partitionDate,
            @Param("limit") int limit);

    /**
     * 按用户权限和交易流水号查询交易语义视图。
     *
     * @param ownerId 已认证用户所有者 ID。
     * @param entityReference 已消歧交易流水号。
     * @param limit 结果数量上限。
     * @return 交易语义视图记录。
     */
    List<Map<String, Object>> selectTradeFacts(
            @Param("ownerId") String ownerId,
            @Param("entityReference") String entityReference,
            @Param("limit") int limit);
}
