package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import java.sql.Timestamp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 会话表的原生 MyBatis 映射器。 */
@Mapper
public interface ConversationMapper {

    /**
     * 新增会话并回填数据库自增主键。
     *
     * @param record 待新增的会话记录。
     * @return 受影响行数。
     */
    int insert(ConversationPersistenceRecord record);

    /**
     * 按用户和首次提问幂等键读取有效会话。
     *
     * @param ownerId 用户所有者 ID。
     * @param idempotencyKey 首次提问幂等键。
     * @return 匹配的有效会话，不存在时为空。
     */
    ConversationPersistenceRecord selectByCreationKey(
            @Param("ownerId") String ownerId,
            @Param("idempotencyKey") String idempotencyKey);

    /**
     * 按用户和公开标识读取有效会话。
     *
     * @param ownerId 用户所有者 ID。
     * @param id 会话公开标识。
     * @return 匹配的有效会话，不存在时为空。
     */
    ConversationPersistenceRecord selectActive(
            @Param("ownerId") String ownerId,
            @Param("id") String id);

    /**
     * 按所属用户读取有界记录列表。
     *
     * @param ownerId 用户所有者 ID。
     * @param beforeUpdatedAt 上一页末项更新时间，首页为空。
     * @param beforeId 上一页末项 ID，首页为空。
     * @param limit 数量上限。
     * @return 按所属用户读取有界记录列表。
     */
    List<ConversationPersistenceRecord> selectByOwner(
            @Param("ownerId") String ownerId,
            @Param("beforeUpdatedAt") Timestamp beforeUpdatedAt,
            @Param("beforeId") String beforeId,
            @Param("limit") int limit);

    /**
     * 统计当前会话未进入终态的回答数量。
     *
     * @param ownerId 用户所有者 ID。
     * @param id 会话唯一标识。
     * @return 活动回答数量。
     */
    int countActiveAnswers(@Param("ownerId") String ownerId, @Param("id") String id);

    /**
     * 在当前事务中锁定并读取有效会话。
     *
     * @param ownerId 用户所有者 ID。
     * @param id 会话唯一标识。
     * @return 存在且归属匹配时返回会话记录。
     */
    ConversationPersistenceRecord selectActiveForUpdate(
            @Param("ownerId") String ownerId,
            @Param("id") String id);

    /**
     * 查询带回答状态的历史消息。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param id 唯一标识。
     *
     * @param limit 数量上限。
     *
     * @return 查询带回答状态的历史消息。
     */
    List<ChatMessagePersistenceRecord> selectMessages(
            @Param("ownerId") String ownerId,
            @Param("id") String id,
            @Param("limit") int limit);

    /**
     * 查询指定历史用户消息随问题提交的附件元数据。
     *
     * @param ownerId 用户所有者 ID。
     * @param id 会话唯一标识。
     * @param messageIds 本次有界历史中的用户消息 ID。
     * @return 按用户消息和关联创建顺序排列的附件元数据。
     */
    List<MessageAttachmentPersistenceRecord> selectMessageAttachments(
            @Param("ownerId") String ownerId,
            @Param("id") String id,
            @Param("messageIds") List<String> messageIds);

    /**
     * 修改有效会话名称和更新时间。
     *
     * @param ownerId 用户所有者 ID。
     * @param id 会话公开标识。
     * @param title 新会话名称。
     * @param updatedAt 更新时间。
     * @return 受影响行数。
     */
    int rename(
            @Param("ownerId") String ownerId,
            @Param("id") String id,
            @Param("title") String title,
            @Param("updatedAt") Timestamp updatedAt);

    /**
     * 对有效会话执行逻辑删除。
     *
     * @param ownerId 用户所有者 ID。
     * @param id 会话公开标识。
     * @param deletedAt 删除时间。
     * @return 受影响行数。
     */
    int softDelete(
            @Param("ownerId") String ownerId,
            @Param("id") String id,
            @Param("deletedAt") Timestamp deletedAt);

    /**
     * 更新有效会话最后活动时间。
     *
     * @param id 会话公开标识。
     * @param updatedAt 更新时间。
     * @return 受影响行数。
     */
    int touch(@Param("id") String id, @Param("updatedAt") Timestamp updatedAt);

}
