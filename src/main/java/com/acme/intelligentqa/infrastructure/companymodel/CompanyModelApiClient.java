package com.acme.intelligentqa.infrastructure.companymodel;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.CompanyModelProperties;
import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * 按公司 HiAgent 运行态协议创建会话、发起流式问答并停止生成。
 */
@Component
public final class CompanyModelApiClient {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyModelApiClient.class);

    /**
     * 模型 HTTP 异常错误码。
     */
    private static final String CODE_HTTP_ERROR = "COMPANY_MODEL_HTTP_ERROR";

    /**
     * 公司模型创建会话接口路径。
     */
    private static final String CREATE_CONVERSATION_PATH = "/create_conversation";
    /**
     * 公司模型流式问答接口路径。
     */
    private static final String CHAT_QUERY_PATH = "/chat_query";
    /**
     * 公司模型停止接口路径。
     */
    private static final String STOP_MESSAGE_PATH = "/stop_message";
    /**
     * 配置参数。
     */
    private final CompanyModelProperties properties;
    /**
     * HTTP 客户端。
     */
    private final RestTemplate restTemplate;
    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;
    /**
     * 公司模型提示词构造器。
     */
    private final CompanyModelPromptFactory promptFactory;

    /**
     * 创建 {@code CompanyModelApiClient} 实例。
     *
     * @param properties 配置参数。
     *
     * @param restTemplate HTTP 客户端。
     *
     * @param objectMapper JSON 对象映射器。
     *
     * @param promptFactory 公司模型提示词构造器。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    CompanyModelApiClient(
            final CompanyModelProperties properties,
            @Qualifier("companyModelRestTemplate") final RestTemplate restTemplate,
            final ObjectMapper objectMapper,
            final CompanyModelPromptFactory promptFactory) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.promptFactory = promptFactory;
    }

    /**
     * 编排证据获取并流式生成回答。
     *
     * @param request 接口请求。
     *
     * @param consumer 事件订阅回调。
     *
     * @return 编排证据获取并流式生成回答。
     */
    public LanguageModelPort.GenerationResult generate(
            final LanguageModelPort.GenerationRequest request,
            final Consumer<String> consumer) {
        return generate(request, consumer, LanguageModelPort.NO_CANCELLATION);
    }

    /**
     * 编排证据获取并流式生成回答。
     *
     * @param request 接口请求。
     *
     * @param consumer 事件订阅回调。
     *
     * @param control 生成取消状态控制器。
     *
     * @return 编排证据获取并流式生成回答。
     */
    public LanguageModelPort.GenerationResult generate(
            final LanguageModelPort.GenerationRequest request,
            final Consumer<String> consumer,
            final LanguageModelPort.GenerationControl control) {
        requireEnabled();
        validateUserId(request.ownerId());
        try {
            final String appConversationId = resolveAppConversationId(request);
            control.onAppConversationId(appConversationId);
            final String query = promptFactory.create(request);
            final ChatQueryRequest body = new ChatQueryRequest(
                    request.ownerId(), appConversationId, query, "streaming", false);
            final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
            final String url = properties.baseUrl() + CHAT_QUERY_PATH;
            LOGGER.info("COMPANY MODEL API CALL -> POST {} (SSE streaming)", url);
            logChatQuerySummary(query);
            executeChatQuery(url, body, decoder, consumer, control);
            if (!decoder.hasAnswer()) {
                throw new DependencyUnavailableException(
                        "COMPANY_MODEL_EMPTY_RESPONSE", "company model stream did not contain an Answer field");
            }
            LOGGER.info("COMPANY MODEL SSE summary: messageId={}, chars={}",
                    maskPlatformId(decoder.lastMessageId()), decoder.totalAnswerCharacters());
            return new LanguageModelPort.GenerationResult("company-hiagent", "stream_end");
        } catch (final DependencyUnavailableException exception) {
            LOGGER.error("Company model generation failed: {}", exception.getMessage());
            throw exception;
        } catch (final RestClientResponseException exception) {
            LOGGER.error("Company model API HTTP error: status={}", exception.getRawStatusCode());
            throw new DependencyUnavailableException(
                    CODE_HTTP_ERROR,
                    "company model API returned HTTP " + exception.getRawStatusCode(),
                    exception);
        } catch (final RestClientException exception) {
            LOGGER.error("Company model API request failed: {}", exception.getMessage());
            throw new DependencyUnavailableException(
                    "COMPANY_MODEL_UNAVAILABLE", "company model API request failed", exception);
        }
    }

    /**
     * 执行流式问答 HTTP 请求并读取 SSE 数据流。
     *
     * @param url 请求 URL。
     * @param body 请求体对象。
     * @param decoder 公司模型事件解码器。
     * @param consumer 事件订阅回调。
     * @param control 生成取消状态控制器。
     */
    private void executeChatQuery(
            final String url,
            final ChatQueryRequest body,
            final CompanyModelEventDecoder decoder,
            final Consumer<String> consumer,
            final LanguageModelPort.GenerationControl control) {
        restTemplate.execute(
                url,
                HttpMethod.POST,
                restTemplate.httpEntityCallback(new HttpEntity<>(body, headers())),
                response -> {
                    final int statusCode = response.getRawStatusCode();
                    if (statusCode >= 400) {
                        LOGGER.error("HTTP {} from {}", statusCode, url);
                        throw new DependencyUnavailableException(
                                CODE_HTTP_ERROR,
                                "company model API returned HTTP " + statusCode);
                    }
                    LOGGER.info("COMPANY MODEL API SSE streaming started for: {}", url);
                    readEventStream(url, response.getBody(), decoder, consumer, control);
                    return null;
                });
    }

    /**
     * 打印流式问答请求摘要：只记录 Query 字符数与响应模式等非敏感元数据，
     * 严禁打印 Query 提示词全文、UserID 与 AppConversationID，满足日志合规红线。
     *
     * @param query 组装后的模型查询提示词，仅取其长度用于诊断。
     */
    private void logChatQuerySummary(final String query) {
        LOGGER.info("COMPANY MODEL CHAT REQUEST: queryChars={}, responseMode=streaming, pubAgentJump=false",
                query == null ? 0 : query.length());
    }

    /**
     * 对平台会话 ID 或消息 ID 做日志脱敏：只保留末 4 位供排查，其余遮蔽，避免平台标识明文落盘。
     *
     * @param platformId 平台标识；可为 null。
     * @return 脱敏后的平台标识；入参为 null 或空时返回 null。
     */
    private String maskPlatformId(final String platformId) {
        if (platformId == null || platformId.isEmpty()) {
            return platformId;
        }
        if (platformId.length() <= 4) {
            return "***";
        }
        return "***" + platformId.substring(platformId.length() - 4);
    }

    /**
     * 调用公司模型协议规定的停止接口。
     *
     * @param userId 公司模型用户标识。
     *
     * @param messageId 消息 ID。
     */
    public void stopMessage(final String userId, final String messageId) {
        requireEnabled();
        validateUserId(userId);
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        final String url = properties.baseUrl() + STOP_MESSAGE_PATH;
        LOGGER.info("COMPANY MODEL API CALL -> POST {}", url);
        final StopMessageRequest body = new StopMessageRequest(userId, messageId);
        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, jsonHeaders()),
                    Void.class);
            LOGGER.info("COMPANY MODEL stop_message success: messageId={}", maskPlatformId(messageId));
        } catch (final RestClientException exception) {
            LOGGER.error("COMPANY MODEL stop_message failed: messageId={}, error={}",
                    maskPlatformId(messageId), exception.getMessage());
            throw new DependencyUnavailableException(
                    "COMPANY_MODEL_STOP_UNAVAILABLE", "company model stop_message request failed", exception);
        }
    }

    /**
     * 解析已有或创建全新的公司模型会话标识。
     *
     * @param request 接口请求。
     * @return 公司模型会话标识。
     */
    private String resolveAppConversationId(final LanguageModelPort.GenerationRequest request) {
        if (request.appConversationId() != null && !request.appConversationId().trim().isEmpty()) {
            return request.appConversationId().trim();
        }
        return createConversation(request.ownerId());
    }

    /**
     * 创建公司模型会话。
     *
     * @param userId 公司模型用户标识。
     *
     * @return 创建公司模型会话。
     */
    private String createConversation(final String userId) {
        final String url = properties.baseUrl() + CREATE_CONVERSATION_PATH;
        LOGGER.info("COMPANY MODEL API CALL -> POST {}", url);
        final CreateConversationRequest body = new CreateConversationRequest(userId, Collections.emptyMap());
        final JsonNode response = restTemplate.postForObject(
                url,
                new HttpEntity<>(body, headers()),
                JsonNode.class);
        final String conversationId = response == null
                ? "" : response.path("Conversation").path("AppConversationID").asText("");
        if (conversationId.trim().isEmpty()) {
            LOGGER.error("COMPANY MODEL create_conversation failed: missing AppConversationID");
            throw new DependencyUnavailableException(
                    "COMPANY_MODEL_INVALID_CONVERSATION", "create_conversation returned no AppConversationID");
        }
        LOGGER.info("COMPANY MODEL create_conversation success: AppConversationID={}", maskPlatformId(conversationId));
        return conversationId;
    }

    /**
     * 增量读取公司模型 SSE 数据流。
     *
     * @param url 请求 URL。
     *
     * @param stream 订阅当前用户可访问的回答流。
     *
     * @param decoder 公司模型事件解码器。
     *
     * @param consumer 事件订阅回调。
     *
     * @param control 生成取消状态控制器。
     *
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    private void readEventStream(
            final String url,
            final java.io.InputStream stream,
            final CompanyModelEventDecoder decoder,
            final Consumer<String> consumer,
            final LanguageModelPort.GenerationControl control) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            final StringBuilder data = new StringBuilder();
            long responseCharacters = 0L;
            String line;
            while ((line = readBoundedLine(reader, properties.maxRequestCharacters())) != null) {
                responseCharacters += line.length() + 1L;
                if (responseCharacters > (long) properties.maxRequestCharacters() * 4L) {
                    throw responseTooLarge("company model SSE response exceeded the configured total limit");
                }
                accumulateEventLine(line, data, decoder, consumer, control);
            }
            flushEvent(data, decoder, consumer, control);
            decoder.flush(consumer);
            LOGGER.info("COMPANY MODEL SSE DONE: forwarded {} events from {}", decoder.eventCount(), url);
        }
    }

    /**
     * 按 SSE 行语义累积数据并在事件边界触发解码。
     *
     * @param line 当前 SSE 行。
     *
     * @param data 当前事件已累积的数据。
     *
     * @param decoder 公司模型事件解码器。
     *
     * @param consumer 事件订阅回调。
     *
     * @param control 生成取消状态控制器。
     *
     * @throws IOException 解码或取消处理失败时抛出。
     */
    private void accumulateEventLine(
            final String line,
            final StringBuilder data,
            final CompanyModelEventDecoder decoder,
            final Consumer<String> consumer,
            final LanguageModelPort.GenerationControl control) throws IOException {
        if (line.isEmpty()) {
            flushEvent(data, decoder, consumer, control);
            return;
        }
        if (!line.startsWith("data:")) {
            return;
        }
        String eventLine = line.substring(5).trim();
        while (eventLine.startsWith("data:")) {
            eventLine = eventLine.substring(5).trim();
        }
        final int separatorLength = data.length() > 0 ? 1 : 0;
        if ((long) data.length() + separatorLength + eventLine.length()
                > properties.maxRequestCharacters()) {
            throw responseTooLarge("company model SSE event exceeded the configured event limit");
        }
        if (data.length() > 0) {
            data.append('\n');
        }
        data.append(eventLine);
    }

    /**
     * 读取一行并在分配无界字符串前执行字符上限检查。
     *
     * @param reader SSE 字符流读取器。
     *
     * @param maximumCharacters 单行最大字符数。
     *
     * @return 当前行；流结束且没有剩余字符时返回空值。
     *
     * @throws IOException 读取底层流失败时抛出。
     */
    private String readBoundedLine(
            final Reader reader,
            final int maximumCharacters) throws IOException {
        final StringBuilder line = new StringBuilder(Math.min(maximumCharacters, 1024));
        int character;
        while ((character = reader.read()) != -1) {
            if (character == '\n') {
                break;
            }
            if (line.length() >= maximumCharacters) {
                throw responseTooLarge("company model SSE line exceeded the configured line limit");
            }
            line.append((char) character);
        }
        if (character == -1 && line.length() == 0) {
            return null;
        }
        if (line.length() > 0 && line.charAt(line.length() - 1) == '\r') {
            line.setLength(line.length() - 1);
        }
        return line.toString();
    }

    /**
     * 创建不回显响应正文的模型响应超限异常。
     *
     * @param detail 安全诊断说明。
     *
     * @return 模型响应超限异常。
     */
    private DependencyUnavailableException responseTooLarge(final String detail) {
        return new DependencyUnavailableException("COMPANY_MODEL_RESPONSE_TOO_LARGE", detail);
    }

    /**
     * 提交已经累积完整的模型事件。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     *
     * @param decoder 公司模型事件解码器。
     *
     * @param consumer 事件订阅回调。
     *
     * @param control 生成取消状态控制器。
     *
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    private void flushEvent(
            final StringBuilder data,
            final CompanyModelEventDecoder decoder,
            final Consumer<String> consumer,
            final LanguageModelPort.GenerationControl control) throws IOException {
        if (data.length() > 0) {
            decoder.accept(data.toString(), consumer, control::onMessageId);
            data.setLength(0);
            if (control.isCancellationRequested()) {
                throw new com.acme.intelligentqa.common.error.GenerationCancelledException();
            }
        }
    }

    /**
     * 处理HTTP 请求头。
     *
     * @return HTTP 请求头。
     */
    private HttpHeaders headers() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
        return headers;
    }

    /**
     * 创建 JSON 请求和响应所需的 HTTP 头。
     *
     * @return 创建 JSON 请求和响应所需的 HTTP 头。
     */
    private HttpHeaders jsonHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * 校验真实模型调用已显式启用。
     */
    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new DependencyUnavailableException(
                    "COMPANY_MODEL_UNCONFIGURED", "company model API is not enabled");
        }
    }

    /**
     * 校验公司模型用户 ID 符合接口长度约束。
     *
     * @param userId 公司模型用户标识。
     */
    private void validateUserId(final String userId) {
        if (userId.length() > 20) {
            throw new DependencyUnavailableException(
                    "COMPANY_MODEL_USER_ID_INVALID", "company model UserID must contain 1 to 20 characters");
        }
    }

    /**
     * 公司 HiAgent 创建会话请求。
     */
    private static final class CreateConversationRequest {
        /**
         * 公司智能体初始化变量。
         */
        private final Map<String, String> inputs;
        /**
         * 公司模型用户标识。
         */
        private final String userId;

        /**
         * 创建 {@code CreateConversationRequest} 实例。
         *
         * @param userId 公司模型用户标识。
         *
         * @param inputs 公司智能体初始化变量。
         */
        CreateConversationRequest(final String userId, final Map<String, String> inputs) {
            this.userId = userId;
            this.inputs = inputs;
        }

        /**
         * 返回公司智能体初始化变量。
         *
         * @return 公司智能体初始化变量。
         */
        @JsonProperty("Inputs")
        public Map<String, String> getInputs() { return inputs; }
        /**
         * 返回公司模型用户标识。
         *
         * @return 公司模型用户标识。
         */
        @JsonProperty("UserID")
        public String getUserId() { return userId; }
    }

    /**
     * 公司 HiAgent 停止消息请求。
     */
    private static final class StopMessageRequest {
        /**
         * 公司模型用户标识。
         */
        private final String userId;
        /**
         * 消息 ID。
         */
        private final String messageId;

        /**
         * 创建 {@code StopMessageRequest} 实例。
         *
         * @param userId 公司模型用户标识。
         *
         * @param messageId 消息 ID。
         */
        StopMessageRequest(final String userId, final String messageId) {
            this.userId = userId;
            this.messageId = messageId;
        }

        /**
         * 返回公司模型用户标识。
         *
         * @return 公司模型用户标识。
         */
        @JsonProperty("UserID")
        public String getUserId() { return userId; }
        /**
         * 返回公司模型消息 ID。
         *
         * @return 公司模型消息 ID。
         */
        @JsonProperty("MessageID")
        public String getMessageId() { return messageId; }
    }

    /**
     * 公司 HiAgent 流式问答请求。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static final class ChatQueryRequest {
        /**
         * 公司模型用户标识。
         */
        private final String userId;
        /**
         * 公司模型会话 ID。
         */
        private final String appConversationId;
        /**
         * 模型查询文本。
         */
        private final String query;
        /**
         * 公司模型响应模式。
         */
        private final String responseMode;
        /**
         * 是否启用公司平台的发布智能体跳转。
         */
        private final boolean pubAgentJump;

        /**
         * 创建 {@code ChatQueryRequest} 实例。
         *
         * @param userId 公司模型用户标识。
         *
         * @param appConversationId 公司模型会话 ID。
         *
         * @param query 模型查询文本。
         *
         * @param responseMode 公司模型响应模式。
         *
         * @param pubAgentJump 是否启用公司平台的发布智能体跳转。
         */
        ChatQueryRequest(
                final String userId,
                final String appConversationId,
                final String query,
                final String responseMode,
                final boolean pubAgentJump) {
            this.userId = userId;
            this.appConversationId = appConversationId;
            this.query = query;
            this.responseMode = responseMode;
            this.pubAgentJump = pubAgentJump;
        }

        /**
         * 返回公司模型用户标识。
         *
         * @return 公司模型用户标识。
         */
        @JsonProperty("UserID")
        public String getUserId() { return userId; }
        /**
         * 返回公司模型会话 ID。
         *
         * @return 公司模型会话 ID。
         */
        @JsonProperty("AppConversationID")
        public String getAppConversationId() { return appConversationId; }
        /**
         * 返回模型查询文本。
         *
         * @return 模型查询文本。
         */
        @JsonProperty("Query")
        public String getQuery() { return query; }
        /**
         * 返回公司模型响应模式。
         *
         * @return 公司模型响应模式。
         */
        @JsonProperty("ResponseMode")
        public String getResponseMode() { return responseMode; }
        /**
         * 读取公司平台智能体跳转开关。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @JsonProperty("PubAgentJump")
        public boolean isPubAgentJump() { return pubAgentJump; }
    }
}
