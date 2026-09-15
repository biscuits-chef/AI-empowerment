package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 会话消息表的原生 MyBatis 映射器。 */
@Mapper
public interface ChatMessageMapper {

    /**
     * 新增消息并回填数据库自增主键。
     *
     * @param record 待新增的消息记录。
     * @return 受影响行数。
     */
    int insert(ChatMessagePersistenceRecord record);

    /**
     * 按公开标识更新消息正文。
     *
     * @param id 消息公开标识。
     * @param content 消息正文。
     * @return 受影响行数。
     */
    int updateContent(@Param("id") String id, @Param("content") String content);
}
