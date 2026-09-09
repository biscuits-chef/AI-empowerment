package com.acme.intelligentqa.adapter.out.stream;

import com.acme.intelligentqa.common.error.EventReplayGapException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.port.out.AnswerEventHistoryPort;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 将回答事件先写入 GoldenDB，再推送给本实例订阅者的事件适配器。
 */
@Primary
@Component
public class PersistentAnswerEventAdapter implements AnswerEventPort {

    /** 每次重放读取的事件数量上限。 */
    private final int replayLimit;
    /** 回答事件持久化端口。 */
    private final AnswerEventHistoryPort history;
    /** 系统时钟。 */
    private final Clock clock;
    /** 按回答隔离的本实例订阅通道。 */
    private final ConcurrentMap<UUID, EventChannel> channels = new ConcurrentHashMap<>();

    /**
     * 创建持久化回答事件适配器。
     *
     * @param properties 问答容量配置。
     * @param history 回答事件持久化端口。
     * @param clock 系统时钟。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public PersistentAnswerEventAdapter(
            final QaProperties properties,
            final AnswerEventHistoryPort history,
            final Clock clock) {
        /*
         * 最坏情况下模型可能逐字符返回 delta。持久化事件重放上限必须覆盖一份完整回答，
         * 否则长回答断线后会反复命中重放缺口，无法恢复执行过程和最终产物。
         */
        this.replayLimit = Math.max(
                properties.eventBufferSize(),
                properties.maxAnswerCharacters() + 64);
        this.history = history;
        this.clock = clock;
    }

    /**
     * 持久化并发布一个流式事件。
     *
     * @param answerId 回答 ID。
     * @param type 事件类型。
     * @param data 事件数据。
     */
    @Override
    public void publish(final UUID answerId, final String type, final String data) {
        channel(answerId).publish(type, data);
    }

    /**
     * 从指定序号订阅持久化事件和后续实时事件。
     *
     * @param answerId 回答 ID。
     * @param afterSequence 最后确认的事件序号。
     * @param consumer 事件订阅回调。
     * @return 可关闭的订阅句柄。
     */
    @Override
    public Subscription subscribe(
            final UUID answerId,
            final long afterSequence,
            final Consumer<AnswerEvent> consumer) {
        final EventChannel channel = channel(answerId);
        channel.subscribe(afterSequence, consumer);
        return () -> channel.unsubscribe(consumer);
    }

    /**
     * 返回指定回答的串行化事件通道。
     *
     * @param answerId 回答 ID。
     * @return 回答事件通道。
     */
    private EventChannel channel(final UUID answerId) {
        return channels.computeIfAbsent(answerId, EventChannel::new);
    }

    /**
     * 单个回答的持久化顺序和本实例订阅者集合。
     */
    private final class EventChannel {
        /** 回答 ID。 */
        private final UUID answerId;
        /** 当前本实例订阅者。 */
        private final List<Consumer<AnswerEvent>> consumers = new ArrayList<>();

        /**
         * 创建回答事件通道。
         *
         * @param answerId 回答 ID。
         */
        EventChannel(final UUID answerId) { this.answerId = answerId; }

        /**
         * 在同一回答内串行持久化和通知事件。
         *
         * @param type 事件类型。
         * @param data 事件数据。
         */
        synchronized void publish(final String type, final String data) {
            final AnswerEvent event = history.append(answerId, type, data, Instant.now(clock));
            for (final Consumer<AnswerEvent> consumer : new ArrayList<>(consumers)) {
                consumer.accept(event);
            }
        }

        /**
         * 先重放数据库事件，再注册实时订阅者，避免本实例内出现订阅竞态。
         *
         * @param afterSequence 最后确认的事件序号。
         * @param consumer 事件订阅回调。
         */
        synchronized void subscribe(final long afterSequence, final Consumer<AnswerEvent> consumer) {
            if (afterSequence > 0 && !history.contains(answerId, afterSequence)) {
                throw new EventReplayGapException();
            }
            final List<AnswerEvent> events = history.list(answerId, afterSequence, replayLimit + 1);
            if (events.size() > replayLimit) {
                throw new EventReplayGapException();
            }
            for (final AnswerEvent event : events) {
                consumer.accept(event);
            }
            consumers.add(consumer);
        }

        /**
         * 移除实时订阅者。
         *
         * @param consumer 事件订阅回调。
         */
        synchronized void unsubscribe(final Consumer<AnswerEvent> consumer) {
            consumers.remove(consumer);
        }
    }
}
