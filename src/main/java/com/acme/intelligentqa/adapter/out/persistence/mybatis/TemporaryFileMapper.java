package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import java.sql.Timestamp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 临时文件表的原生 MyBatis 映射器。
 */
@Mapper
public interface TemporaryFileMapper {

    /**
     * 新增临时文件并回填数据库自增主键。
     *
     * @param record 待新增的临时文件记录。
     * @return 受影响行数。
     */
    int insert(TemporaryFilePersistenceRecord record);

    /**
     * 按用户与幂等键读取未删除文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param idempotencyKey 上传幂等键。
     * @return 匹配的文件记录，不存在时为空。
     */
    TemporaryFilePersistenceRecord selectByIdempotencyKey(
            @Param("ownerId") String ownerId,
            @Param("idempotencyKey") String idempotencyKey);

    /**
     * 按公开标识读取未删除文件。
     *
     * @param id 文件公开标识。
     * @return 匹配的文件记录，不存在时为空。
     */
    TemporaryFilePersistenceRecord selectById(@Param("id") String id);

    /**
     * 按用户、会话和公开标识读取未删除文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话公开标识。
     * @param id 文件公开标识。
     * @return 匹配的文件记录，不存在时为空。
     */
    TemporaryFilePersistenceRecord selectOwnedActive(
            @Param("ownerId") String ownerId,
            @Param("conversationId") String conversationId,
            @Param("id") String id);

    /**
     * 更新未删除文件的处理状态。
     *
     * @param id 文件公开标识。
     * @param status 新处理状态。
     * @param updatedAt 更新时间。
     * @return 受影响行数。
     */
    int updateStatus(
            @Param("id") String id,
            @Param("status") String status,
            @Param("updatedAt") Timestamp updatedAt);

    /**
     * 按归属条件将文件标记为等待删除。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话公开标识。
     * @param id 文件公开标识。
     * @param deletedAt 删除时间。
     * @return 受影响行数。
     */
    int markDeletePending(
            @Param("ownerId") String ownerId,
            @Param("conversationId") String conversationId,
            @Param("id") String id,
            @Param("deletedAt") Timestamp deletedAt);

    /**
     * 按用户和会话读取有界活动文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param limit 数量上限。
     * @return 临时文件记录列表。
     */
    List<TemporaryFilePersistenceRecord> selectActive(
            @Param("ownerId") String ownerId,
            @Param("conversationId") String conversationId,
            @Param("limit") int limit);
}
