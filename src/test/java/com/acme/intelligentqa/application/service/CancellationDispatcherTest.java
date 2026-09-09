package com.acme.intelligentqa.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.CancellationProperties;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import com.acme.intelligentqa.domain.port.out.CancellationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.LanguageModelCancellationPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 验证 CancellationDispatcher 的业务行为与边界。
 */
class CancellationDispatcherTest {

    /**
     * 固定测试时钟时间。
     */
    private static final Instant NOW = Instant.parse("2026-08-19T09:00:00Z");
    /**
     * 持久化仓储。
     */
    private CancellationRepositoryPort repository;
    /**
     * 测试大模型端口。
     */
    private LanguageModelCancellationPort model;
    /**
     * 回答事件列表。
     */
    private AnswerEventPort events;
    /**
     * 停止任务分发器。
     */
    private CancellationDispatcher dispatcher;

    /**
     * 初始化每个测试使用的隔离环境。
     */
    @BeforeEach
    void setUp() {
        repository = mock(CancellationRepositoryPort.class);
        model = mock(LanguageModelCancellationPort.class);
        events = mock(AnswerEventPort.class);
        dispatcher = new CancellationDispatcher(
                repository, model, events,
                new CancellationProperties(1000, 10000, 2000, 30000, 3, 10),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * 验证远端停止成功后回答转换为已停止终态。
     */
    @Test
    void stopsRemoteGenerationAndMarksAnswerCancelled() {
        final UUID answerId = UUID.randomUUID();
        when(repository.claim(eq(answerId), anyString(), eq(NOW), any(), eq(3)))
                .thenReturn(Optional.of(new CancellationRepositoryPort.Task(
                        answerId, "user-1", "message-1", AnswerSnapshot.Status.GENERATING.name(), 1)));
        when(repository.markCancelled(eq(answerId), anyString(), eq(NOW))).thenReturn(true);

        dispatcher.dispatch(answerId);

        verify(model).stop("user-1", "message-1");
        verify(events).publish(answerId, "cancelled", "USER_REQUESTED");
    }

    /**
     * 验证远端停止瞬时失败可以按策略重试。
     */
    @Test
    void retriesTransientRemoteStopFailure() {
        final UUID answerId = UUID.randomUUID();
        when(repository.claim(eq(answerId), anyString(), eq(NOW), any(), eq(3)))
                .thenReturn(Optional.of(new CancellationRepositoryPort.Task(
                        answerId, "user-1", "message-1", AnswerSnapshot.Status.GENERATING.name(), 1)));
        doThrow(new DependencyUnavailableException("STOP_TIMEOUT", "timeout"))
                .when(model).stop("user-1", "message-1");

        dispatcher.dispatch(answerId);

        verify(repository).reschedule(
                eq(answerId), anyString(), eq("STOP_TIMEOUT"), eq(NOW.plusMillis(2000)));
    }

    /**
     * 验证模型尚未启动时停止任务在本地收敛。
     */
    @Test
    void cancelsLocallyWhenModelGenerationHasNotStarted() {
        final UUID answerId = UUID.randomUUID();
        when(repository.claim(eq(answerId), anyString(), eq(NOW), any(), eq(3)))
                .thenReturn(Optional.of(new CancellationRepositoryPort.Task(
                        answerId, "user-1", null, AnswerSnapshot.Status.QUERYING.name(), 1)));
        when(repository.markCancelled(eq(answerId), anyString(), eq(NOW))).thenReturn(true);

        dispatcher.dispatch(answerId);

        verify(repository).markCancelled(eq(answerId), anyString(), eq(NOW));
        verify(events).publish(answerId, "cancelled", "USER_REQUESTED");
    }

    /**
     * 验证扫描会发布最后一次租约崩溃后的失败收敛事件。
     */
    @Test
    void convergesAndPublishesExpiredExhaustedTask() {
        final UUID answerId = UUID.randomUUID();
        when(repository.findDispatchable(NOW, 10))
                .thenReturn(Collections.singletonList(answerId));
        when(repository.markExpiredExhausted(
                answerId, NOW, 3, "CANCELLATION_ATTEMPTS_EXHAUSTED"))
                .thenReturn(true);

        dispatcher.scan();

        verify(events).publish(
                answerId, "cancellation_failed", "CANCELLATION_ATTEMPTS_EXHAUSTED");
    }
}
