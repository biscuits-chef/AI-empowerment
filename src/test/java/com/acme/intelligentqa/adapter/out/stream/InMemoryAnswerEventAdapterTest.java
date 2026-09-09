package com.acme.intelligentqa.adapter.out.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.intelligentqa.common.error.EventReplayGapException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 验证 InMemoryAnswerEventAdapter 的业务行为与边界。
 */
class InMemoryAnswerEventAdapterTest {

    /**
     * 验证请求事件过期时返回重放缺口。
     */
    @Test
    void rejectsReplayWhenRequestedEventsHaveExpired() {
        final InMemoryAnswerEventAdapter adapter = adapter(2);
        final UUID answerId = UUID.randomUUID();
        adapter.publish(answerId, "delta", "一");
        adapter.publish(answerId, "delta", "二");
        adapter.publish(answerId, "completed", "stop");

        assertThrows(EventReplayGapException.class,
                () -> adapter.subscribe(answerId, 0, event -> { }));
    }

    /**
     * 验证从最后确认序号的下一事件开始重放。
     */
    @Test
    void replaysFromTheNextAvailableSequence() {
        final InMemoryAnswerEventAdapter adapter = adapter(2);
        final UUID answerId = UUID.randomUUID();
        adapter.publish(answerId, "delta", "一");
        adapter.publish(answerId, "delta", "二");
        adapter.publish(answerId, "completed", "stop");
        final List<AnswerEvent> events = new ArrayList<>();

        adapter.subscribe(answerId, 1, events::add).close();

        assertEquals(2, events.size());
        assertEquals(2, events.get(0).sequence());
    }

    /**
     * 验证未知非零续传序号不会被静默接受。
     */
    @Test
    void rejectsUnknownNonzeroResumePosition() {
        final InMemoryAnswerEventAdapter adapter = adapter(2);

        assertThrows(EventReplayGapException.class,
                () -> adapter.subscribe(UUID.randomUUID(), 7, event -> { }));
    }

    /**
     * 处理被测适配器。
     *
     * @param bufferSize 事件缓存容量。
     *
     * @return 被测适配器。
     */
    private InMemoryAnswerEventAdapter adapter(final int bufferSize) {
        return new InMemoryAnswerEventAdapter(
                new QaProperties(false, 10, 10, 2, 100, 100, 10, 0.8D, bufferSize, 1, 1, 1));
    }
}
