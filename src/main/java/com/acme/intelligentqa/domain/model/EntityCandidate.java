package com.acme.intelligentqa.domain.model;

import java.util.Objects;

/**
 * 实体消歧时可供用户选择的稳定引用和显示名称。
 */
public final class EntityCandidate {

    /**
     * 实体稳定引用。
     */
    private final String reference;
    /**
     * 显示名称。
     */
    private final String label;
    /**
     * 经授权目录返回的规范名称。
     */
    private final String canonicalName;

    /**
     * 创建 {@code EntityCandidate} 实例。
     *
     * @param reference 实体稳定引用。
     *
     * @param label 显示名称。
     */
    public EntityCandidate(final String reference, final String label) {
        this(reference, label, label);
    }

    /**
     * 创建带规范名称的 {@code EntityCandidate} 实例。
     *
     * @param reference 实体稳定引用。
     *
     * @param label 显示名称。
     *
     * @param canonicalName 经授权目录返回的规范名称。
     */
    public EntityCandidate(
            final String reference,
            final String label,
            final String canonicalName) {
        this.reference = requireText(reference, "reference");
        this.label = requireText(label, "label");
        this.canonicalName = requireText(canonicalName, "canonicalName");
    }

    /**
     * 返回实体稳定引用。
     *
     * @return 实体稳定引用。
     */
    public String reference() { return reference; }
    /**
     * 返回显示名称。
     *
     * @return 显示名称。
     */
    public String label() { return label; }
    /**
     * 返回经授权目录确认的规范名称。
     *
     * @return 经授权目录确认的规范名称。
     */
    public String canonicalName() { return canonicalName; }

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
