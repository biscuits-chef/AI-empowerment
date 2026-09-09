package com.acme.intelligentqa.adapter.in.web;

import com.acme.intelligentqa.common.error.AuthenticationRequiredException;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.AgentType;
import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.in.AnswerCancellationUseCase;
import com.acme.intelligentqa.domain.port.in.QuestionAnswerUseCase;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 提供问题提交、回答流订阅、停止、重生成和反馈接口。
 */
@RestController
@RequestMapping("/api/v1")
public class QuestionAnswerController {

    /**
     * SSE 连接超时时间（毫秒）。
     */
    private static final long STREAM_TIMEOUT_MILLIS = 120_000L;
    /**
     * 智能问答用例。
     */
    private final QuestionAnswerUseCase questionAnswerUseCase;
    /**
     * 停止回答用例。
     */
    private final AnswerCancellationUseCase answerCancellationUseCase;

    /**
     * 创建 {@code QuestionAnswerController} 实例。
     *
     * @param questionAnswerUseCase 智能问答用例。
     *
     * @param answerCancellationUseCase 停止回答用例。
     */
    public QuestionAnswerController(
            final QuestionAnswerUseCase questionAnswerUseCase,
            final AnswerCancellationUseCase answerCancellationUseCase) {
        this.questionAnswerUseCase = questionAnswerUseCase;
        this.answerCancellationUseCase = answerCancellationUseCase;
    }

    /**
     * 校验输入后提交当前问题。
     *
     * @param principal 认证用户主体。
     *
     * @param chatId 会话 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param request 接口请求。
     *
     * @return 校验输入后提交当前问题。
     */
    @PostMapping("/chats/{chatId}/questions")
    public ResponseEntity<AnswerResponse> submit(
            final Principal principal,
            @PathVariable final UUID chatId,
            @RequestHeader("Idempotency-Key") final String idempotencyKey,
            @Valid @RequestBody final QuestionRequest request) {
        final AnswerSnapshot answer = questionAnswerUseCase.submit(
                owner(principal), chatId, request.getAgentType(), request.getQuestion(),
                request.fileReferences(), idempotencyKey);
        return accepted(answer);
    }

    /**
     * 将原问题作为新的完整问答轮次重新发起并保留历史问答。
     *
     * @param principal 认证用户主体。
     *
     * @param answerId 回答 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 新问答轮次的回答快照。
     */
    @PostMapping("/answers/{answerId}/regenerations")
    public ResponseEntity<AnswerResponse> regenerate(
            final Principal principal,
            @PathVariable final UUID answerId,
            @RequestHeader("Idempotency-Key") final String idempotencyKey) {
        return accepted(questionAnswerUseCase.regenerate(owner(principal), answerId, idempotencyKey));
    }

    /**
     * 申请停止指定回答并返回当前状态。
     *
     * @param principal 认证用户主体。
     *
     * @param answerId 回答 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param request 接口请求。
     *
     * @return 申请停止指定回答并返回当前状态。
     */
    @PostMapping("/answers/{answerId}/cancellation")
    public ResponseEntity<AnswerResponse> cancel(
            final Principal principal,
            @PathVariable final UUID answerId,
            @RequestHeader("Idempotency-Key") final String idempotencyKey,
            @Valid @RequestBody final CancellationRequest request) {
        final AnswerSnapshot answer = answerCancellationUseCase.cancel(
                owner(principal), answerId, idempotencyKey, request.getReason());
        if (answer.status() == AnswerSnapshot.Status.CANCEL_REQUESTED) {
            return accepted(answer);
        }
        return ResponseEntity.ok(AnswerResponse.from(answer));
    }

    /**
     * 读取当前用户的目标业务对象。
     *
     * @param principal 认证用户主体。
     *
     * @param answerId 回答 ID。
     *
     * @return 读取当前用户的目标业务对象。
     */
    @GetMapping("/answers/{answerId}")
    public AnswerResponse get(final Principal principal, @PathVariable final UUID answerId) {
        return AnswerResponse.from(questionAnswerUseCase.getAnswer(owner(principal), answerId));
    }

