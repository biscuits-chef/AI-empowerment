package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import java.sql.Timestamp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 回答反馈写入与更新的 MyBatis 映射器。 */
@Mapper
public interface AnswerFeedbackMapper {

    /**
     * 新增或更新持久化记录。
     *
     * @param answerId 回答 ID。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param feedbackType 反馈类型。
     *
     * @param now 当前时间。
     *
     * @return 新增或更新持久化记录。
     */
    int upsert(
            @Param("answerId") String answerId,
            @Param("ownerId") String ownerId,
            @Param("feedbackType") String feedbackType,
            @Param("now") Timestamp now);
}
