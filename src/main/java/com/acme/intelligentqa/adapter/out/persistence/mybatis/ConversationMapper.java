package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.sql.Timestamp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 会话表的 MyBatis-Plus 映射器。 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationPersistenceRecord> {

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

}