    /**
     * 提交用户反馈并更新显示状态。
     *
     * @param principal 认证用户主体。
     *
     * @param answerId 回答 ID。
     *
     * @param request 接口请求。
     *
     * @return 用户反馈。
     */
    @PutMapping("/answers/{answerId}/feedback")
    public ResponseEntity<Void> feedback(
            final Principal principal,
            @PathVariable final UUID answerId,
            @Valid @RequestBody final FeedbackRequest request) {
        questionAnswerUseCase.recordFeedback(owner(principal), answerId, request.getFeedback());
        return ResponseEntity.noContent().build();
    }

    /**
     * 处理回答事件列表。
     *
     * @param principal 认证用户主体。
     *
     * @param answerId 回答 ID。
     *
     * @param afterSequence 最后确认的事件序号。
     *
     * @return 回答事件列表。
     */
    @GetMapping(path = "/answers/{answerId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(
            final Principal principal,
            @PathVariable final UUID answerId,
            @RequestHeader(value = "Last-Event-ID", defaultValue = "0") final long afterSequence) {
        final SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        final AtomicReference<QuestionAnswerUseCase.Subscription> subscription = new AtomicReference<>();
        final AtomicBoolean terminal = new AtomicBoolean();
        emitter.onCompletion(() -> close(subscription));
        emitter.onTimeout(() -> close(subscription));
        emitter.onError(error -> close(subscription));
        final QuestionAnswerUseCase.Subscription active = questionAnswerUseCase.subscribe(
                owner(principal), answerId, afterSequence, event -> send(emitter, event, terminal));
        subscription.set(active);
        if (terminal.get()) {
            active.close();
        }
        return emitter;
    }

