package com.acme.intelligentqa.infrastructure.companymodel;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 将 HiAgent SSE 数据帧解码为内部流事件，并对未知结构失败关闭。
 */
final class CompanyModelEventDecoder {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyModelEventDecoder.class);

    /**
     * 已由接口契约明确记录的模型回答字段名列表。
     */
    private static final List<String> ANSWER_FIELDS = Collections.unmodifiableList(
            Arrays.asList("Answer", "answer"));

    /**
     * 已由接口契约明确记录的模型消息 ID 字段名列表。
     */
    private static final List<String> MESSAGE_ID_FIELDS = Collections.unmodifiableList(
            Arrays.asList("MessageID", "message_id", "id"));

    /**
     * 模型错误事件中的错误详情字段名列表。
     */
    private static final List<String> ERROR_FIELDS = Collections.unmodifiableList(
            Arrays.asList("error_msg", "message", "error", "detail"));

    /**
     * 智能体错误事件名称。
     */
    private static final String EVENT_AGENT_ERROR = "agent_error";

    /**
     * 通用错误事件名称。
     */
    private static final String EVENT_ERROR = "error";

    /**
     * 深度思考开启标签。
     */
    private static final String TAG_THINK_OPEN = "<think>";

    /**
     * 深度思考结束标签。
     */
    private static final String TAG_THINK_CLOSE = "</think>";

    /**
     * 答案正文开启标签。
     */
    private static final String TAG_ANSWER_OPEN = "<answer>";

    /**
     * 答案正文结束标签。
     */
    private static final String TAG_ANSWER_CLOSE = "</answer>";

    /**
     * 支持的标签最大字符长度。
     */
    private static final int MAX_TAG_LENGTH = 10;

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 跨数据块的未决标签字符缓冲区。
     */
    private final StringBuilder tagBuffer = new StringBuilder();

    /**
     * 已累积的原始回答文本。
     */
    private String accumulated = "";

    /**
     * 当前流是否处于深度思考标签内部。
     */
    private boolean inThink = false;

    /**
     * 是否已成功向调用方推送过有效回答内容。
     */
    private boolean hasEmittedAnswer = false;

    /**
     * 已成功解析的有效事件总数。
     */
    private int eventCount = 0;

    /**
     * 最新捕获的模型消息 ID。
     */
    private String lastMessageId;

    /**
     * 已成功推送给调用方的回答总字符数。
     */
    private int totalAnswerCharacters = 0;

    /**
     * 创建 {@code CompanyModelEventDecoder} 实例。
     *
     * @param objectMapper JSON 对象映射器。
     */
    CompanyModelEventDecoder(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 接收并解析一个公司模型数据帧。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     * @param consumer 事件订阅回调。
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    void accept(final String data, final Consumer<String> consumer) throws IOException {
        accept(data, consumer, value -> { });
    }

    /**
     * 接收并解析一个公司模型数据帧。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     * @param consumer 事件订阅回调。
     * @param messageIdConsumer 公司模型消息 ID 回调。
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    void accept(
            final String data,
            final Consumer<String> consumer,
            final Consumer<String> messageIdConsumer) throws IOException {
        if (ignored(data)) {
            return;
        }
        eventCount++;
        final JsonNode root = objectMapper.readTree(data);
        logRawEvent(root, data);
        checkErrorEvent(root);
        notifyMessageId(root, messageIdConsumer);
        emitAnswer(root, consumer);
    }

    /**
     * 原始帧日志：跳过高频 message 帧，记录其他控制帧单行摘要以防日志刷屏。
     *
     * @param root JSON 根节点。
     * @param rawData 原始数据帧字符串。
     */
    private void logRawEvent(final JsonNode root, final String rawData) {
        final String event = findText(root, Collections.singletonList("event"));
        if (!"message".equalsIgnoreCase(event)) {
            LOGGER.info("[AGENT RAW] event={} data={}",
                    event != null ? event : "data",
                    CompanyModelLogText.abbreviate(rawData, 500));
        }
    }

    /**
     * 检查并处理模型返回的错误事件。
     *
     * @param root JSON 根节点。
     */
    private void checkErrorEvent(final JsonNode root) {
        final String event = findText(root, Collections.singletonList("event"));
        if (EVENT_AGENT_ERROR.equalsIgnoreCase(event) || EVENT_ERROR.equalsIgnoreCase(event)) {
            final String errorMsg = findText(root, ERROR_FIELDS);
            final String detail = errorMsg != null && !errorMsg.trim().isEmpty()
                    ? errorMsg.trim()
                    : "company model returned error event: " + event;
            LOGGER.warn("Company model returned error event: {}", detail);
            throw new DependencyUnavailableException("COMPANY_MODEL_AGENT_ERROR", detail);
        }
    }

    /**
     * 向停止协调逻辑通知公司模型消息 ID。
     *
     * @param root JSON 根节点。
     * @param messageIdConsumer 公司模型消息 ID 回调。
     */
    private void notifyMessageId(final JsonNode root, final Consumer<String> messageIdConsumer) {
        final String messageId = findText(root, MESSAGE_ID_FIELDS);
        if (messageId != null && !messageId.trim().isEmpty()) {
            this.lastMessageId = messageId.trim();
            messageIdConsumer.accept(this.lastMessageId);
        }
    }

    /**
     * 将模型回答增量发送给调用方。
     *
     * @param root JSON 根节点。
     * @param consumer 事件订阅回调。
     */
    private void emitAnswer(final JsonNode root, final Consumer<String> consumer) {
        final String rawAnswer = findText(root, ANSWER_FIELDS);
        if (rawAnswer == null || rawAnswer.isEmpty()) {
            return;
        }
        final String answer = extractInnerAnswer(rawAnswer);
        final String delta;
        if (answer.startsWith(accumulated)) {
            delta = answer.substring(accumulated.length());
            accumulated = answer;
        } else {
            delta = answer;
            accumulated += answer;
        }
        if (!delta.isEmpty()) {
            filterAndEmit(delta, consumer);
        }
    }

    /**
     * 从可能嵌套的 JSON 字符串中提取内层回答文本。
     *
     * @param rawAnswer 原始回答字符串。
     * @return 提取后的纯文本回答；无法解析为 JSON 时返回原值。
     */
    private String extractInnerAnswer(final String rawAnswer) {
        if (rawAnswer == null) {
            return null;
        }
        final String trimmed = rawAnswer.trim();
        if (!trimmed.startsWith("{")) {
            return rawAnswer;
        }
        try {
            final JsonNode innerNode = objectMapper.readTree(trimmed);
            final String innerAnswer = findText(innerNode, ANSWER_FIELDS);
            return innerAnswer != null ? innerAnswer : rawAnswer;
        } catch (final IOException ignored) {
            return rawAnswer;
        }
    }

    /**
     * 过滤增量文本中的思考与结构标签并推送正文。
     *
     * @param chunk 当前增量文本块。
     * @param consumer 事件订阅回调。
     */
    private void filterAndEmit(final String chunk, final Consumer<String> consumer) {
        tagBuffer.append(chunk);
        final String content = tagBuffer.toString();
        int index = 0;
        int consumed = 0;
        while (index < content.length()) {
            final int lt = content.indexOf('<', index);
            if (lt < 0) {
                emitFilteredText(content.substring(index), consumer);
                consumed = content.length();
                break;
            }
            emitFilteredText(content.substring(index, lt), consumer);
            final int nextIndex = resolveTag(content, lt, consumer);
            if (nextIndex < 0) {
                consumed = lt;
                break;
            }
            index = nextIndex;
            consumed = index;
        }
        tagBuffer.setLength(0);
        if (consumed < content.length()) {
            tagBuffer.append(content.substring(consumed));
        }
    }

    /**
     * 解析并处理当前小于号起始的标签或普通文本片段。
     *
     * @param content 缓冲区文本内容。
     * @param ltPos 小于号出现的位置索引。
     * @param consumer 事件订阅回调。
     * @return 下一个读取位置；若属于跨数据块未完结标签需等待下一块时返回负数。
     */
    private int resolveTag(final String content, final int ltPos, final Consumer<String> consumer) {
        final String rest = content.substring(ltPos);
        final int tagLength = matchTagLength(rest);
        if (tagLength > 0) {
            applyTag(rest.substring(0, tagLength));
            return ltPos + tagLength;
        }
        final int gt = content.indexOf('>', ltPos);
        if (gt < 0 && rest.length() <= MAX_TAG_LENGTH) {
            return -1;
        }
        emitFilteredText(content.substring(ltPos, ltPos + 1), consumer);
        return ltPos + 1;
    }

    /**
     * 按当前思考状态将非思考文本输出给回调。
     *
     * @param text 待输出文本。
     * @param consumer 事件订阅回调。
     */
    private void emitFilteredText(final String text, final Consumer<String> consumer) {
        if (text.isEmpty() || inThink) {
            return;
        }
        hasEmittedAnswer = true;
        totalAnswerCharacters += text.length();
        consumer.accept(text);
    }

    /**
     * 匹配支持的已知标签长度。
     *
     * @param candidate 候选标签字符串。
     * @return 匹配成功时返回标签字符长度，未匹配时返回 0。
     */
    private int matchTagLength(final String candidate) {
        if (candidate.startsWith(TAG_THINK_OPEN)) {
            return TAG_THINK_OPEN.length();
        }
        if (candidate.startsWith(TAG_THINK_CLOSE)) {
            return TAG_THINK_CLOSE.length();
        }
        if (candidate.startsWith(TAG_ANSWER_OPEN)) {
            return TAG_ANSWER_OPEN.length();
        }
        if (candidate.startsWith(TAG_ANSWER_CLOSE)) {
            return TAG_ANSWER_CLOSE.length();
        }
        return 0;
    }

    /**
     * 应用标签状态变更。
     *
     * @param tag 已匹配标签文本。
     */
    private void applyTag(final String tag) {
        if (TAG_THINK_OPEN.equals(tag)) {
            inThink = true;
        } else if (TAG_THINK_CLOSE.equals(tag)) {
            inThink = false;
        }
    }

    /**
     * 在流正常结束时冲刷缓冲区中剩余的文本内容。
     *
     * @param consumer 事件订阅回调。
     */
    void flush(final Consumer<String> consumer) {
        if (tagBuffer.length() > 0) {
            emitFilteredText(tagBuffer.toString(), consumer);
            tagBuffer.setLength(0);
        }
    }

    /**
     * 处理接口签名要求但当前逻辑不使用的参数。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean ignored(final String data) {
        return data == null || data.trim().isEmpty() || "[DONE]".equals(data.trim());
    }

    /**
     * 判断当前事件是否包含可用回答文本。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean hasAnswer() {
        return hasEmittedAnswer;
    }

    /**
     * 从根对象的精确字段路径提取文本。
     *
     * @param node 待读取的 JSON 或语法树节点。
     * @param fields 待查找字段列表。
     * @return 从兼容字段路径提取文本。
     */
    private String findText(final JsonNode node, final List<String> fields) {
        for (final String field : fields) {
            final JsonNode candidate = node == null ? null : node.get(field);
            if (candidate != null && candidate.isTextual()) {
                return candidate.textValue();
            }
        }
        return null;
    }

    /**
     * 获取已成功解析的有效事件总数。
     *
     * @return 已成功解析的有效事件总数。
     */
    int eventCount() {
        return eventCount;
    }

    /**
     * 获取最新捕获的模型消息 ID。
     *
     * @return 最新捕获的模型消息 ID；未捕获时为 null。
     */
    String lastMessageId() {
        return lastMessageId;
    }

    /**
     * 获取已成功推送给调用方的回答总字符数。
     *
     * @return 已成功推送给调用方的回答总字符数。
     */
    int totalAnswerCharacters() {
        return totalAnswerCharacters;
    }

}
