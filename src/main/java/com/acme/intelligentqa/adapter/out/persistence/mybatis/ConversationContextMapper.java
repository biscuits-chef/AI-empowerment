package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 结构化会话上下文表的 MyBatis-Plus 映射器。 */
@Mapper
public interface ConversationContextMapper extends BaseMapper<ConversationContextPersistenceRecord> {
}

