package com.acme.intelligentqa.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.intelligentqa.common.error.IdempotencyConflictException;
import com.acme.intelligentqa.domain.model.AgentType;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.QuestionSubmission;
import com.acme.intelligentqa.domain.port.in.AnswerCancellationUseCase;
import com.acme.intelligentqa.domain.port.in.QuestionAnswerUseCase;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 验证 QuestionAnswerController 的业务行为与边界。
 */
class QuestionAnswerControllerTest {

    /**
     * 入站用例。
     */
    private QuestionAnswerUseCase useCase;
    /**
     * 停止回答用例。
     */
    private AnswerCancellationUseCase cancellationUseCase;
    /**
     * 模拟 MVC 请求客户端。
     */
    private MockMvc mockMvc;

    /**
     * 初始化每个测试使用的隔离环境。
     */
    @BeforeEach
    void setUp() {
        useCase = mock(QuestionAnswerUseCase.class);
        cancellationUseCase = mock(AnswerCancellationUseCase.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuestionAnswerController(useCase, cancellationUseCase))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    /**
     * 验证提交问题返回已持久化的受理凭据。
     *
     * @throws Exception 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void returnsAcceptedTicketForQuestion() throws Exception {
        final UUID chatId = UUID.randomUUID();
        final UUID answerId = UUID.randomUUID();
        final UUID questionId = UUID.randomUUID();
        final UUID traceId = UUID.randomUUID();
        final Instant now = Instant.parse("2026-08-19T01:00:00Z");
        final AnswerSnapshot answer = new AnswerSnapshot(
                answerId, chatId, questionId, traceId, null,
                AnswerSnapshot.Status.PENDING, "", null, now, null);
        final Conversation conversation = new Conversation(
                chatId, "user-1", "问题", now, now);
        when(useCase.submitQuestion(
                eq("user-1"), isNull(), eq(AgentType.SMART_DATA), eq("问题"),
                anyList(), eq("request-1"))).thenReturn(
                        new QuestionSubmission(conversation, answer, true));
        final Principal principal = () -> "user-1";

        mockMvc.perform(post("/api/v1/questions/submission")
                        .principal(principal)
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatId\":null,\"agentType\":\"SMART_DATA\",\"question\":\"问题\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/answers/" + answerId + "/events"))
                .andExpect(jsonPath("$.conversation.id").value(chatId.toString()))
                .andExpect(jsonPath("$.conversationCreated").value(true))
                .andExpect(jsonPath("$.answer.answerId").value(answerId.toString()))
                .andExpect(jsonPath("$.answer.traceId").value(traceId.toString()))
                .andExpect(jsonPath("$.answer.status").value("PENDING"));
    }

    /**
     * 验证已有会话的后续提问可以省略 Agent 类型。
     *
     * @throws Exception 请求执行失败时抛出。
     */
    @Test
    void acceptsFollowUpWithoutAgentType() throws Exception {
        final UUID chatId = UUID.randomUUID();
        final UUID answerId = UUID.randomUUID();
        final Instant now = Instant.parse("2026-08-19T01:00:00Z");
        final AnswerSnapshot answer = new AnswerSnapshot(
                answerId, chatId, UUID.randomUUID(), UUID.randomUUID(), null,
                AnswerSnapshot.Status.PENDING, "", null, now, null);
        final Conversation conversation = new Conversation(
                chatId, "user-1", "已有会话", AgentType.SMART_DATA, now, now);
        when(useCase.submitQuestion(
                eq("user-1"), eq(chatId), isNull(), eq("继续提问"),
                anyList(), eq("follow-up-1"))).thenReturn(
                        new QuestionSubmission(conversation, answer, false));

        mockMvc.perform(post("/api/v1/questions/submission")
                        .principal((Principal) () -> "user-1")
                        .header("Idempotency-Key", "follow-up-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatId\":\"" + chatId + "\",\"question\":\"继续提问\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.conversationCreated").value(false))
                .andExpect(jsonPath("$.answer.answerId").value(answerId.toString()));
    }

    /**
     * 验证第一阶段拒绝在问题请求中携带文件引用。
     *
     * @throws Exception 当请求执行失败时抛出。
     */
    @Test
    void rejectsFileReferencesInPhaseOne() throws Exception {
        final Principal principal = () -> "user-1";
        when(useCase.submitQuestion(any(), any(), any(), any(), anyList(), any()))
                .thenThrow(new AssertionError("must not be called"));

        mockMvc.perform(post("/api/v1/questions/submission")
                        .principal(principal)
                        .header("Idempotency-Key", "request-with-file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentType\":\"SMART_DATA\",\"question\":\"问题\",\"files\":["
                                + "{\"fileId\":\"00000000-0000-0000-0000-000000000001\","
                                + "\"usage\":\"QUERY_INPUT\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /**
     * 验证无认证主体时拒绝访问。
     *
     * @throws Exception 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void rejectsMissingAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/questions/submission")
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentType\":\"SMART_DATA\",\"question\":\"问题\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.detail").value("请先完成公司统一认证"));
    }

    /**
     * 验证问题请求体校验。
     *
     * @throws Exception 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void validatesQuestionBody() throws Exception {
        final Principal principal = () -> "user-1";
        when(useCase.submitQuestion(any(), any(), any(), any(), anyList(), any()))
                .thenThrow(new AssertionError("must not be called"));

        mockMvc.perform(post("/api/v1/questions/submission")
                        .principal(principal)
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentType\":\"SMART_DATA\",\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 验证首次提问必须携带前端选择的 Agent 类型。
     *
     * @throws Exception 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void rejectsMissingAgentTypeForNewConversation() throws Exception {
        mockMvc.perform(post("/api/v1/questions/submission")
                        .principal((Principal) () -> "user-1")
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"问题\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 验证未知 Agent 类型不会进入应用用例。
     *
     * @throws Exception 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void rejectsUnknownAgentType() throws Exception {
        mockMvc.perform(post("/api/v1/questions/submission")
                        .principal((Principal) () -> "user-1")
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentType\":\"UNKNOWN\",\"question\":\"问题\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.detail").value("请求参数或请求体不符合接口约束"));
    }

    /**
     * 验证同一幂等键用于不同请求时返回稳定 409 契约。
     *
     * @throws Exception 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void returnsStableProblemForIdempotencyConflict() throws Exception {
        when(useCase.submitQuestion(any(), any(), any(), any(), anyList(), any()))
                .thenThrow(new IdempotencyConflictException());

        mockMvc.perform(post("/api/v1/questions/submission")
                        .principal((Principal) () -> "user-1")
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentType\":\"SMART_DATA\",\"question\":\"问题\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency conflict"))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.detail").value("相同幂等键不能用于不同的请求内容"));
    }

    /**
     * 验证畸形 JSON 不向客户端回显框架解析细节。
     *
     * @throws Exception 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void hidesParserDetailsForMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/questions/submission")
                        .principal((Principal) () -> "user-1")
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("请求参数或请求体不符合接口约束"));
    }

    /**
     * 验证停止请求被正确受理。
     *
     * @throws Exception 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void acceptsCancellationRequest() throws Exception {
        final UUID answerId = UUID.randomUUID();
        final AnswerSnapshot answer = new AnswerSnapshot(
                answerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                AnswerSnapshot.Status.CANCEL_REQUESTED, "部分回答", null,
                Instant.parse("2026-08-19T01:00:00Z"), null,
                "USER_REQUESTED", "GENERATING", null,
                Instant.parse("2026-08-19T01:00:01Z"), null);
        when(cancellationUseCase.cancel(
                eq("user-1"), eq(answerId), eq("cancel-1"),
                eq(AnswerCancellationUseCase.CancellationReason.USER_REQUESTED))).thenReturn(answer);

        mockMvc.perform(post("/api/v1/answers/{answerId}/cancellation", answerId)
                        .principal((Principal) () -> "user-1")
                        .header("Idempotency-Key", "cancel-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"USER_REQUESTED\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("CANCEL_REQUESTED"))
                .andExpect(jsonPath("$.cancelledStage").value("GENERATING"));
    }
}
