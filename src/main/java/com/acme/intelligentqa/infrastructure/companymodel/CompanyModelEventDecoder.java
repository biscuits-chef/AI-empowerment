package com.acme.intelligentqa.infrastructure.companymodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 将 HiAgent SSE 数据帧解码为内部流事件，并对未知结构失败关闭。
 */
final class CompanyModelEventDecoder {

    /**
     * 已由当前文档明确记录的模型回答字段名列表。
     */
    private static final List<String> ANSWER_FIELDS = Collections.singletonList("Answer");
    /**
     * 已由当前文档明确记录的模型消息 ID 字段名列表。
     */
    private static final List<String> MESSAGE_ID_FIELDS = Collections.singletonList("MessageID");
    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;
    /**
     * 已累积的回答文本。
     */
    private String accumulated = "";

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
     *
     * @param consumer 事件订阅回调。
     *
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    void accept(final String data, final Consumer<String> consumer) throws IOException {
        accept(data, consumer, value -> { });
    }

    /**
     * 接收并解析一个公司模型数据帧。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     *
     * @param consumer 事件订阅回调。
     *
     * @param messageIdConsumer 公司模型消息 ID 回调。
     *
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    void accept(
            final String data,
            final Consumer<String> consumer,
            final Consumer<String> messageIdConsumer) throws IOException {
        if (ignored(data)) {
            return;
        }
        final JsonNode root = objectMapper.readTree(data);
        notifyMessageId(root, messageIdConsumer);
        emitAnswer(root, consumer);
    }

    /**
     * 向停止协调逻辑通知公司模型消息 ID。
     *
     * @param root JSON 根节点。
     *
     * @param messageIdConsumer 公司模型消息 ID 回调。
     */
    private void notifyMessageId(final JsonNode root, final Consumer<String> messageIdConsumer) {
        final String messageId = findText(root, MESSAGE_ID_FIELDS);
        if (messageId != null && !messageId.trim().isEmpty()) {
            messageIdConsumer.accept(messageId);
        }
    }

    /**
     * 将模型回答增量发送给调用方。
     *
     * @param root JSON 根节点。
     *
     * @param consumer 事件订阅回调。
     *
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    private void emitAnswer(final JsonNode root, final Consumer<String> consumer) throws IOException {
        final String answer = findText(root, ANSWER_FIELDS);
        if (answer == null || answer.isEmpty()) {
            return;
        }
        final String delta;
        if (answer.startsWith(accumulated)) {
            delta = answer.substring(accumulated.length());
            accumulated = answer;
        } else {
            delta = answer;
            accumulated += answer;
        }
        if (!delta.isEmpty()) {
            consumer.accept(delta);
        }
    }

    /**
     * 处理接口签名要求但当前逻辑不使用的参数。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     *
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
        return !accumulated.isEmpty();
    }

    /**
     * 从根对象的精确字段路径提取文本。
     *
     * @param node 待读取的 JSON 或语法树节点。
     *
     * @param fields 待查找字段列表。
     *
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

}
