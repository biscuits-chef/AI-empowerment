package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerCancellationMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.CancellationTaskPersistenceRecord;
import com.acme.intelligentqa.common.error.AnswerAlreadyTerminalException;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.in.AnswerCancellationUseCase;
import com.acme.intelligentqa.domain.port.out.CancellationRepositoryPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于数据库条件更新实现停止任务租约和并发状态保护。
 */
@Repository
public class MybatisCancellationRepository implements CancellationRepositoryPort {

    /**
     * 回答表映射器。
     */
    private final AnswerMapper answerMapper;
    /**
     * 停止任务 SQL 映射器。
     */
    private final AnswerCancellationMapper cancellationMapper;

    /**
     * 创建 {@code MybatisCancellationRepository} 实例。
     *
     * @param answerMapper 回答表映射器。
     *
     * @param cancellationMapper 停止任务 SQL 映射器。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected mappers are retained and not exposed")
    public MybatisCancellationRepository(
            final AnswerMapper answerMapper,
            final AnswerCancellationMapper cancellationMapper) {
        this.answerMapper = answerMapper;
        this.cancellationMapper = cancellationMapper;
    }

    /**
     * 携带同源凭据并在有界超时内调用 JSON 接口。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param reason 原因。
     *
     * @param requestedAt 请求受理时间。
     *
     * @param messageIdDeadline 等待公司模型消息 ID 的截止时间。
     *
     * @return 接口请求。
     */
    @Override
    @Transactional
    public RequestResult request(
            final String ownerId,
            final UUID answerId,
            final String idempotencyKey,
            final AnswerCancellationUseCase.CancellationReason reason,
            final Instant requestedAt,
            final Instant messageIdDeadline) {
        final AnswerPersistenceRecord current = findRequired(ownerId, answerId);
        final AnswerSnapshot.Status currentStatus = AnswerSnapshot.Status.valueOf(current.getStatus());
        if (isCancellationStatus(currentStatus)) {
            return new RequestResult(AnswerRecordMapper.toDomain(current), false);
        }
        if (!isActive(currentStatus)) {
            throw new AnswerAlreadyTerminalException(currentStatus.name());
        }
        final int updated = execute(() -> cancellationMapper.request(
                answerId.toString(), ownerId, idempotencyKey, reason.name(),
                Timestamp.from(requestedAt), Timestamp.from(messageIdDeadline)),
                "failed to request answer cancellation");
        if (updated == 0) {
            final AnswerPersistenceRecord raced = findRequired(ownerId, answerId);
            final AnswerSnapshot.Status racedStatus = AnswerSnapshot.Status.valueOf(raced.getStatus());
            if (isCancellationStatus(racedStatus)) {
                return new RequestResult(AnswerRecordMapper.toDomain(raced), false);
            }
            throw new AnswerAlreadyTerminalException(racedStatus.name());
        }
        final AnswerPersistenceRecord requested = findRequired(ownerId, answerId);
        final boolean waitingForMessageId = AnswerSnapshot.Status.GENERATING.name()
                .equals(requested.getCancelledStage())
                && (current.getMessageId() == null || current.getMessageId().trim().isEmpty());
        final Instant firstDispatch = waitingForMessageId ? messageIdDeadline : requestedAt;
        requireOne(execute(() -> cancellationMapper.insertTask(
                        answerId.toString(), Timestamp.from(firstDispatch), Timestamp.from(requestedAt)),
                "failed to create answer cancellation task"), "failed to create answer cancellation task");
        return new RequestResult(AnswerRecordMapper.toDomain(requested), true);
    }

    /**
     * 判断回答是否已收到停止请求。
     *
     * @param answerId 回答 ID。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    public boolean isCancellationRequested(final UUID answerId) {
        return execute(() -> cancellationMapper.countCancellationRequested(answerId.toString()),
                "failed to read answer cancellation state") > 0;
    }

    /**
     * 保存公司模型侧消息 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param messageId 公司模型侧消息 ID。
     *
     * @param dispatchAt 本次任务分发时间。
     */
    @Override
    @Transactional
    public void recordMessageId(
            final UUID answerId,
            final String messageId,
            final Instant dispatchAt) {
        final int updated = execute(() -> cancellationMapper.recordMessageId(
                        answerId.toString(), messageId, Timestamp.from(dispatchAt)),
                "failed to record provider message id");
        if (updated > 0) {
            execute(() -> cancellationMapper.wakeTask(answerId.toString(), Timestamp.from(dispatchAt)),
                    "failed to wake answer cancellation task");
        }
    }

