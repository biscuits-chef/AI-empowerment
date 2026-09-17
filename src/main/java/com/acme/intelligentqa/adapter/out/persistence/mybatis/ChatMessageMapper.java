package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 会话消息表的 MyBatis-Plus 映射器。 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessagePersistenceRecord> {
}

