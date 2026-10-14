package com.acme.intelligentqa.infrastructure.companymodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * 验证 CompanyModelEventDecoder 的协议解析、标签过滤与错误事件捕获逻辑。
 */
class CompanyModelEventDecoderTest {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证普通文本增量正常推送且正确统计回答状态。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void emitsPlainTextAndTracksAnswerStatus() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final List<String> chunks = new ArrayList<>();

        decoder.accept("{\"Answer\":\"你好，\"}", chunks::add);
        decoder.accept("{\"Answer\":\"你好，世界！\"}", chunks::add);
        decoder.flush(chunks::add);

        assertTrue(decoder.hasAnswer());
        assertEquals(java.util.Arrays.asList("你好，", "世界！"), chunks);
    }

    /**
     * 验证小写 answer 字段同样受支持。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void supportsLowerCaseAnswerField() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final List<String> chunks = new ArrayList<>();

        decoder.accept("{\"answer\":\"小写字段正文\"}", chunks::add);
        decoder.flush(chunks::add);

        assertTrue(decoder.hasAnswer());
        assertEquals(java.util.Collections.singletonList("小写字段正文"), chunks);
    }

    /**
     * 验证嵌套在 JSON 字符串内的内层 answer 能被正确提取。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void extractsInnerAnswerFromNestedJsonString() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final List<String> chunks = new ArrayList<>();

        decoder.accept("{\"Answer\":\"{\\\"answer\\\":\\\"解构后的回答正文\\\"}\"}", chunks::add);
        decoder.flush(chunks::add);

        assertTrue(decoder.hasAnswer());
        assertEquals(java.util.Collections.singletonList("解构后的回答正文"), chunks);
    }

    /**
     * 验证深度思考标签跨分片时被静默过滤且不泄露思考内容。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void silentlyFiltersThinkTagsAcrossMultipleChunks() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final List<String> chunks = new ArrayList<>();

        decoder.accept("{\"Answer\":\"<thi\"}", chunks::add);
        decoder.accept("{\"Answer\":\"<think>正在\"}", chunks::add);
        decoder.accept("{\"Answer\":\"<think>正在思考业务背景</thi\"}", chunks::add);
        decoder.accept("{\"Answer\":\"<think>正在思考业务背景</think>根据数据分析，\"}", chunks::add);
        decoder.accept("{\"Answer\":\"<think>正在思考业务背景</think>根据数据分析，结果为合格。\"}", chunks::add);
        decoder.flush(chunks::add);

        assertTrue(decoder.hasAnswer());
        assertEquals(java.util.Arrays.asList("根据数据分析，", "结果为合格。"), chunks);
    }

    /**
     * 验证仅有思考标签而无正式正文时判定为无有效回答。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void reportsNoAnswerWhenOnlyThinkTagsPresent() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final List<String> chunks = new ArrayList<>();

        decoder.accept("{\"Answer\":\"<think>仅有思考内容</think>\"}", chunks::add);
        decoder.flush(chunks::add);

        assertFalse(decoder.hasAnswer());
        assertTrue(chunks.isEmpty());
    }

    /**
     * 验证 answer 结构标签自身被剥离而内部正文正常透传。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void stripsAnswerStructureTagsWhileEmittingContent() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final List<String> chunks = new ArrayList<>();

        decoder.accept("{\"Answer\":\"<answer>标签内部的正文内容</answer>\"}", chunks::add);
        decoder.flush(chunks::add);

        assertTrue(decoder.hasAnswer());
        assertEquals(java.util.Collections.singletonList("标签内部的正文内容"), chunks);
    }

    /**
     * 验证文本中包含普通小于号时不被误吞并正常输出。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void preservesOrdinaryLessThanComparisonText() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final List<String> chunks = new ArrayList<>();

        decoder.accept("{\"Answer\":\"如果 a < b 且 c > d 则符合要求\"}", chunks::add);
        decoder.flush(chunks::add);

        assertTrue(decoder.hasAnswer());
        assertEquals("如果 a < b 且 c > d 则符合要求", String.join("", chunks));
    }

    /**
     * 验证接收到模型错误事件时立即抛出不可用异常。
     */
    @Test
    void throwsDependencyUnavailableExceptionOnModelErrorEvents() {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);

        final DependencyUnavailableException agentErrorException = assertThrows(
                DependencyUnavailableException.class,
                () -> decoder.accept("{\"event\":\"agent_error\",\"error_msg\":\"调用智能体超时\"}", value -> { }));
        assertEquals("COMPANY_MODEL_AGENT_ERROR", agentErrorException.errorCode());
        assertTrue(agentErrorException.getMessage().contains("调用智能体超时"));

        final DependencyUnavailableException generalErrorException = assertThrows(
                DependencyUnavailableException.class,
                () -> decoder.accept("{\"event\":\"error\",\"message\":\"配额超限\"}", value -> { }));
        assertEquals("COMPANY_MODEL_AGENT_ERROR", generalErrorException.errorCode());
        assertTrue(generalErrorException.getMessage().contains("配额超限"));
    }

    /**
     * 验证能够从 message_start 早期事件捕获消息标识。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void capturesMessageIdFromEarlyMessageStartEvent() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final AtomicReference<String> capturedId = new AtomicReference<>();

        decoder.accept(
                "{\"event\":\"message_start\",\"id\":\"msg-start-001\",\"conversation_id\":\"conv-001\"}",
                value -> { },
                capturedId::set);

        assertEquals("msg-start-001", capturedId.get());
    }

    /**
     * 验证能够从通用 message 事件捕获 MessageID 字段。
     *
     * @throws IOException 当解析发生 IO 错误时抛出。
     */
    @Test
    void capturesMessageIdFromStandardMessageEvent() throws IOException {
        final CompanyModelEventDecoder decoder = new CompanyModelEventDecoder(objectMapper);
        final AtomicReference<String> capturedId = new AtomicReference<>();

        decoder.accept(
                "{\"event\":\"message\",\"MessageID\":\"msg-standard-002\",\"Answer\":\"内容\"}",
                value -> { },
                capturedId::set);

        assertEquals("msg-standard-002", capturedId.get());
    }
}
