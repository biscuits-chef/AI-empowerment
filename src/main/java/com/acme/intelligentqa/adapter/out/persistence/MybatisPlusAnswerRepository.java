package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerFeedbackMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ChatMessageMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ChatMessagePersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationPersistenceRecord;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.in.QuestionAnswerUseCase;
import com.acme.intelligentqa.domain.port.out.AnswerRepositoryPort;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 MyBatis-Plus 的回答、消息、反馈和状态持久化适配器。
 */
@Repository
public class MybatisPlusAnswerRepository implements AnswerRepositoryPort {

    /**
     * 回答表映射器。
     */
    private final AnswerMapper answerMapper;
    /**
     * 会话消息表映射器。
     */
    private final ChatMessageMapper messageMapper;
    /**
     * 会话表映射器。
     */
    private final ConversationMapper conversationMapper;
    /**
     * 回答反馈 SQL 映射器。
     */
    private final AnswerFeedbackMapper feedbackMapper;

    /**
     * 创建 {@code MybatisPlusAnswerRepository} 实例。
     *
     * @param answerMapper 回答表映射器。
     *
     * @param messageMapper 会话消息表映射器。
     *
     * @param conversationMapper 会话表映射器。
     *
     * @param feedbackMapper 回答反馈 SQL 映射器。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected mappers are retained and not exposed")
    public MybatisPlusAnswerRepository(
            final AnswerMapper answerMapper,
            final ChatMessageMapper messageMapper,
            final ConversationMapper conversationMapper,
            final AnswerFeedbackMapper feedbackMapper) {
        this.answerMapper = answerMapper;
        this.messageMapper = messageMapper;
        this.conversationMapper = conversationMapper;
        this.feedbackMapper = feedbackMapper;
    }

    /**
     * 按用户和幂等键查询已受理结果。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 按用户和幂等键查询已受理结果。
     */
    @Override
    public Optional<AnswerSnapshot> findByIdempotencyKey(final String ownerId, final String idempotencyKey) {
        return Optional.ofNullable(execute(
                        () -> answerMapper.selectByIdempotencyKey(ownerId, idempotencyKey),
                        "failed to read idempotent answer"))
                .map(AnswerRecordMapper::toDomain);
    }

    /**
     * 创建并持久化业务对象。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param questionId 问题 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param traceId 链路追踪 ID。
     *
     * @param regeneratedFromAnswerId 原回答 ID。
     *
     * @param question 用户问题。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param now 当前时间。
     *
     * @return 创建并持久化业务对象。
     */
    @Override
    @Transactional
    public AnswerSnapshot create(
            final String ownerId,
            final UUID conversationId,
            final UUID questionId,
            final UUID answerId,
            final UUID traceId,
            final UUID regeneratedFromAnswerId,
            final String question,
            final String idempotencyKey,
            final Instant now) {
        final AnswerPersistenceRecord answer = answerRecord(
                ownerId, conversationId, questionId, answerId, traceId,
                regeneratedFromAnswerId, idempotencyKey, now);
        try {
            requireOne(answerMapper.insert(answer), "failed to insert answer");
            requireOne(messageMapper.insert(messageRecord(
                            questionId, conversationId, null, "USER", question, now)),
                    "failed to insert question message");
            requireOne(messageMapper.insert(messageRecord(
                            answerId, conversationId, answerId, "ASSISTANT", "", now.plusNanos(1000L))),
                    "failed to insert assistant message");
            touchConversation(conversationId, now);
        } catch (final DuplicateKeyException exception) {
            return findByIdempotencyKey(ownerId, idempotencyKey)
                    .orElseThrow(() -> new PersistenceOperationException(
                            "duplicate answer could not be read", exception));
        } catch (final DataAccessException exception) {
            throw new PersistenceOperationException("failed to create answer", exception);
        }
        return AnswerRecordMapper.toDomain(answer);
    }

