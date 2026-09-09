package com.acme.intelligentqa.domain.model;

import java.util.Objects;

/**
 * 受控业务数据库查询返回的可追溯事实。
 */
public final class BusinessFact {

    /**
     * 证据来源编码。
     */
    private final String sourceCode;
    /**
     * 内容。
     */
    private final String content;

    /**
     * 创建 {@code BusinessFact} 实例。
     *
     * @param sourceCode 证据来源编码。
     *
     * @param content 内容。
     */
    public BusinessFact(final String sourceCode, final String content) {
        this.sourceCode = requireText(sourceCode, "sourceCode");
        this.content = requireText(content, "content");
    }

    /**
     * 处理证据来源编码。
     *
     * @return 证据来源编码。
     */
    public String sourceCode() { return sourceCode; }
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
