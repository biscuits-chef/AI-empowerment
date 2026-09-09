package com.acme.intelligentqa.adapter.out.stream;

import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.common.error.EventReplayGapException;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * 仅供单实例本地开发使用的有界内存事件发布与重放适配器。
 */
public class InMemoryAnswerEventAdapter implements AnswerEventPort {

    /**
     * 按回答 ID 隔离的事件通道集合。
     */
    private final ConcurrentMap<UUID, EventChannel> channels = new ConcurrentHashMap<>();
    /**
     * 事件缓存容量。
     */
    private final int bufferSize;

    /**
     * 创建 {@code InMemoryAnswerEventAdapter} 实例。
     *
     * @param properties 配置参数。
     */
    public InMemoryAnswerEventAdapter(final QaProperties properties) {
        this.bufferSize = properties.eventBufferSize();
    }

    /**
     * 按回答序号发布一个流式事件。
     *
     * @param answerId 回答 ID。
     *
     * @param type 类型。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     */
    @Override
    public void publish(final UUID answerId, final String type, final String data) {
        channels.computeIfAbsent(answerId, ignored -> new EventChannel(bufferSize)).publish(type, data);
    }

    /**
     * 从指定序号订阅回答事件。
     *
     * @param answerId 回答 ID。
     *
     * @param afterSequence 最后确认的事件序号。
     *
     * @param consumer 事件订阅回调。
     *
     * @return 从指定序号订阅回答事件。
     */
    @Override
    public Subscription subscribe(
            final UUID answerId,
            final long afterSequence,
            final Consumer<AnswerEvent> consumer) {
        final EventChannel channel = channels.computeIfAbsent(answerId, ignored -> new EventChannel(bufferSize));
        channel.subscribe(afterSequence, consumer);
        return () -> channel.unsubscribe(consumer);
    }

    /**
     * 单个回答的有序内存事件缓存与订阅通道。
     */
    private static final class EventChannel {
        /**
         * 集合最大允许条目数。
         */
        private final int maximumSize;
        /**
         * 回答事件列表。
         */
        private final Deque<AnswerEvent> events = new ArrayDeque<>();
        /**
         * 当前事件订阅者集合。
         */
        private final List<Consumer<AnswerEvent>> consumers = new ArrayList<>();
        /**
         * 事件序号。
         */
        private long sequence;

        /**
         * 创建 {@code EventChannel} 实例。
         *
         * @param maximumSize 集合最大允许条目数。
         */
        EventChannel(final int maximumSize) {
            this.maximumSize = maximumSize;
        }

        /**
         * 按回答序号发布一个流式事件。
         *
         * @param type 类型。
         *
         * @param data 一个 SSE 事件的多行数据内容。
         */
        synchronized void publish(final String type, final String data) {
            final AnswerEvent event = new AnswerEvent(++sequence, type, data, Instant.now());
            events.addLast(event);
            while (events.size() > maximumSize) {
                events.removeFirst();
            }
            for (final Consumer<AnswerEvent> consumer : new ArrayList<>(consumers)) {
                consumer.accept(event);
            }
        }

        /**
         * 从指定序号订阅回答事件。
         *
         * @param afterSequence 最后确认的事件序号。
         *
         * @param consumer 事件订阅回调。
         */
        synchronized void subscribe(final long afterSequence, final Consumer<AnswerEvent> consumer) {
            final AnswerEvent first = events.peekFirst();
            if ((first == null && afterSequence > 0)
                    || (first != null && afterSequence < first.sequence() - 1)) {
                throw new EventReplayGapException();
            }
            for (final AnswerEvent event : events) {
                if (event.sequence() > afterSequence) {
                    consumer.accept(event);
                }
            }
            consumers.add(consumer);
        }

        /**
         * 移除事件监听器并释放订阅资源。
         *
         * @param consumer 事件订阅回调。
         */
        synchronized void unsubscribe(final Consumer<AnswerEvent> consumer) {
            consumers.remove(consumer);
        }
    }
}
