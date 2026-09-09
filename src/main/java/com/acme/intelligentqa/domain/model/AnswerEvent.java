package com.acme.intelligentqa.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 回答生成过程中的有序领域事件，用于 SSE 推送与断线重放。
 */
public final class AnswerEvent {

    /**
     * 事件序号。
     */
    private final long sequence;
    /**
     * 类型。
     */
    private final String type;
    /**
     * 一个 SSE 事件的多行数据内容。
     */
    private final String data;
    /**
     * 事件发生时间。
     */
    private final Instant occurredAt;

    /**
     * 创建 {@code AnswerEvent} 实例。
     *
     * @param sequence 事件序号。
     *
     * @param type 类型。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     *
     * @param occurredAt 事件发生时间。
     */
    public AnswerEvent(final long sequence, final String type, final String data, final Instant occurredAt) {
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        this.sequence = sequence;
        this.type = requireText(type, "type");
        this.data = Objects.requireNonNull(data, "data must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    /**
     * 返回事件序号。
     *
     * @return 事件序号。
     */
    public long sequence() { return sequence; }
    /**
     * 返回类型。
     *
     * @return 类型。
     */
    public String type() { return type; }
    /**
     * 处理一个 SSE 事件的多行数据内容。
     *
     * @return 一个 SSE 事件的多行数据内容。
     */
    public String data() { return data; }
    /**
     * 返回事件发生时间。
     *
     * @return 事件发生时间。
     */
    public Instant occurredAt() { return occurredAt; }

    /**
     * 校验文本非空并返回原值。
     *
     * @param value 输入值。
     *
     * @param field 字段名称。
     *
     * @return 校验文本非空并返回原值。
     */
    private static String requireText(final String value, final String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