    /**
     * 查询本批可领取的停止任务。
     *
     * @param now 当前时间。
     *
     * @param limit 数量上限。
     *
     * @return 查询本批可领取的停止任务。
     */
    @Override
    public List<UUID> findDispatchable(final Instant now, final int limit) {
        final List<String> values = execute(
                () -> cancellationMapper.selectDispatchable(Timestamp.from(now), limit),
                "failed to find dispatchable cancellation tasks");
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        final List<UUID> result = new ArrayList<>(values.size());
        for (final String value : values) {
            result.add(UUID.fromString(value));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 将最后一次领取后崩溃且租约已过期的回答与任务共同收敛为失败。
     *
     * @param answerId 回答 ID。
     *
     * @param now 当前时间。
     *
     * @param maximumAttempts 停止接口最大尝试次数。
     *
     * @param errorCode 稳定错误码。
     *
     * @return 本次调用完成失败收敛时返回 true，否则返回 false。
     */
    @Override
    @Transactional
    public boolean markExpiredExhausted(
            final UUID answerId,
            final Instant now,
            final int maximumAttempts,
            final String errorCode) {
        final Timestamp timestamp = Timestamp.from(now);
        final int answerUpdated = execute(() -> cancellationMapper.markAnswerExpiredExhausted(
                        answerId.toString(), timestamp, maximumAttempts, errorCode),
                "failed to converge exhausted answer cancellation");
        final int taskUpdated = execute(() -> cancellationMapper.failTaskExpiredExhausted(
                        answerId.toString(), timestamp, maximumAttempts, errorCode),
                "failed to fail exhausted answer cancellation task");
        // 两个状态在同一事务中条件更新；只有回答和任务都由本次调用收敛时才发布一次终态事件。
        return answerUpdated == 1 && taskUpdated == 1;
    }

    /**
     * 按租约条件领取待执行的停止任务。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param now 当前时间。
     *
     * @param leaseUntil 租约到期时间。
     *
     * @param maximumAttempts 停止接口最大尝试次数。
     *
     * @return 按租约条件领取待执行的停止任务。
     */
    @Override
    @Transactional
    public Optional<Task> claim(
            final UUID answerId,
            final String workerId,
            final Instant now,
            final Instant leaseUntil,
            final int maximumAttempts) {
        final int claimed = execute(() -> cancellationMapper.claim(
                        answerId.toString(), workerId, Timestamp.from(now), Timestamp.from(leaseUntil),
                        maximumAttempts),
                "failed to claim answer cancellation task");
        if (claimed == 0) {
            return Optional.empty();
        }
        final CancellationTaskPersistenceRecord record = execute(
                () -> cancellationMapper.selectClaimed(answerId.toString(), workerId),
                "failed to read claimed answer cancellation task");
        if (record == null) {
            throw new PersistenceOperationException(
                    "claimed answer cancellation task was not readable",
                    new IllegalStateException("claimed task disappeared"));
        }
        return Optional.of(new Task(
                UUID.fromString(record.getAnswerId()), record.getOwnerId(), record.getMessageId(),
                record.getCancelledStage(), record.getAttemptCount()));
    }

    /**
     * 在并发条件允许时将回答标记为已停止。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param cancelledAt 停止完成时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    @Transactional
    public boolean markCancelled(final UUID answerId, final String workerId, final Instant cancelledAt) {
        final int updated = execute(() -> cancellationMapper.markCancelled(
                        answerId.toString(), workerId, Timestamp.from(cancelledAt)),
                "failed to mark answer cancelled");
        if (updated == 0) {
            return false;
        }
        requireOne(execute(() -> cancellationMapper.completeTask(
                        answerId.toString(), workerId, Timestamp.from(cancelledAt)),
                "failed to complete answer cancellation task"), "failed to complete answer cancellation task");
        return true;
    }

    /**
     * 为暂时失败的停止任务安排下一次执行。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param errorCode 错误码。
     *
     * @param nextAttemptAt 下一次停止任务计划执行时间。
     */
    @Override
    @Transactional
    public void reschedule(
            final UUID answerId,
            final String workerId,
            final String errorCode,
            final Instant nextAttemptAt) {
        requireOne(execute(() -> cancellationMapper.reschedule(
                        answerId.toString(), workerId, errorCode, Timestamp.from(nextAttemptAt)),
                "failed to reschedule answer cancellation task"),
                "failed to reschedule answer cancellation task");
        execute(() -> cancellationMapper.recordAnswerCancellationError(answerId.toString(), errorCode),
                "failed to record answer cancellation error");
    }

    /**
     * 将生成异常转换为失败或未完整终态。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param errorCode 错误码。
     *
     * @param failedAt 失败记录时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    @Transactional
    public boolean markFailed(
            final UUID answerId,
            final String workerId,
            final String errorCode,
            final Instant failedAt) {
        final int updated = execute(() -> cancellationMapper.markFailed(
                        answerId.toString(), workerId, errorCode, Timestamp.from(failedAt)),
                "failed to mark answer cancellation failed");
        if (updated == 0) {
            return false;
        }
        requireOne(execute(() -> cancellationMapper.failTask(
                        answerId.toString(), workerId, errorCode, Timestamp.from(failedAt)),
                "failed to fail answer cancellation task"), "failed to fail answer cancellation task");
        return true;
    }

    /**
     * 查询必需的回答，不存在时抛出资源异常。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @return 查询必需的回答，不存在时抛出资源异常。
     */
    private AnswerPersistenceRecord findRequired(final String ownerId, final UUID answerId) {
        final AnswerPersistenceRecord result = execute(
                () -> answerMapper.selectByOwnerAndId(ownerId, answerId.toString()),
                "failed to read answer");
        if (result == null) {
            throw new ResourceNotFoundException("answer not found: " + answerId);
        }
        return result;
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
     * 判断回答是否仍处于可继续生成状态。
     *
     * @param status 业务状态。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private static boolean isActive(final AnswerSnapshot.Status status) {
        return status == AnswerSnapshot.Status.PENDING
                || status == AnswerSnapshot.Status.RETRIEVING
                || status == AnswerSnapshot.Status.QUERYING
                || status == AnswerSnapshot.Status.GENERATING;
    }

    /**
     * 判断状态是否属于停止处理阶段。
     *
     * @param status 业务状态。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private static boolean isCancellationStatus(final AnswerSnapshot.Status status) {
        return status == AnswerSnapshot.Status.CANCEL_REQUESTED
                || status == AnswerSnapshot.Status.CANCELLED
                || status == AnswerSnapshot.Status.CANCEL_FAILED;
    }

}
