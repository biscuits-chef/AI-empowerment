package com.acme.intelligentqa.adapter.out.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.intelligentqa.common.error.EventReplayGapException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.port.out.AnswerEventHistoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 验证持久化回答事件适配器的历史重放与实时通知语义。
 */
class PersistentAnswerEventAdapterTest {

    /** 固定测试时间。 */
    private static final Instant NOW = Instant.parse("2026-09-03T01:00:00Z");

    /**
     * 验证订阅先重放数据库事件，再接收后续持久化事件。
     */
    @Test
    void replaysPersistentHistoryBeforeDeliveringNewEvents() {
        final UUID answerId = UUID.randomUUID();
        final FakeHistory history = new FakeHistory();
        history.append(answerId, "retrieval_started", "STARTED", NOW);
        final PersistentAnswerEventAdapter adapter = adapter(history);
        final List<AnswerEvent> received = new ArrayList<>();

        adapter.subscribe(answerId, 0, received::add);
        adapter.publish(answerId, "completed", "stop");

        assertEquals(2, received.size());
        assertEquals("retrieval_started", received.get(0).type());
        assertEquals("completed", received.get(1).type());
    }

    /**
     * 验证未知续传序号会明确报告事件缺口。
     */
    @Test
    void rejectsUnknownResumeSequence() {
        final PersistentAnswerEventAdapter adapter = adapter(new FakeHistory());

        assertThrows(EventReplayGapException.class,
                () -> adapter.subscribe(UUID.randomUUID(), 9L, event -> { }));
    }

    /**
     * 创建使用固定时钟的被测适配器。
     *
     * @param history 测试事件历史。
     * @return 被测持久化事件适配器。
     */
    private PersistentAnswerEventAdapter adapter(final AnswerEventHistoryPort history) {
        final QaProperties properties = new QaProperties(
                false, 4000, 30000, 10, 1000, 1000, 10, 0.8D, 2, 1, 1, 1);
        return new PersistentAnswerEventAdapter(
                properties, history, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * 单元测试使用的有序内存事件历史。
     */
    private static final class FakeHistory implements AnswerEventHistoryPort {
        /** 按回答保存的事件集合。 */
        private final Map<UUID, List<AnswerEvent>> values = new LinkedHashMap<>();
        /** 下一个全局事件序号。 */
        private long nextSequence = 1L;

        /**
         * 追加并返回测试事件。
         *
         * @param answerId 回答 ID。
         * @param type 事件类型。
         * @param data 事件数据。
         * @param occurredAt 发生时间。
         * @return 已分配序号的测试事件。
         */
        @Override
        public AnswerEvent append(
                final UUID answerId,
                final String type,
                final String data,
                final Instant occurredAt) {
            final AnswerEvent event = new AnswerEvent(nextSequence++, type, data, occurredAt);
            values.computeIfAbsent(answerId, ignored -> new ArrayList<>()).add(event);
            return event;
        }

        /**
         * 返回指定序号之后的有界测试事件。
         *
         * @param answerId 回答 ID。
         * @param afterSequence 最后确认序号。
         * @param limit 数量上限。
         * @return 有序测试事件列表。
         */
        @Override
        public List<AnswerEvent> list(
                final UUID answerId,
                final long afterSequence,
                final int limit) {
            final List<AnswerEvent> result = new ArrayList<>();
            for (final AnswerEvent event : values.getOrDefault(
                    answerId, Collections.<AnswerEvent>emptyList())) {
                if (event.sequence() > afterSequence && result.size() < limit) {
                    result.add(event);
                }
            }
            return result;
        }

        /**
         * 判断序号是否属于回答。
         *
         * @param answerId 回答 ID。
         * @param sequence 事件序号。
         * @return 存在时返回 true。
         */
        @Override
        public boolean contains(final UUID answerId, final long sequence) {
            for (final AnswerEvent event : values.getOrDefault(
                    answerId, Collections.<AnswerEvent>emptyList())) {
                if (event.sequence() == sequence) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 测试适配器不使用批量展示事件查询。
         *
         * @param ownerId 用户所有者 ID。
         * @param conversationId 会话 ID。
         * @param answerIds 回答 ID 列表。
         * @return 空映射。
         */
        @Override
        public Map<UUID, List<AnswerEvent>> listDisplayEvents(
                final String ownerId,
                final UUID conversationId,
                final List<UUID> answerIds) {
            return Collections.emptyMap();
        }
    }
}
