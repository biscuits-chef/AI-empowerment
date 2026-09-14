package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerEventMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerEventPersistenceRecord;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.port.out.AnswerEventHistoryPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

/**
 * 使用 GoldenDB 持久化回答执行事件和可恢复产物引用。
 */
@Repository
public class MybatisPlusAnswerEventRepository implements AnswerEventHistoryPort {

    /** 回答事件表映射器。 */
    private final AnswerEventMapper mapper;

    /**
     * 创建回答事件持久化仓储。
     *
     * @param mapper 回答事件表映射器。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected mapper is retained and not exposed")
    public MybatisPlusAnswerEventRepository(final AnswerEventMapper mapper) { this.mapper = mapper; }

    /**
     * 追加一条回答事件。
     *
     * @param answerId 回答 ID。
     * @param type 事件类型。
     * @param data 事件数据。
     * @param occurredAt 事件发生时间。
     * @return 已分配稳定序号的回答事件。
     */
    @Override
    public AnswerEvent append(
            final UUID answerId,
            final String type,
            final String data,
            final Instant occurredAt) {
        final AnswerEventPersistenceRecord record = new AnswerEventPersistenceRecord();
        record.setAnswerId(answerId.toString());
        record.setEventType(type);
        record.setEventData(data);
        record.setOccurredAt(Timestamp.from(occurredAt));
        final int affected = execute(() -> mapper.insert(record), "failed to append answer event");
        if (affected != 1 || record.getId() == null) {
            throw new PersistenceOperationException(
                    "failed to append answer event",
                    new IllegalStateException("event id was not generated"));
        }
        return toDomain(record);
    }

    /**
     * 从指定序号之后读取有界事件。
     *
     * @param answerId 回答 ID。
     * @param afterSequence 最后确认的事件序号。
     * @param limit 最大返回数量。
     * @return 按序号升序排列的回答事件。
     */
    @Override
    public List<AnswerEvent> list(final UUID answerId, final long afterSequence, final int limit) {
        return toDomain(execute(
                () -> mapper.selectAfter(answerId.toString(), afterSequence, limit),
                "failed to list answer events"));
    }

    /**
     * 校验事件序号是否属于指定回答。
     *
     * @param answerId 回答 ID。
     * @param sequence 事件序号。
     * @return 事件存在时返回 true。
     */
    @Override
    public boolean contains(final UUID answerId, final long sequence) {
        return execute(
                () -> mapper.countSequence(answerId.toString(), sequence),
                "failed to inspect answer event") == 1;
    }

    /**
     * 批量读取历史消息需要展示的执行事件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param answerIds 当前历史窗口内的回答 ID。
     * @return 以回答 ID 为键的执行事件。
     */
    @Override
    public Map<UUID, List<AnswerEvent>> listDisplayEvents(
            final String ownerId,
            final UUID conversationId,
            final List<UUID> answerIds) {
        if (answerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final List<String> ids = new ArrayList<>(answerIds.size());
        for (final UUID answerId : answerIds) {
            ids.add(answerId.toString());
        }
        final List<AnswerEventPersistenceRecord> records = execute(
                () -> mapper.selectDisplayEvents(ownerId, conversationId.toString(), ids),
                "failed to list display events");
        final Map<UUID, List<AnswerEvent>> grouped = new LinkedHashMap<>();
        for (final AnswerEventPersistenceRecord record : records) {
            grouped.computeIfAbsent(UUID.fromString(record.getAnswerId()), ignored -> new ArrayList<>())
                    .add(toDomain(record));
        }
        return grouped;
    }

    /**
     * 将持久化记录列表转换为领域事件。
     *
     * @param records 持久化记录列表。
     * @return 领域事件列表。
     */
    private static List<AnswerEvent> toDomain(final List<AnswerEventPersistenceRecord> records) {
        final List<AnswerEvent> events = new ArrayList<>(records.size());
        for (final AnswerEventPersistenceRecord record : records) {
            events.add(toDomain(record));
        }
        return events;
    }

    /**
     * 将持久化记录转换为领域事件。
     *
     * @param record 持久化记录。
     * @return 领域事件。
     */
    private static AnswerEvent toDomain(final AnswerEventPersistenceRecord record) {
        return new AnswerEvent(
                record.getId(), record.getEventType(), record.getEventData(), record.occurredAtInstant());
    }

    /**
     * 执行数据库操作并转换稳定异常。
     *
     * @param operation 数据库操作。
     * @param message 安全错误上下文。
     * @param <T> 数据库操作返回类型。
     * @return 数据库操作结果。
     */
    private <T> T execute(final Supplier<T> operation, final String message) {
        try {
            return operation.get();
        } catch (final DataAccessException exception) {
            throw new PersistenceOperationException(message, exception);
        }
    }
}
