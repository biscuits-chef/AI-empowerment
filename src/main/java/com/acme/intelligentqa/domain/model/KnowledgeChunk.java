package com.acme.intelligentqa.domain.model;

import java.util.Objects;

/**
 * 公司知识库返回的授权知识片段及其来源信息。
 */
public final class KnowledgeChunk {

    /**
     * 来源唯一标识。
     */
    private final String sourceId;
    /**
     * 会话名称。
     */
    private final String title;
    /**
     * 内容。
     */
    private final String content;

    /**
     * 创建 {@code KnowledgeChunk} 实例。
     *
     * @param sourceId 来源唯一标识。
     *
     * @param title 会话名称。
     *
     * @param content 内容。
     */
    public KnowledgeChunk(final String sourceId, final String title, final String content) {
        this.sourceId = requireText(sourceId, "sourceId");
        this.title = requireText(title, "title");
        this.content = requireText(content, "content");
    }

    /**
     * 返回来源唯一标识。
     *
     * @return 来源唯一标识。
     */
    public String sourceId() { return sourceId; }
    /**
     * 返回会话名称。
     *
     * @return 会话名称。
     */
    public String title() { return title; }
    /**
     * 返回内容。
     *
     * @return 内容。
     */
    public String content() { return content; }

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
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
