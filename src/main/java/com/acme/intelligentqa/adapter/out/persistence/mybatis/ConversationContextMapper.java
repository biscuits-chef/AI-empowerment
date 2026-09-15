package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 结构化会话上下文表的原生 MyBatis 映射器。 */
@Mapper
public interface ConversationContextMapper {

    /**
     * 新增结构化上下文并回填数据库自增主键。
     *
     * @param record 待新增的上下文记录。
     * @return 受影响行数。
     */
    int insert(ConversationContextPersistenceRecord record);

    /**
     * 按用户和会话读取结构化上下文。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话公开标识。
     * @return 匹配的结构化上下文，不存在时为空。
     */
    ConversationContextPersistenceRecord selectByOwnerAndConversation(
            @Param("ownerId") String ownerId,
            @Param("conversationId") String conversationId);

    /**
     * 按乐观版本条件更新结构化上下文。
     *
     * @param record 新的上下文记录内容。
     * @param expectedVersion 更新前期望版本号。
     * @return 受影响行数。
     */
    int updateByVersion(
            @Param("record") ConversationContextPersistenceRecord record,
            @Param("expectedVersion") long expectedVersion);
}
