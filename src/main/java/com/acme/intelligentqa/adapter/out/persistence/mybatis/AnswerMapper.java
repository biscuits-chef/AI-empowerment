package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import java.sql.Timestamp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 回答表的原生 MyBatis 映射器。 */
@Mapper
public interface AnswerMapper {

    /**
     * 新增回答并回填数据库自增主键。
     *
     * @param record 待新增的回答记录。
     * @return 受影响行数。
     */
    int insert(AnswerPersistenceRecord record);

    /**
     * 按用户与幂等键读取已受理记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 按用户与幂等键读取已受理记录。
     */
    AnswerPersistenceRecord selectByIdempotencyKey(
            @Param("ownerId") String ownerId,
            @Param("idempotencyKey") String idempotencyKey);

    /**
     * 按用户与唯一标识读取记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param id 唯一标识。
     *
     * @return 按用户与唯一标识读取记录。
     */
    AnswerPersistenceRecord selectByOwnerAndId(
            @Param("ownerId") String ownerId,
            @Param("id") String id);

    /**
     * 读取原始用户问题。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param id 唯一标识。
     *
     * @return 读取原始用户问题。
     */
    String selectQuestion(@Param("ownerId") String ownerId, @Param("id") String id);

    /**
     * 按允许的活动源状态迁移回答状态。
     *
     * @param id 回答公开标识。
     * @param status 目标状态。
     * @return 受影响行数。
     */
    int transitionStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 幂等保存公司 HiAgent 应用会话 ID。
     *
     * @param id 回答公开标识。
     * @param appConversationId 公司 HiAgent 应用会话 ID。
     * @return 受影响行数。
     */
    int recordAppConversationId(
            @Param("id") String id,
            @Param("appConversationId") String appConversationId);

    /**
     * 在生成状态下保存部分回答正文。
     *
     * @param id 回答公开标识。
     * @param content 部分回答正文。
     * @return 受影响行数。
     */
    int savePartial(@Param("id") String id, @Param("content") String content);

    /**
     * 按活动源状态收敛回答终态。
     *
     * @param id 回答公开标识。
     * @param status 目标终态。
     * @param content 最终或部分回答正文。
     * @param errorCode 稳定错误码，正常完成时为空。
     * @param completedAt 完成时间。
     * @return 受影响行数。
     */
    int finalizeAnswer(
            @Param("id") String id,
            @Param("status") String status,
            @Param("content") String content,
            @Param("errorCode") String errorCode,
            @Param("completedAt") Timestamp completedAt);

}
