package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 回答执行事件表的 MyBatis-Plus 映射器。
 */
@Mapper
public interface AnswerEventMapper extends BaseMapper<AnswerEventPersistenceRecord> {

    /**
     * 从指定序号之后读取回答事件。
     *
     * @param answerId 回答 ID。
     * @param afterSequence 最后确认的事件序号。
     * @param limit 最大返回数量。
     * @return 按事件序号升序排列的记录。
     */
    List<AnswerEventPersistenceRecord> selectAfter(
            @Param("answerId") String answerId,
            @Param("afterSequence") long afterSequence,
            @Param("limit") int limit);

    /**
     * 查询事件序号是否属于指定回答。
     *
     * @param answerId 回答 ID。
     * @param sequence 事件序号。
     * @return 匹配记录数量。
     */
    int countSequence(@Param("answerId") String answerId, @Param("sequence") long sequence);

    /**
     * 批量读取会话历史中的展示事件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param answerIds 当前历史窗口中的回答 ID。
     * @return 不包含文本增量与内部元数据的展示事件。
     */
    List<AnswerEventPersistenceRecord> selectDisplayEvents(
            @Param("ownerId") String ownerId,
            @Param("conversationId") String conversationId,
            @Param("answerIds") List<String> answerIds);
}
