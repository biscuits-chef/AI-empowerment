package com.acme.intelligentqa.infrastructure.companymodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 模拟公司大模型 HTTP 传输层响应拦截器，以原始 SSE 报文返回深度思考与问答正文，避免侵入上层适配器代码。
 */
public class SimulatedCompanyModelHttpInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 会话创建接口路径后缀。
     */
    private static final String PATH_CREATE_CONVERSATION = "/create_conversation";

    /**
     * 流式问答接口路径后缀。
     */
    private static final String PATH_CHAT_QUERY = "/chat_query";

    /**
     * 停止生成接口路径后缀。
     */
    private static final String PATH_STOP_MESSAGE = "/stop_message";

    /**
     * 模拟消息标识。
     */
    private static final String MOCK_MESSAGE_ID = "msg-mock-puyin-001";

    /**
     * 模拟会话标识。
     */
    private static final String MOCK_CONVERSATION_ID = "conv-mock-puyin-001";

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 流式数据帧输出延迟时间（毫秒）。
     */
    private final long streamDelayMillis;

    /**
     * 创建默认延迟的模拟 HTTP 拦截器。
     */
    public SimulatedCompanyModelHttpInterceptor() {
        this(new ObjectMapper(), 70L);
    }

    /**
     * 创建指定帧延迟的模拟 HTTP 拦截器。
     *
     * @param streamDelayMillis 每帧之间的延迟毫秒数。
     */
    public SimulatedCompanyModelHttpInterceptor(final long streamDelayMillis) {
        this(new ObjectMapper(), streamDelayMillis);
    }

    /**
     * 创建指定对象映射器与帧延迟的模拟 HTTP 拦截器。
     *
     * @param objectMapper JSON 对象映射器。
     * @param streamDelayMillis 每帧之间的延迟毫秒数。
     */
    public SimulatedCompanyModelHttpInterceptor(
            final ObjectMapper objectMapper,
            final long streamDelayMillis) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.streamDelayMillis = streamDelayMillis;
    }

    /**
     * 拦截针对公司大模型的 HTTP 请求并在传输层模拟返回原始协议报文。
     *
     * @param request 客户端 HTTP 请求。
     * @param body 请求载荷字节。
     * @param execution 拦截器执行上下文。
     * @return 模拟的客户端 HTTP 响应。
     * @throws IOException 当执行网络调用或解析报文发生 IO 异常时抛出。
     */
    @Override
    public ClientHttpResponse intercept(
            final HttpRequest request,
            final byte[] body,
            final ClientHttpRequestExecution execution) throws IOException {
        final String path = request.getURI().getPath();
        if (path != null && path.endsWith(PATH_CREATE_CONVERSATION)) {
            return mockCreateConversationResponse();
        }
        if (path != null && path.endsWith(PATH_CHAT_QUERY)) {
            return mockChatQueryStreamResponse();
        }
        if (path != null && path.endsWith(PATH_STOP_MESSAGE)) {
            return mockStopMessageResponse();
        }
        return execution.execute(request, body);
    }

    /**
     * 模拟生成创建会话接口的 JSON 响应报文。
     *
     * @return 状态码 200 的会话创建响应。
     */
    private ClientHttpResponse mockCreateConversationResponse() {
        final String json = "{\"Conversation\":{\"AppConversationID\":\"" + MOCK_CONVERSATION_ID + "\"}}";
        return new SimulatedClientHttpResponse(
                HttpStatus.OK,
                MediaType.APPLICATION_JSON,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 模拟生成停止消息接口的 JSON 响应报文。
     *
     * @return 状态码 200 的停止响应。
     */
    private ClientHttpResponse mockStopMessageResponse() {
        return new SimulatedClientHttpResponse(
                HttpStatus.OK,
                MediaType.APPLICATION_JSON,
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 模拟生成流式问答接口的 SSE 响应报文。
     *
     * @return 包含深度思考与完整业务正文的流式 HTTP 响应。
     */
    @SuppressWarnings("PMD.CloseResource")
    private ClientHttpResponse mockChatQueryStreamResponse() {
        final List<String> frames = buildRawSseFrames();
        final InputStream stream = new SimulatedSseInputStream(frames, streamDelayMillis);
        return new SimulatedClientHttpResponse(
                HttpStatus.OK,
                MediaType.TEXT_EVENT_STREAM,
                stream);
    }

    /**
     * 构建包含思考、正文、耗时、推荐追问与扩展属性的 HiAgent 原始 SSE 报文帧列表。
     *
     * @return 符合平台规范的原始 SSE 帧字符串列表。
     */
    private List<String> buildRawSseFrames() {
        final List<String> frames = new ArrayList<>();

        final Map<String, Object> start = new LinkedHashMap<>();
        start.put("event", "message_start");
        start.put("id", MOCK_MESSAGE_ID);
        start.put("conversation_id", MOCK_CONVERSATION_ID);
        frames.add(toSseLine(start));

        final String[] chunks = {
                "<think>\n",
                "收到用户业务咨询，开始分析问题语义与业务意图...\n",
                "1. 意图识别：识别为理财产品基础信息与交易规则查询。\n",
                "2. 知识库检索：已检索到《浦银理财产品管理规范_2024版》与《业务办理操作指引》。\n",
                "3. 业务数据库查询：已通过受控通道查询产品要素表（PROD_INFO）及申购开放状态。\n",
                "4. 双通道交叉比对：知识库与数据库中产品起售金额、支持渠道与到期规则完全一致。\n",
                "5. 综合生成策略：组织标准答复，明确办理渠道、材料准备清单及系统流转节点。\n",
                "</think>\n\n",
                "根据浦银理财最新业务规范与管理规定，",
                "您所咨询的理财产品与业务现已支持全流程线上办理。\n\n",
                "### 办理要点与指引：\n",
                "1. **适用渠道**：统一协同门户【业务办理】模块或手机银行企业版。\n",
                "2. **所需材料**：法定代表人或经办人有效身份证件、业务办理授权书及机构账户信息。\n",
                "3. **审核周期**：线上提交申请后，系统预计在 1 个工作日内完成合规与资质校验，审核通过后即刻生效。\n\n",
                "如有任何疑问，可参考下方推荐问题进一步咨询。"
        };
        for (final String chunk : chunks) {
            final Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("event", "message");
            msg.put("id", MOCK_MESSAGE_ID);
            msg.put("Answer", chunk);
            frames.add(toSseLine(msg));
        }

        final Map<String, Object> end = new LinkedHashMap<>();
        end.put("event", "message_end");
        end.put("id", MOCK_MESSAGE_ID);
        end.put("time_cost", 1280);
        end.put("rec_questions", Arrays.asList(
                "如何在线办理该理财业务？",
                "办理该业务需要准备哪些申请材料？",
                "业务办理的审核周期大概是多久？"));
        end.put("rewrite_query", "浦银理财产品线上办理流程及所需材料要求");
        end.put("doc_reference_json",
                "[{\"title\":\"浦银理财产品管理规范_2024版.pdf\",\"page\":12},"
                        + "{\"title\":\"理财产品业务办理操作指引.pdf\",\"page\":5}]");
        end.put("sql_message",
                "SELECT prod_code, prod_name, min_amount, status FROM prod_info "
                        + "WHERE prod_status = 'OPEN' AND channel = 'ONLINE'");
        end.put("scene_name", "理财产品信息问答");
        frames.add(toSseLine(end));

        frames.add("data: [DONE]\n\n");
        return frames;
    }

    /**
     * 将键值映射格式化为单条 SSE 数据帧行文本。
     *
     * @param payload 待序列化的对象映射。
     * @return 格式化后的 SSE 数据帧字符串。
     */
    private String toSseLine(final Map<String, Object> payload) {
        try {
            return "data: " + objectMapper.writeValueAsString(payload) + "\n\n";
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to serialize SSE payload", exception);
        }
    }

    /**
     * 模拟 HTTP 客户端响应实现类。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Internal simulated response")
    private static final class SimulatedClientHttpResponse implements ClientHttpResponse {

        /**
         * 响应状态码。
         */
        private final HttpStatus status;

        /**
         * 响应头映射。
         */
        private final HttpHeaders headers;

        /**
         * 响应输入流。
         */
        private final InputStream body;

        /**
         * 创建模拟响应实例。
         *
         * @param status 响应状态枚举。
         * @param mediaType 媒体内容类型。
         * @param body 响应体输入流。
         */
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Internal simulated response")
        SimulatedClientHttpResponse(
                final HttpStatus status,
                final MediaType mediaType,
                final InputStream body) {
            this.status = status;
            this.headers = new HttpHeaders();
            this.headers.setContentType(mediaType);
            this.body = body;
        }

        /**
         * 获取响应状态枚举。
         *
         * @return 响应状态枚举。
         */
        @Override
        public HttpStatus getStatusCode() {
            return status;
        }

        /**
         * 获取状态码原生数字。
         *
         * @return 状态码整数。
         */
        @Override
        public int getRawStatusCode() {
            return status.value();
        }

        /**
         * 获取状态描述短语。
         *
         * @return 状态文本。
         */
        @Override
        public String getStatusText() {
            return status.getReasonPhrase();
        }

        /**
         * 获取响应头。
         *
         * @return 响应头映射。
         */
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        /**
         * 获取响应体输入流。
         *
         * @return 输入流。
         */
        @Override
        public InputStream getBody() {
            return body;
        }

        /**
         * 关闭响应输入流。
         */
        @Override
        public void close() {
            if (body != null) {
                try {
                    body.close();
                } catch (final IOException ignored) {
                    // 忽略关闭异常
                }
            }
        }
    }
}