    /**
     * 查询满足用户隔离条件的目标记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @return 查询满足用户隔离条件的目标记录。
     */
    @Override
    public Optional<AnswerSnapshot> find(final String ownerId, final UUID answerId) {
        return Optional.ofNullable(execute(
                        () -> answerMapper.selectByOwnerAndId(ownerId, answerId.toString()),
                        "failed to read answer"))
                .map(AnswerRecordMapper::toDomain);
    }

    /**
     * 读取回答对应的原始问题。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @return 读取回答对应的原始问题。
     */
    @Override
    public Optional<String> findQuestion(final String ownerId, final UUID answerId) {
        return Optional.ofNullable(execute(
                () -> answerMapper.selectQuestion(ownerId, answerId.toString()),
                "failed to read answer question"));
    }

    /**
     * 按允许的源状态执行回答状态条件更新。
     *
     * @param answerId 回答 ID。
     *
     * @param status 业务状态。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    public boolean transitionStatus(final UUID answerId, final AnswerSnapshot.Status status) {
        final LambdaUpdateWrapper<AnswerPersistenceRecord> update =
                new LambdaUpdateWrapper<AnswerPersistenceRecord>()
                        .eq(AnswerPersistenceRecord::getPublicId, answerId.toString())
                        .in(AnswerPersistenceRecord::getStatus, activeStatuses())
                        .set(AnswerPersistenceRecord::getStatus, status.name());
        return execute(() -> answerMapper.update(null, update), "failed to update answer status") == 1;
    }

    /**
     * 保存公司 HiAgent 创建会话接口返回的应用会话 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param appConversationId 公司 HiAgent 应用会话 ID。
     *
     * @return 条件成立且写入成功时返回 true，否则返回 false。
     */
    @Override
    public boolean recordAppConversationId(final UUID answerId, final String appConversationId) {
        if (appConversationId == null || appConversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("appConversationId must not be blank");
        }
        final LambdaUpdateWrapper<AnswerPersistenceRecord> update =
                new LambdaUpdateWrapper<AnswerPersistenceRecord>()
                        .eq(AnswerPersistenceRecord::getPublicId, answerId.toString())
                        .in(AnswerPersistenceRecord::getStatus, java.util.Arrays.asList(
                                AnswerSnapshot.Status.GENERATING.name(),
                                AnswerSnapshot.Status.CANCEL_REQUESTED.name()))
                        .and(wrapper -> wrapper
                                .isNull(AnswerPersistenceRecord::getAppConversationId)
                                .or()
                                .eq(AnswerPersistenceRecord::getAppConversationId, appConversationId))
                        .set(AnswerPersistenceRecord::getAppConversationId, appConversationId);
        return execute(
                () -> answerMapper.update(null, update),
                "failed to record HiAgent app conversation id") == 1;
    }

