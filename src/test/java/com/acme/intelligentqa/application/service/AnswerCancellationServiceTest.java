package com.acme.intelligentqa.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.intelligentqa.config.CancellationProperties;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.in.AnswerCancellationUseCase;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import com.acme.intelligentqa.domain.port.out.CancellationRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 验证 AnswerCancellationService 的业务行为与边界。
 */
class AnswerCancellationServiceTest {

    /**
     * 固定测试时钟时间。
     */
    private static final Instant NOW = Instant.parse("2026-08-19T10:00:00Z");

    /**
     * 验证停止请求先持久化再发布事件和投递任务。
     */
    @Test
    void persistsRequestPublishesEventAndDispatchesTask() {
        final UUID answerId = UUID.randomUUID();
        final CancellationRepositoryPort repository = mock(CancellationRepositoryPort.class);
        final AnswerEventPort events = mock(AnswerEventPort.class);
        final CancellationDispatcher dispatcher = mock(CancellationDispatcher.class);
        final AnswerSnapshot requested = new AnswerSnapshot(
                answerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                AnswerSnapshot.Status.CANCEL_REQUESTED, "", null, NOW, null,
                "USER_REQUESTED", "QUERYING", null, NOW, null);
        when(repository.request(
                eq("user-1"), eq(answerId), eq("cancel-1"),
                eq(AnswerCancellationUseCase.CancellationReason.USER_REQUESTED),
                eq(NOW), eq(NOW.plusSeconds(30))))
                .thenReturn(new CancellationRepositoryPort.RequestResult(requested, true));
        final AnswerCancellationService service = new AnswerCancellationService(
                repository, events,
                new CancellationProperties(1000, 10000, 2000, 30000, 3, 10),
                dispatcher, Clock.fixed(NOW, ZoneOffset.UTC));

        final AnswerSnapshot result = service.cancel(
                "user-1", answerId, "cancel-1",
                AnswerCancellationUseCase.CancellationReason.USER_REQUESTED);

        assertEquals(AnswerSnapshot.Status.CANCEL_REQUESTED, result.status());
        verify(events).publish(answerId, "cancellation_requested", "USER_REQUESTED");
        verify(dispatcher).dispatch(answerId);
    }

    /**
     * 验证重复幂等停止请求不会再次投递任务。
     */
    @Test
    void doesNotRedispatchIdempotentCancellation() {
        final UUID answerId = UUID.randomUUID();
        final CancellationRepositoryPort repository = mock(CancellationRepositoryPort.class);
        final AnswerEventPort events = mock(AnswerEventPort.class);
        final CancellationDispatcher dispatcher = mock(CancellationDispatcher.class);
        final AnswerSnapshot cancelled = new AnswerSnapshot(
                answerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                AnswerSnapshot.Status.CANCELLED, "部分", null, NOW, NOW,
                "USER_REQUESTED", "GENERATING", null, NOW, NOW);
        when(repository.request(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CancellationRepositoryPort.RequestResult(cancelled, false));
        final AnswerCancellationService service = new AnswerCancellationService(
                repository, events,
                new CancellationProperties(1000, 10000, 2000, 30000, 3, 10),
                dispatcher, Clock.fixed(NOW, ZoneOffset.UTC));

        final AnswerSnapshot result = service.cancel(
                "user-1", answerId, "cancel-1",
                AnswerCancellationUseCase.CancellationReason.USER_REQUESTED);

        assertEquals(AnswerSnapshot.Status.CANCELLED, result.status());
    }
}
