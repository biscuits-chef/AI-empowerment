package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.in.AnswerCancellationUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 持久化停止任务、租约和重试状态的出站端口。
 */
public interface CancellationRepositoryPort {

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
     * @param firstDispatchAt 停止任务首次计划执行时间。
     *
     * @return 接口请求。
     */
    RequestResult request(
            String ownerId,
            UUID answerId,
            String idempotencyKey,
            AnswerCancellationUseCase.CancellationReason reason,
            Instant requestedAt,
            Instant firstDispatchAt);

    /**
     * 判断回答是否已收到停止请求。
     *
     * @param answerId 回答 ID。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean isCancellationRequested(UUID answerId);

    /**
     * 保存公司模型侧消息 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param providerMessageId 公司模型侧消息 ID。
     *
     * @param dispatchAt 本次任务分发时间。
     */
    void recordProviderMessageId(UUID answerId, String providerMessageId, Instant dispatchAt);

    /**
     * 查询本批可领取的停止任务。
     *
     * @param now 当前时间。
     *
     * @param limit 数量上限。
     *
     * @return 查询本批可领取的停止任务。
     */
    List<UUID> findDispatchable(Instant now, int limit);

    /**
     * 将最后一次领取后崩溃且租约已过期的任务原子收敛为失败。
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
    boolean markExpiredExhausted(
            UUID answerId,
            Instant now,
            int maximumAttempts,
            String errorCode);

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
    Optional<Task> claim(
            UUID answerId,
            String workerId,
            Instant now,
            Instant leaseUntil,
            int maximumAttempts);

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
    boolean markCancelled(UUID answerId, String workerId, Instant cancelledAt);

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
    void reschedule(UUID answerId, String workerId, String errorCode, Instant nextAttemptAt);

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
    boolean markFailed(UUID answerId, String workerId, String errorCode, Instant failedAt);

    /**
     * 停止请求持久化受理结果。
     */
    final class RequestResult {
        /**
         * 回答快照。
         */
        private final AnswerSnapshot answer;
        /**
         * 是否为首次受理的停止请求。
         */
        private final boolean newlyRequested;

        /**
         * 创建 {@code RequestResult} 实例。
         *
         * @param answer 回答快照。
         *
         * @param newlyRequested 是否为首次受理的停止请求。
         */
        public RequestResult(final AnswerSnapshot answer, final boolean newlyRequested) {
            this.answer = Objects.requireNonNull(answer, "answer must not be null");
            this.newlyRequested = newlyRequested;
        }

        /**
         * 处理回答快照。
         *
         * @return 回答快照。
         */
        public AnswerSnapshot answer() { return answer; }
        /**
         * 处理是否为首次受理的停止请求。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        public boolean newlyRequested() { return newlyRequested; }
    }

    /**
     * 包含租约与重试信息的持久化停止任务。
     */
    final class Task {
        /**
         * 回答 ID。
         */
        private final UUID answerId;
        /**
         * 用户所有者 ID。
         */
        private final String ownerId;
        /**
         * 公司模型侧消息 ID。
         */
        private final String providerMessageId;
        /**
         * 停止时所处阶段。
         */
        private final String cancelledStage;
        /**
         * 已执行的停止尝试次数。
         */
        private final int attemptCount;

        /**
         * 创建 {@code Task} 实例。
         *
         * @param answerId 回答 ID。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param providerMessageId 公司模型侧消息 ID。
         *
         * @param cancelledStage 停止时所处阶段。
         *
         * @param attemptCount 已执行的停止尝试次数。
         */
        public Task(
                final UUID answerId,
                final String ownerId,
                final String providerMessageId,
                final String cancelledStage,
                final int attemptCount) {
            this.answerId = Objects.requireNonNull(answerId, "answerId must not be null");
            this.ownerId = requireText(ownerId, "ownerId");
            this.providerMessageId = providerMessageId;
            this.cancelledStage = requireText(cancelledStage, "cancelledStage");
            this.attemptCount = attemptCount;
        }

        /**
         * 返回回答 ID。
         *
         * @return 回答 ID。
         */
        public UUID answerId() { return answerId; }
        /**
         * 返回用户所有者 ID。
         *
         * @return 用户所有者 ID。
         */
        public String ownerId() { return ownerId; }
        /**
         * 处理公司模型侧消息 ID。
         *
         * @return 公司模型侧消息 ID。
         */
        public String providerMessageId() { return providerMessageId; }
        /**
         * 返回停止时所处阶段。
         *
         * @return 停止时所处阶段。
         */
        public String cancelledStage() { return cancelledStage; }
        /**
         * 处理已执行的停止尝试次数。
         *
         * @return 已执行的停止尝试次数。
         */
        public int attemptCount() { return attemptCount; }

        /**
         * 校验文本非空并返回原值。
         *
         * @param value 输入值。
         *
         * @param field 字段名称。
         *
         * @return 校验文本非空并返回原值。
         */
        private static String requireText(final String value, final String field) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(field + " must not be blank");
            }
            return value;
        }
    }
}
