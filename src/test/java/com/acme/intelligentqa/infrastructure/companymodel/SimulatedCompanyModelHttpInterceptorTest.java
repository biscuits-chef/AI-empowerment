package com.acme.intelligentqa.infrastructure.companymodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 验证模拟大模型 HTTP 拦截器在传输层的报文拦截与流式数据生成行为。
 */
class SimulatedCompanyModelHttpInterceptorTest {

    /**
     * 验证拦截创建会话请求并返回包含模拟会话标识的 JSON 响应。
     *
     * @throws IOException 当 IO 异常发生时抛出。
     */
    @Test
    void interceptsCreateConversationAndReturnsAppConversationId() throws IOException {
        final SimulatedCompanyModelHttpInterceptor interceptor = new SimulatedCompanyModelHttpInterceptor(0L);
        final HttpRequest request = createMockRequest("http://company-hiagent-service/create_conversation");
        final ClientHttpRequestExecution execution = (req, body) -> {
            throw new AssertionError("Should not delegate to execution");
        };

        final ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getRawStatusCode());
        assertEquals("OK", response.getStatusText());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());

        final String body = readString(response);
        assertTrue(body.contains("conv-mock-puyin-001"));
        response.close();
    }

    /**
     * 验证拦截停止生成请求并返回空 JSON 对象。
     *
     * @throws IOException 当 IO 异常发生时抛出。
     */
    @Test
    void interceptsStopMessageAndReturnsEmptyJson() throws IOException {
        final SimulatedCompanyModelHttpInterceptor interceptor = new SimulatedCompanyModelHttpInterceptor(0L);
        final HttpRequest request = createMockRequest("http://company-hiagent-service/stop_message");
        final ClientHttpRequestExecution execution = (req, body) -> {
            throw new AssertionError("Should not delegate to execution");
        };

        final ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals("{}", readString(response));
        response.close();
    }

    /**
     * 验证拦截流式问答请求并输出包含思考与结构化扩展属性的原始 SSE 数据流。
     *
     * @throws IOException 当 IO 异常发生时抛出。
     */
    @Test
    void interceptsChatQueryAndStreamsRawSseEvents() throws IOException {
        final SimulatedCompanyModelHttpInterceptor interceptor = new SimulatedCompanyModelHttpInterceptor(0L);
        final HttpRequest request = createMockRequest("http://company-hiagent-service/chat_query");
        final ClientHttpRequestExecution execution = (req, body) -> {
            throw new AssertionError("Should not delegate to execution");
        };

        final ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_EVENT_STREAM, response.getHeaders().getContentType());

        final List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        }
        response.close();

        assertTrue(lines.size() > 5);
        assertTrue(lines.get(0).contains("message_start"));
        assertTrue(lines.stream().anyMatch(l -> l.contains("<think>")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("</think>")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("rewrite_query")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("doc_reference_json")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("sql_message")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("rec_questions")));
        assertTrue(lines.get(lines.size() - 1).contains("[DONE]"));
    }

    /**
     * 验证未知路径透传执行而不触发模拟拦截。
     *
     * @throws IOException 当 IO 异常发生时抛出。
     */
    @Test
    void passesThroughOtherRequests() throws IOException {
        final SimulatedCompanyModelHttpInterceptor interceptor = new SimulatedCompanyModelHttpInterceptor(0L);
        final HttpRequest request = createMockRequest("http://company-hiagent-service/other_endpoint");
        final boolean[] executed = {false};
        final ClientHttpRequestExecution execution = (req, body) -> {
            executed[0] = true;
            return null;
        };

        final ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertTrue(executed[0]);
    }

    /**
     * 验证模拟流式输入流的基本读取与逐帧解析逻辑。
     *
     * @throws IOException 当 IO 异常发生时抛出。
     */
    @Test
    void verifiesSimulatedSseInputStreamOperations() throws IOException {
        final List<String> frames = Arrays.asList("data: 1\n\n", "data: 2\n\n");
        final SimulatedSseInputStream stream = new SimulatedSseInputStream(frames, 0L);

        final byte[] buffer = new byte[64];
        final int readCount = stream.read(buffer, 0, buffer.length);
        assertTrue(readCount > 0);

        final String content = new String(buffer, 0, readCount, StandardCharsets.UTF_8);
        assertTrue(content.contains("data: 1"));

        stream.close();
        assertEquals(-1, stream.read());
    }

    /**
     * 从 HTTP 响应体读取全部字符串。
     *
     * @param response 客户端 HTTP 响应。
     * @return 响应体字符串。
     * @throws IOException 当读取流发生 IO 错误时抛出。
     */
    private static String readString(final ClientHttpResponse response) throws IOException {
        final StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    /**
     * 创建用于测试的模拟 HTTP 请求对象。
     *
     * @param uri 请求目标 URI 字符串。
     * @return 模拟 HTTP 请求对象。
     */
    private static HttpRequest createMockRequest(final String uri) {
        return new TestHttpRequest(URI.create(uri));
    }

    /**
     * 测试专用的模拟 HTTP 请求实现类。
     */
    private static final class TestHttpRequest implements HttpRequest {

        /**
         * 请求目标 URI。
         */
        private final URI uri;

        /**
         * 请求头集合。
         */
        private final HttpHeaders headers = new HttpHeaders();

        /**
         * 创建测试请求实例。
         *
         * @param uri 请求目标 URI。
         */
        TestHttpRequest(final URI uri) {
            this.uri = uri;
        }

        /**
         * 获取 HTTP 请求方法枚举。
         *
         * @return POST 请求方法枚举。
         */
        @Override
        public HttpMethod getMethod() {
            return HttpMethod.POST;
        }

        /**
         * 获取 HTTP 请求方法原生字符串。
         *
         * @return 方法字符串。
         */
        @Override
        public String getMethodValue() {
            return "POST";
        }

        /**
         * 获取请求目标 URI。
         *
         * @return 目标 URI 实例。
         */
        @Override
        public URI getURI() {
            return uri;
        }

        /**
         * 获取 HTTP 请求头。
         *
         * @return 请求头映射对象。
         */
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