    /**
     * 保存已生成的部分回答文本。
     *
     * @param answerId 回答 ID。
     *
     * @param content 内容。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    @Transactional
    public boolean savePartial(final UUID answerId, final String content) {
        final LambdaUpdateWrapper<AnswerPersistenceRecord> answerUpdate =
                new LambdaUpdateWrapper<AnswerPersistenceRecord>()
                        .eq(AnswerPersistenceRecord::getPublicId, answerId.toString())
                        .eq(AnswerPersistenceRecord::getStatus, AnswerSnapshot.Status.GENERATING.name())
                        .set(AnswerPersistenceRecord::getContent, content);
        final int updated = execute(
                () -> answerMapper.update(null, answerUpdate), "failed to save partial answer");
        if (updated == 0) {
            return false;
        }
        final LambdaUpdateWrapper<ChatMessagePersistenceRecord> messageUpdate =
                new LambdaUpdateWrapper<ChatMessagePersistenceRecord>()
                        .eq(ChatMessagePersistenceRecord::getPublicId, answerId.toString())
                        .set(ChatMessagePersistenceRecord::getContent, content);
        requireOne(execute(
                        () -> messageMapper.update(null, messageUpdate),
                        "failed to save partial answer message"),
                "failed to save partial answer message");
        return true;
    }

    /**
     * 保存最终内容并将回答转换为完成状态。
     *
     * @param answerId 回答 ID。
     *
     * @param content 内容。
     *
     * @param completedAt 完成时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    @Transactional
    public boolean complete(final UUID answerId, final String content, final Instant completedAt) {
        return finalizeAnswer(answerId, AnswerSnapshot.Status.COMPLETED, content, null, completedAt);
    }

    /**
     * 保存确定性追问并将回答转换为待澄清状态。
     *
     * @param answerId 回答 ID。
     *
     * @param content 内容。
     *
     * @param completedAt 完成时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    @Transactional
    public boolean clarify(final UUID answerId, final String content, final Instant completedAt) {
        return finalizeAnswer(answerId, AnswerSnapshot.Status.NEEDS_CLARIFICATION, content, null, completedAt);
    }

    /**
     * 记录失败或未完整状态，并保留已生成内容。
     *
     * @param answerId 回答 ID。
     *
     * @param status 业务状态。
     *
     * @param partialContent 已生成的部分内容。
     *
     * @param errorCode 错误码。
     *
     * @param completedAt 完成时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    @Transactional
    public boolean fail(
            final UUID answerId,
            final AnswerSnapshot.Status status,
            final String partialContent,
            final String errorCode,
            final Instant completedAt) {
        return finalizeAnswer(answerId, status, partialContent, errorCode, completedAt);
    }

    /**
     * 新增或覆盖用户对指定回答的反馈。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param feedback 用户反馈。
     *
     * @param now 当前时间。
     */
    @Override
    public void upsertFeedback(
            final String ownerId,
            final UUID answerId,
            final QuestionAnswerUseCase.Feedback feedback,
            final Instant now) {
        requireUpsert(execute(
                () -> feedbackMapper.upsert(
                        answerId.toString(), ownerId, feedback.name(), Timestamp.from(now)),
                "failed to save answer feedback"));
    }

    /**
     * 根据并发状态保存回答最终内容。
     *
     * @param answerId 回答 ID。
     *
     * @param status 业务状态。
     *
     * @param content 内容。
     *
     * @param errorCode 错误码。
     *
     * @param completedAt 完成时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean finalizeAnswer(
            final UUID answerId,
            final AnswerSnapshot.Status status,
            final String content,
            final String errorCode,
            final Instant completedAt) {
        final LambdaUpdateWrapper<AnswerPersistenceRecord> answerUpdate =
                new LambdaUpdateWrapper<AnswerPersistenceRecord>()
                        .eq(AnswerPersistenceRecord::getPublicId, answerId.toString())
                        .in(AnswerPersistenceRecord::getStatus, activeStatuses())
                        .set(AnswerPersistenceRecord::getStatus, status.name())
                        .set(AnswerPersistenceRecord::getContent, content)
                        .set(AnswerPersistenceRecord::getErrorCode, errorCode)
                        .set(AnswerPersistenceRecord::getCompletedAt, Timestamp.from(completedAt));
        final LambdaUpdateWrapper<ChatMessagePersistenceRecord> messageUpdate =
                new LambdaUpdateWrapper<ChatMessagePersistenceRecord>()
                        .eq(ChatMessagePersistenceRecord::getPublicId, answerId.toString())
                        .set(ChatMessagePersistenceRecord::getContent, content);
        final int updated = execute(() -> answerMapper.update(null, answerUpdate), "failed to finalize answer");
        if (updated == 0) {
            return false;
        }
        requireOne(execute(
                        () -> messageMapper.update(null, messageUpdate),
                        "failed to finalize answer message"),
                "failed to finalize answer message");
        return true;
    }

    /**
     * 更新会话的最后活动时间。
     *
     * @param conversationId 会话 ID。
     *
     * @param now 当前时间。
     */
    private void touchConversation(final UUID conversationId, final Instant now) {
        final LambdaUpdateWrapper<ConversationPersistenceRecord> update =
                new LambdaUpdateWrapper<ConversationPersistenceRecord>()
                        .eq(ConversationPersistenceRecord::getPublicId, conversationId.toString())
                        .isNull(ConversationPersistenceRecord::getDeletedAt)
                        .set(ConversationPersistenceRecord::getUpdatedAt, Timestamp.from(now));
        requireOne(conversationMapper.update(null, update), "failed to update conversation timestamp");
    }

