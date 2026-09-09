package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.AnswerEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 持久化回答执行事件并为历史恢复提供批量读取能力。
 */
public interface AnswerEventHistoryPort {

    /**
     * 追加一条回答事件。
     *
     * @param answerId 回答 ID。
     * @param type 事件类型。
     * @param data 事件数据。
     * @param occurredAt 事件发生时间。
     * @return 已分配稳定序号的回答事件。
     */
    AnswerEvent append(UUID answerId, String type, String data, Instant occurredAt);

    /**
     * 从指定序号之后读取有界事件。
     *
     * @param answerId 回答 ID。
     * @param afterSequence 最后确认的事件序号。
     * @param limit 最大返回数量。
     * @return 按序号升序排列的回答事件。
     */
    List<AnswerEvent> list(UUID answerId, long afterSequence, int limit);

    /**
     * 校验事件序号是否属于指定回答。
     *
     * @param answerId 回答 ID。
     * @param sequence 事件序号。
     * @return 事件存在时返回 true。
     */
    boolean contains(UUID answerId, long sequence);

    /**
     * 批量读取历史消息需要展示的执行事件，不返回回答文本增量。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param answerIds 当前历史窗口内的回答 ID。
     * @return 以回答 ID 为键、事件序号升序排列的执行事件。
     */
    Map<UUID, List<AnswerEvent>> listDisplayEvents(
            String ownerId,
            UUID conversationId,
            List<UUID> answerIds);
}