    /**
     * 处理问题提交受理结果。
     *
     * @param answer 回答快照。
     *
     * @return 问题提交受理结果。
     */
    private ResponseEntity<AnswerResponse> accepted(final AnswerSnapshot answer) {
        final URI streamUri = URI.create("/api/v1/answers/" + answer.id() + "/events");
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.LOCATION, streamUri.toString())
                .body(AnswerResponse.from(answer));
    }

    /**
     * 向 SSE 客户端发送一个回答事件。
     *
     * @param emitter SSE 事件发送器。
     *
     * @param event 回答事件。
     *
     * @param terminal 回答是否已进入终态。
     */
    private void send(final SseEmitter emitter, final AnswerEvent event, final AtomicBoolean terminal) {
        try {
            emitter.send(SseEmitter.event()
                    .id(Long.toString(event.sequence()))
                    .name(event.type())
                    .data(EventData.from(event), MediaType.APPLICATION_JSON));
            if ("completed".equals(event.type())
                    || "error".equals(event.type())
                    || "cancelled".equals(event.type())
                    || "cancellation_failed".equals(event.type())) {
                terminal.set(true);
                emitter.complete();
            }
        } catch (final IOException exception) {
            terminal.set(true);
            emitter.completeWithError(exception);
        }
    }

    /**
     * 移除当前事件订阅并释放资源。
     *
     * @param reference 实体稳定引用。
     */
    private void close(final AtomicReference<QuestionAnswerUseCase.Subscription> reference) {
        final QuestionAnswerUseCase.Subscription subscription = reference.getAndSet(null);
        if (subscription != null) {
            subscription.close();
        }
    }

    /**
     * 处理当前认证用户标识。
     *
     * @param principal 认证用户主体。
     *
     * @return 当前认证用户标识。
     */
    private String owner(final Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().trim().isEmpty()) {
            throw new AuthenticationRequiredException();
        }
        return principal.getName();
    }

    /**
     * 用户提交问题的请求体。
     */
    public static final class QuestionRequest {
        /** 用户在前端选择的 Agent 类型。 */
        @NotNull
        private AgentType agentType;
        /**
         * 用户问题。
         */
        @NotBlank
        @Size(max = 4000)
        private String question;
        /** 本次问题引用的临时文件。 */
        @Valid
        @Size(max = 5)
        private List<QuestionFileRequest> files = Collections.emptyList();

        /**
         * 返回用户选择的 Agent 类型。
         *
         * @return 用户选择的 Agent 类型。
         */
        public AgentType getAgentType() { return agentType; }
        /**
         * 设置用户选择的 Agent 类型。
         *
         * @param value 用户选择的 Agent 类型。
         */
        public void setAgentType(final AgentType value) { this.agentType = value; }

        /**
         * 返回用户问题。
         *
         * @return 用户问题。
         */
        public String getQuestion() { return question; }
        /**
         * 设置用户问题。
         *
         * @param value 输入值。
         */
        public void setQuestion(final String value) { this.question = value; }
        /**
         * 返回问题文件引用请求。
         *
         * @return 问题文件引用请求。
         */
        public List<QuestionFileRequest> getFiles() { return new ArrayList<>(files); }
        /**
         * 设置问题文件引用请求。
         *
         * @param value 问题文件引用请求。
         */
        public void setFiles(final List<QuestionFileRequest> value) {
            this.files = value == null ? Collections.emptyList() : new ArrayList<>(value);
        }
        /**
         * 将接口请求转换为领域文件引用。
         *
         * @return 领域文件引用列表。
         */
        private List<QuestionFileReference> fileReferences() {
            final List<QuestionFileReference> references = new ArrayList<>(files.size());
            for (final QuestionFileRequest file : files) {
                references.add(new QuestionFileReference(file.getFileId(), file.getUsage()));
            }
            return references;
        }
    }

    /**
     * 问题提交中的单个临时文件引用。
     */
    public static final class QuestionFileRequest {
        /** 文件 ID。 */
        @NotNull
        private UUID fileId;
        /** 本次问题中的文件使用角色。 */
        @NotNull
        private TemporaryFile.Usage usage;

        /** @return 文件 ID。 */
        public UUID getFileId() { return fileId; }
        /** @param value 文件 ID。 */
        public void setFileId(final UUID value) { this.fileId = value; }
        /** @return 本次问题中的文件使用角色。 */
        public TemporaryFile.Usage getUsage() { return usage; }
        /** @param value 本次问题中的文件使用角色。 */
        public void setUsage(final TemporaryFile.Usage value) { this.usage = value; }
    }

    /**
     * 回答评价请求体。
     */
    public static final class FeedbackRequest {
        /**
         * 用户反馈。
         */
        @NotNull
        private QuestionAnswerUseCase.Feedback feedback;

        /**
         * 返回用户反馈。
         *
         * @return 用户反馈。
         */
        public QuestionAnswerUseCase.Feedback getFeedback() { return feedback; }
        /**
         * 设置用户反馈。
         *
         * @param value 输入值。
         */
        public void setFeedback(final QuestionAnswerUseCase.Feedback value) { this.feedback = value; }
    }

    /**
     * 停止回答请求体。
     */
    public static final class CancellationRequest {
        /**
         * 原因。
         */
        @NotNull
        private AnswerCancellationUseCase.CancellationReason reason;

        /**
         * 返回处理原因。
         *
         * @return 处理原因。
         */
        public AnswerCancellationUseCase.CancellationReason getReason() { return reason; }
        /**
         * 设置处理原因。
         *
         * @param value 输入值。
         */
        public void setReason(final AnswerCancellationUseCase.CancellationReason value) { this.reason = value; }
    }

    /**
     * 回答持久化快照的接口响应。
     */
    public static final class AnswerResponse {
        /**
         * 回答 ID。
         */
        private final UUID answerId;
        /**
         * 问题 ID。
         */
        private final UUID questionId;
        /**
         * 链路追踪 ID。
         */
        private final UUID traceId;
        /**
         * 原回答 ID。
         */
        private final UUID regeneratedFromAnswerId;
        /**
         * 业务状态。
         */
        private final String status;
        /**
         * 内容。
         */
        private final String content;
        /**
         * 错误码。
         */
        private final String errorCode;
        /**
         * 停止原因。
         */
        private final String cancelReason;
        /**
         * 停止时所处阶段。
         */
        private final String cancelledStage;
        /**
         * 停止失败错误码。
         */
        private final String cancelErrorCode;
        /**
         * 回答事件流地址。
         */
        private final String streamPath;
        /**
         * 创建时间。
         */
        private final Instant createdAt;
        /**
         * 完成时间。
         */
        private final Instant completedAt;
        /**
         * 停止请求时间。
         */
        private final Instant cancelRequestedAt;
        /**
         * 停止完成时间。
         */
        private final Instant cancelledAt;

        /**
         * 创建 {@code AnswerResponse} 实例。
         *
         * @param answer 回答快照。
         */
        private AnswerResponse(final AnswerSnapshot answer) {
            this.answerId = answer.id();
            this.questionId = answer.questionId();
            this.traceId = answer.traceId();
            this.regeneratedFromAnswerId = answer.regeneratedFromAnswerId();
            this.status = answer.status().name();
            this.content = answer.content();
            this.errorCode = answer.errorCode();
            this.cancelReason = answer.cancelReason();
            this.cancelledStage = answer.cancelledStage();
            this.cancelErrorCode = answer.cancelErrorCode();
            this.streamPath = "/api/v1/answers/" + answer.id() + "/events";
            this.createdAt = answer.createdAt();
            this.completedAt = answer.completedAt();
            this.cancelRequestedAt = answer.cancelRequestedAt();
            this.cancelledAt = answer.cancelledAt();
        }

        /**
         * 将源对象转换为接口响应对象。
         *
         * @param answer 回答快照。
         *
         * @return 将源对象转换为接口响应对象。
         */
        static AnswerResponse from(final AnswerSnapshot answer) { return new AnswerResponse(answer); }
        /**
         * 返回回答 ID。
         *
         * @return 回答 ID。
         */
        public UUID getAnswerId() { return answerId; }
        /**
         * 返回问题 ID。
         *
         * @return 问题 ID。
         */
        public UUID getQuestionId() { return questionId; }
        /**
         * 返回链路追踪 ID。
         *
         * @return 链路追踪 ID。
         */
        public UUID getTraceId() { return traceId; }
        /**
         * 返回此次重生成对应的原回答 ID。
         *
         * @return 此次重生成对应的原回答 ID。
         */
        public UUID getRegeneratedFromAnswerId() { return regeneratedFromAnswerId; }
        /**
         * 返回HTTP 或回答状态。
         *
         * @return HTTP 或回答状态。
         */
        public String getStatus() { return status; }
        /**
         * 返回回答或消息内容。
         *
         * @return 回答或消息内容。
         */
        public String getContent() { return content; }
        /**
         * 返回错误码。
         *
         * @return 错误码。
         */
        public String getErrorCode() { return errorCode; }
        /**
         * 返回用户停止回答的原因。
         *
         * @return 用户停止回答的原因。
         */
        public String getCancelReason() { return cancelReason; }
        /**
         * 返回停止时所处的生成阶段。
         *
         * @return 停止时所处的生成阶段。
         */
        public String getCancelledStage() { return cancelledStage; }
        /**
         * 返回停止失败错误码。
         *
         * @return 停止失败错误码。
         */
        public String getCancelErrorCode() { return cancelErrorCode; }
        /**
         * 返回回答事件订阅地址。
         *
         * @return 回答事件订阅地址。
         */
        public String getStreamPath() { return streamPath; }
        /**
         * 返回创建时间。
         *
         * @return 创建时间。
         */
        public Instant getCreatedAt() { return createdAt; }
        /**
         * 返回回答终态时间。
         *
         * @return 回答终态时间。
         */
        public Instant getCompletedAt() { return completedAt; }
        /**
         * 返回停止请求时间。
         *
         * @return 停止请求时间。
         */
        public Instant getCancelRequestedAt() { return cancelRequestedAt; }
        /**
         * 返回停止完成时间。
         *
         * @return 停止完成时间。
         */
        public Instant getCancelledAt() { return cancelledAt; }
    }

    /**
     * 回答事件的 SSE 载荷。
     */
    public static final class EventData {
        /**
         * 输入值。
         */
        private final String value;
        /**
         * 事件发生时间。
         */
        private final Instant occurredAt;

        /**
         * 创建 {@code EventData} 实例。
         *
         * @param value 输入值。
         *
         * @param occurredAt 事件发生时间。
         */
        private EventData(final String value, final Instant occurredAt) {
            this.value = value;
            this.occurredAt = occurredAt;
        }

        /**
         * 将源对象转换为接口响应对象。
         *
         * @param event 回答事件。
         *
         * @return 将源对象转换为接口响应对象。
         */
        static EventData from(final AnswerEvent event) { return new EventData(event.data(), event.occurredAt()); }
        /**
         * 返回事件负载值。
         *
         * @return 事件负载值。
         */
        public String getValue() { return value; }
        /**
         * 返回事件发生时间。
         *
         * @return 事件发生时间。
         */
        public Instant getOccurredAt() { return occurredAt; }
    }
}