    /**
     * 处理回答持久化记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param questionId 问题 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param traceId 链路追踪 ID。
     *
     * @param regeneratedFromAnswerId 原回答 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param now 当前时间。
     *
     * @return 回答持久化记录。
     */
    private AnswerPersistenceRecord answerRecord(
            final String ownerId,
            final UUID conversationId,
            final UUID questionId,
            final UUID answerId,
            final UUID traceId,
            final UUID regeneratedFromAnswerId,
            final String idempotencyKey,
            final Instant now) {
        final AnswerPersistenceRecord record = new AnswerPersistenceRecord();
        record.setPublicId(answerId.toString());
        record.setConversationId(conversationId.toString());
        record.setQuestionId(questionId.toString());
        record.setOwnerId(ownerId);
        record.setIdempotencyKey(idempotencyKey);
        record.setTraceId(traceId.toString());
        record.setRegeneratedFromAnswerId(
                regeneratedFromAnswerId == null ? null : regeneratedFromAnswerId.toString());
        record.setStatus(AnswerSnapshot.Status.PENDING.name());
        record.setContent("");
        record.setCreatedAt(Timestamp.from(now));
        return record;
    }

    /**
     * 处理会话消息持久化记录。
     *
     * @param id 唯一标识。
     *
     * @param conversationId 会话 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param role 消息角色。
     *
     * @param content 内容。
     *
     * @param now 当前时间。
     *
     * @return 会话消息持久化记录。
     */
    private ChatMessagePersistenceRecord messageRecord(
            final UUID id,
            final UUID conversationId,
            final UUID answerId,
            final String role,
            final String content,
            final Instant now) {
        final ChatMessagePersistenceRecord record = new ChatMessagePersistenceRecord();
        record.setPublicId(id.toString());
        record.setConversationId(conversationId.toString());
        record.setAnswerId(answerId == null ? null : answerId.toString());
        record.setRole(role);
        record.setContent(content);
        record.setCreatedAt(Timestamp.from(now));
        return record;
    }

    /**
     * 执行停止任务的远程调用与状态收敛。
     *
     * @param operation 当前数据库操作名称。
     *
     * @param message 提示信息。
     *
     * @return 执行停止任务的远程调用与状态收敛。
     */
    private <T> T execute(final Supplier<T> operation, final String message) {
        try {
            return operation.get();
        } catch (final DataAccessException exception) {
            throw new PersistenceOperationException(message, exception);
        }
    }

    /**
     * 校验数据库操作恰好影响一行。
     *
     * @param affectedRows SQL 实际影响行数。
     *
     * @param message 提示信息。
     */
    private void requireOne(final int affectedRows, final String message) {
        if (affectedRows != 1) {
            throw new PersistenceOperationException(
                    message, new IllegalStateException("expected one affected row but was " + affectedRows));
        }
    }

    /**
     * 校验数据库新增或更新影响行数符合预期。
     *
     * @param affectedRows SQL 实际影响行数。
     */
    private void requireUpsert(final int affectedRows) {
        if (affectedRows < 1 || affectedRows > 2) {
            throw new PersistenceOperationException(
                    "failed to save answer feedback",
                    new IllegalStateException("unexpected upsert row count " + affectedRows));
        }
    }

    /**
     * 处理允许继续生成的非终态集合。
     *
     * @return 允许继续生成的非终态集合。
     */
    private static java.util.List<String> activeStatuses() {
        return java.util.Arrays.asList(
                AnswerSnapshot.Status.PENDING.name(),
                AnswerSnapshot.Status.RETRIEVING.name(),
                AnswerSnapshot.Status.QUERYING.name(),
                AnswerSnapshot.Status.GENERATING.name());
    }
}
