package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerPersistenceRecord;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import java.util.UUID;

/**
 * 在回答持久化记录与领域快照之间执行无副作用转换。
 */
final class AnswerRecordMapper {

    /**
     * 创建 {@code AnswerRecordMapper} 实例。
     */
    private AnswerRecordMapper() {
    }

    /**
     * 将数据库记录转换为领域对象。
     *
     * @param record 持久化记录。
     *
     * @return 将数据库记录转换为领域对象。
     */
    static AnswerSnapshot toDomain(final AnswerPersistenceRecord record) {
        final String regeneratedFrom = record.getRegeneratedFromAnswerId();
        return new AnswerSnapshot(
                UUID.fromString(record.getId()),
                UUID.fromString(record.getConversationId()),
                UUID.fromString(record.getQuestionId()),
                UUID.fromString(record.getTraceId()),
                regeneratedFrom == null ? null : UUID.fromString(regeneratedFrom),
                AnswerSnapshot.Status.valueOf(record.getStatus()),
                record.getContent(),
                record.getErrorCode(),
                record.createdAtInstant(),
                record.completedAtInstant(),
                record.getCancelReason(),
                record.getCancelledStage(),
                record.getCancelErrorCode(),
                record.cancelRequestedAtInstant(),
                record.cancelledAtInstant());
    }
}
