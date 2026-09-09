package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.CancellationProperties;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import com.acme.intelligentqa.domain.port.out.CancellationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.LanguageModelCancellationPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 使用租约调度停止任务，负责调用模型停止接口、重试和最终状态收敛。
 */
@Service
public class CancellationDispatcher {

    /**
     * 模型消息 ID 尚不可用的错误码。
     */
    private static final String MESSAGE_ID_UNAVAILABLE = "MODEL_MESSAGE_ID_UNAVAILABLE";
    /**
     * 最后一次租约内未完成而耗尽尝试次数的错误码。
     */
    private static final String ATTEMPTS_EXHAUSTED = "CANCELLATION_ATTEMPTS_EXHAUSTED";
    /**
     * 停止任务仓储。
     */
    private final CancellationRepositoryPort cancellationRepository;
    /**
     * 公司模型停止端口。
     */
    private final LanguageModelCancellationPort modelCancellation;
    /**
     * 回答事件端口。
     */
    private final AnswerEventPort eventPort;
    /**
     * 配置参数。
     */
    private final CancellationProperties properties;
    /**
     * 系统时钟。
     */
    private final Clock clock;
    /**
     * 当前停止任务执行器标识。
     */
    private final String workerId = UUID.randomUUID().toString();

    /**
     * 创建 {@code CancellationDispatcher} 实例。
     *
     * @param cancellationRepository 停止任务仓储。
     *
     * @param modelCancellation 公司模型停止端口。
     *
     * @param eventPort 回答事件端口。
     *
     * @param properties 配置参数。
     *
     * @param clock 系统时钟。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public CancellationDispatcher(
            final CancellationRepositoryPort cancellationRepository,
            final LanguageModelCancellationPort modelCancellation,
            final AnswerEventPort eventPort,
            final CancellationProperties properties,
            final Clock clock) {
        this.cancellationRepository = cancellationRepository;
        this.modelCancellation = modelCancellation;
        this.eventPort = eventPort;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 扫描并领取到期的持久化停止任务。
     */
    @Scheduled(fixedDelayString = "${app.qa.cancellation.scan-delay-millis:1000}")
    public void scan() {
        final Instant now = Instant.now(clock);
        for (final UUID answerId : cancellationRepository.findDispatchable(now, properties.batchSize())) {
            if (cancellationRepository.markExpiredExhausted(
                    answerId, now, properties.maximumAttempts(), ATTEMPTS_EXHAUSTED)) {
                eventPort.publish(answerId, "cancellation_failed", ATTEMPTS_EXHAUSTED);
            } else {
                dispatch(answerId);
            }
        }
    }

    /**
     * 分发一个待执行的停止任务。
     *
     * @param answerId 回答 ID。
     */
    public void dispatch(final UUID answerId) {
        final Instant now = Instant.now(clock);
        final Optional<CancellationRepositoryPort.Task> claimed = cancellationRepository.claim(
                answerId, workerId, now, now.plusMillis(properties.leaseMillis()), properties.maximumAttempts());
        if (!claimed.isPresent()) {
            return;
        }
        process(claimed.get());
    }

    /**
     * 解析并分发一个完整公司模型事件。
     *
     * @param task 待执行的停止任务。
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void process(final CancellationRepositoryPort.Task task) {
        if (!AnswerSnapshot.Status.GENERATING.name().equals(task.cancelledStage())) {
            complete(task.answerId());
            return;
        }
        if (task.providerMessageId() == null || task.providerMessageId().trim().isEmpty()) {
            fail(task, MESSAGE_ID_UNAVAILABLE);
            return;
        }
        try {
            modelCancellation.stop(task.ownerId(), task.providerMessageId());
            complete(task.answerId());
        } catch (final DependencyUnavailableException exception) {
            retryOrFail(task, exception.errorCode());
        } catch (final RuntimeException exception) {
            retryOrFail(task, "MODEL_STOP_FAILED");
        }
    }

    /**
     * 根据尝试次数安排停止重试或记录最终失败。
     *
     * @param task 待执行的停止任务。
     *
     * @param errorCode 错误码。
     */
    private void retryOrFail(final CancellationRepositoryPort.Task task, final String errorCode) {
        if (task.attemptCount() >= properties.maximumAttempts()) {
            fail(task, errorCode);
        } else {
            cancellationRepository.reschedule(
                    task.answerId(), workerId, errorCode,
                    Instant.now(clock).plusMillis(properties.retryDelayMillis()));
        }
    }

    /**
     * 按租约归属将回答标记为已停止，并发布停止终态事件。
     *
     * @param answerId 回答 ID。
     */
    private void complete(final UUID answerId) {
        if (cancellationRepository.markCancelled(answerId, workerId, Instant.now(clock))) {
            eventPort.publish(answerId, "cancelled", "USER_REQUESTED");
        }
    }

    /**
     * 按租约归属记录停止任务最终失败，并发布停止失败事件。
     *
     * @param task 待执行的停止任务。
     *
     * @param errorCode 错误码。
     */
    private void fail(final CancellationRepositoryPort.Task task, final String errorCode) {
        if (cancellationRepository.markFailed(task.answerId(), workerId, errorCode, Instant.now(clock))) {
            eventPort.publish(task.answerId(), "cancellation_failed", errorCode);
        }
    }
}
