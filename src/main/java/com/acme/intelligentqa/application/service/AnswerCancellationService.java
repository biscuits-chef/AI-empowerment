package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.config.CancellationProperties;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.in.AnswerCancellationUseCase;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import com.acme.intelligentqa.domain.port.out.CancellationRepositoryPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 接收停止回答请求，并以幂等方式创建可恢复的持久化停止任务。
 */
@Service
public class AnswerCancellationService implements AnswerCancellationUseCase {

    /**
     * 请求幂等头名称。
     */
    private static final String IDEMPOTENCY_KEY = "idempotencyKey";
    /**
     * 停止任务仓储。
     */
    private final CancellationRepositoryPort cancellationRepository;
    /**
     * 回答事件端口。
     */
    private final AnswerEventPort eventPort;
    /**
     * 配置参数。
     */
    private final CancellationProperties properties;
    /**
     * 停止任务分发器。
     */
    private final CancellationDispatcher dispatcher;
    /**
     * 系统时钟。
     */
    private final Clock clock;

    /**
     * 创建 {@code AnswerCancellationService} 实例。
     *
     * @param cancellationRepository 停止任务仓储。
     *
     * @param eventPort 回答事件端口。
     *
     * @param properties 配置参数。
     *
     * @param dispatcher 停止任务分发器。
     *
     * @param clock 系统时钟。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public AnswerCancellationService(
            final CancellationRepositoryPort cancellationRepository,
            final AnswerEventPort eventPort,
            final CancellationProperties properties,
            final CancellationDispatcher dispatcher,
            final Clock clock) {
        this.cancellationRepository = cancellationRepository;
        this.eventPort = eventPort;
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    /**
     * 申请停止指定回答并返回当前状态。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param reason 原因。
     *
     * @return 申请停止指定回答并返回当前状态。
     */
    @Override
    @Transactional
    public AnswerSnapshot cancel(
            final String ownerId,
            final UUID answerId,
            final String idempotencyKey,
            final CancellationReason reason) {
        final Instant requestedAt = Instant.now(clock);
        final CancellationRepositoryPort.RequestResult result = cancellationRepository.request(
                ApplicationSupport.requireText(ownerId, "ownerId"),
                Objects.requireNonNull(answerId, "answerId must not be null"),
                ApplicationSupport.requireText(idempotencyKey, IDEMPOTENCY_KEY),
                Objects.requireNonNull(reason, "reason must not be null"),
                requestedAt,
                requestedAt.plusMillis(properties.messageIdWaitMillis()));
        if (result.newlyRequested()) {
            eventPort.publish(answerId, "cancellation_requested", reason.name());
            ApplicationSupport.runAfterCommit(() -> dispatcher.dispatch(answerId));
        }
        return result.answer();
    }

}
