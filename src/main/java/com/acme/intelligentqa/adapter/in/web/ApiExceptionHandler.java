package com.acme.intelligentqa.adapter.in.web;

import com.acme.intelligentqa.common.error.AuthenticationRequiredException;
import com.acme.intelligentqa.common.error.AnswerAlreadyTerminalException;
import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.common.error.ConversationActiveException;
import com.acme.intelligentqa.common.error.EventReplayGapException;
import com.acme.intelligentqa.common.error.FileNotReadyException;
import com.acme.intelligentqa.common.error.FileTooLargeException;
import com.acme.intelligentqa.common.error.IdempotencyConflictException;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.common.error.UnsupportedFileTypeException;
import java.time.Instant;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 将领域与应用异常统一转换为兼容 RFC 7807 的安全错误响应。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 将未认证异常转换为安全响应。
     *
     * @param exception 待转换的原始异常。
     *
     * @param request 接口请求。
     *
     * @return 将未认证异常转换为安全响应。
     */
    @ExceptionHandler(AuthenticationRequiredException.class)
    ResponseEntity<ApiProblem> handleAuthentication(
            final AuthenticationRequiredException exception,
            final HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Authentication required", "请先完成公司统一认证", request);
    }

    /**
     * 将不存在或不可访问资源转换为统一响应。
     *
     * @param exception 待转换的原始异常。
     *
     * @param request 接口请求。
     *
     * @return 将不存在或不可访问资源转换为统一响应。
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiProblem> handleNotFound(
            final ResourceNotFoundException exception,
            final HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Resource not found", "资源不存在或当前用户无权访问", request);
    }

    /**
     * 将事件重放缺口转换为快照恢复提示。
     *
     * @param exception 待转换的原始异常。
     *
     * @param request 接口请求。
     *
     * @return 将事件重放缺口转换为快照恢复提示。
     */
    @ExceptionHandler(EventReplayGapException.class)
    ResponseEntity<ApiProblem> handleReplayGap(
            final EventReplayGapException exception,
            final HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "EVENT_REPLAY_GAP",
                "Event replay gap", "事件历史不完整，请重新加载回答快照", request);
    }

    /**
     * 将回答终态冲突转换为拒绝操作响应。
     *
     * @param exception 待转换的原始异常。
     *
     * @param request 接口请求。
     *
     * @return 将回答终态冲突转换为拒绝操作响应。
     */
    @ExceptionHandler(AnswerAlreadyTerminalException.class)
    ResponseEntity<ApiProblem> handleTerminalAnswer(
            final AnswerAlreadyTerminalException exception,
            final HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "ANSWER_ALREADY_TERMINAL",
                "Answer already terminal", "回答已结束，不能重复执行该操作", request);
    }

    /**
     * 将活动会话删除冲突转换为固定提示。
     *
     * @param exception 待转换的活动会话异常。
     * @param request 接口请求。
     * @return HTTP 409 问题响应。
     */
    @ExceptionHandler(ConversationActiveException.class)
    ResponseEntity<ApiProblem> handleActiveConversation(
            final ConversationActiveException exception,
            final HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "CONVERSATION_ACTIVE",
                "Conversation is active", "会话正在执行，请停止后删除", request);
    }

    /**
     * 将文件状态冲突转换为可恢复的冲突响应。
     *
     * @param exception 待转换的原始异常。
     * @param request 接口请求。
     * @return 冲突问题响应。
     */
    @ExceptionHandler(FileNotReadyException.class)
    ResponseEntity<ApiProblem> handleFileConflict(
            final FileNotReadyException exception, final HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "FILE_NOT_READY",
                "File state conflict", "附件尚未处理完成，请稍后重试", request);
    }

    /**
     * 将幂等请求指纹冲突转换为固定 409 响应。
     *
     * @param exception 待转换的幂等冲突异常。
     * @param request 接口请求。
     * @return 幂等冲突问题响应。
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ApiProblem> handleIdempotencyConflict(
            final IdempotencyConflictException exception, final HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                "Idempotency conflict", "相同幂等键不能用于不同的请求内容", request);
    }

    /**
     * 将文件大小超限转换为载荷过大响应。
     *
     * @param exception 待转换的原始异常。
     * @param request 接口请求。
     * @return 文件过大问题响应。
     */
    @ExceptionHandler({FileTooLargeException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<ApiProblem> handleFileTooLarge(
            final RuntimeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "File too large", "附件不能超过 1 MiB", request);
    }

    /**
     * 将不支持的文件类型转换为媒体类型错误响应。
     *
     * @param exception 待转换的原始异常。
     * @param request 接口请求。
     * @return 文件类型错误问题响应。
     */
    @ExceptionHandler(UnsupportedFileTypeException.class)
    ResponseEntity<ApiProblem> handleUnsupportedFile(
            final UnsupportedFileTypeException exception, final HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE",
                "Unsupported file type", "文件类型不受支持或内容与扩展名不一致", request);
    }

    /**
     * 将输入校验异常转换为请求错误响应。
     *
     * @param exception 待转换的原始异常。
     *
     * @param request 接口请求。
     *
     * @return 将输入校验异常转换为请求错误响应。
     */
    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<ApiProblem> handleBadRequest(final Exception exception, final HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Invalid request", "请求参数或请求体不符合接口约束", request);
    }

    /**
     * 将依赖故障转换为安全服务不可用响应。
     *
     * @param exception 待转换的原始异常。
     *
     * @param request 接口请求。
     *
     * @return 将依赖故障转换为安全服务不可用响应。
     */
    @ExceptionHandler(DependencyUnavailableException.class)
    ResponseEntity<ApiProblem> handleDependencyUnavailable(
            final DependencyUnavailableException exception, final HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, exception.errorCode(),
                "Dependency unavailable", "必要依赖当前不可用，请稍后重试", request);
    }

    /**
     * 将持久化故障转换为不泄露内部细节的服务不可用响应。
     *
     * @param exception 待转换的持久化异常。
     * @param request 接口请求。
     * @return 安全的服务不可用问题响应。
     */
    @ExceptionHandler(PersistenceOperationException.class)
    ResponseEntity<ApiProblem> handlePersistenceUnavailable(
            final PersistenceOperationException exception, final HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "PERSISTENCE_UNAVAILABLE",
                "Dependency unavailable", "服务暂时无法保存请求，请稍后重试", request);
    }

    /**
     * 处理对外问题响应对象。
     *
     * @param status 业务状态。
     *
     * @param code 稳定错误码。
     *
     * @param title 会话名称。
     *
     * @param detail 对外安全错误说明。
     *
     * @param request 接口请求。
     *
     * @return 对外问题响应对象。
     */
    private ResponseEntity<ApiProblem> problem(
            final HttpStatus status,
            final String code,
            final String title,
            final String detail,
            final HttpServletRequest request) {
        final ApiProblem problem = new ApiProblem(
                status.value(), code, UUID.randomUUID().toString(), title,
                detail, request.getRequestURI(), Instant.now());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    /**
     * 统一的安全问题响应结构。
     */
    static final class ApiProblem {
        /**
         * 业务状态。
         */
        private final int status;
        /**
         * 稳定错误码。
         */
        private final String code;
        /**
         * 本次错误的链路追踪 ID。
         */
        private final String traceId;
        /**
         * 会话名称。
         */
        private final String title;
        /**
         * 对外安全错误说明。
         */
        private final String detail;
        /**
         * 错误对应的请求实例路径。
         */
        private final String instance;
        /**
         * 响应时间戳。
         */
        private final Instant timestamp;

        /**
         * 创建 {@code ApiProblem} 实例。
         *
         * @param status 业务状态。
         *
         * @param code 稳定错误码。
         *
         * @param traceId 本次错误的链路追踪 ID。
         *
         * @param title 会话名称。
         *
         * @param detail 对外安全错误说明。
         *
         * @param instance 错误对应的请求实例路径。
         *
         * @param timestamp 响应时间戳。
         */
        ApiProblem(
                final int status,
                final String code,
                final String traceId,
                final String title,
                final String detail,
                final String instance,
                final Instant timestamp) {
            this.status = status;
            this.code = code;
            this.traceId = traceId;
            this.title = title;
            this.detail = detail;
            this.instance = instance;
            this.timestamp = timestamp;
        }

        /**
         * 返回HTTP 或回答状态。
         *
         * @return HTTP 或回答状态。
         */
        public int getStatus() { return status; }
        /**
         * 返回稳定错误码。
         *
         * @return 稳定错误码。
         */
        public String getCode() { return code; }
        /**
         * 返回本次错误的链路追踪 ID。
         *
         * @return 本次错误的链路追踪 ID。
         */
        public String getTraceId() { return traceId; }
        /**
         * 返回会话名称。
         *
         * @return 会话名称。
         */
        public String getTitle() { return title; }
        /**
         * 返回对外安全错误说明。
         *
         * @return 对外安全错误说明。
         */
        public String getDetail() { return detail; }
        /**
         * 返回错误对应的请求实例路径。
         *
         * @return 错误对应的请求实例路径。
         */
        public String getInstance() { return instance; }
        /**
         * 返回响应时间戳。
         *
         * @return 响应时间戳。
         */
        public Instant getTimestamp() { return timestamp; }
    }
}
