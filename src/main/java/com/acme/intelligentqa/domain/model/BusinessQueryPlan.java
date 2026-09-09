package com.acme.intelligentqa.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 经过语义目录和一期关系约束校验的确定性逻辑查询计划。
 */
public final class BusinessQueryPlan {

    /** 一期批准的逻辑查询计划类型。 */
    public enum Type {
        /** 按产品代码或名称查询产品事实。 */
        PRODUCT_LOOKUP,
        /** 按交易流水号查询交易事实。 */
        TRADE_LOOKUP,
        /** 按业务日期查询定开基准日或产品到期日产品。 */
        PRODUCT_REFERENCE_DATE_LIST
    }

    /** 计划类型。 */
    private final Type type;
    /** 计划唯一标识。 */
    private final String planId;
    /** 语义模型版本。 */
    private final String semanticModelVersion;
    /** 已消歧的实体引用。 */
    private final String entityReference;
    /** 日期列表查询使用的业务日期。 */
    private final LocalDate businessDate;
    /** 已批准的返回字段。 */
    private final Set<BusinessSemanticQuery.Field> fields;
    /** 结果数量上限。 */
    private final int limit;

    /**
     * 创建确定性逻辑查询计划。
     *
     * @param type 计划类型。
     * @param planId 计划唯一标识。
     * @param semanticModelVersion 语义模型版本。
     * @param entityReference 已消歧的实体引用。
     * @param fields 已批准的返回字段。
     * @param limit 结果数量上限。
     */
    public BusinessQueryPlan(
            final Type type,
            final String planId,
            final String semanticModelVersion,
            final String entityReference,
            final Set<BusinessSemanticQuery.Field> fields,
            final int limit) {
        this(type, planId, semanticModelVersion, entityReference, null, fields, limit);
    }

    /**
     * 创建确定性逻辑查询计划。
     *
     * @param type 计划类型。
     * @param planId 计划唯一标识。
     * @param semanticModelVersion 语义模型版本。
     * @param entityReference 已消歧的实体引用；日期列表查询允许为空。
     * @param businessDate 日期列表查询使用的业务日期；实体查询允许为空。
     * @param fields 已批准的返回字段。
     * @param limit 结果数量上限。
     */
    public BusinessQueryPlan(
            final Type type,
            final String planId,
            final String semanticModelVersion,
            final String entityReference,
            final LocalDate businessDate,
            final Set<BusinessSemanticQuery.Field> fields,
            final int limit) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.planId = requireText(planId, "planId");
        this.semanticModelVersion = requireText(semanticModelVersion, "semanticModelVersion");
        if (type != Type.PRODUCT_REFERENCE_DATE_LIST) {
            this.entityReference = requireText(entityReference, "entityReference");
        } else {
            this.entityReference = entityReference;
        }
        if (type == Type.PRODUCT_REFERENCE_DATE_LIST && businessDate == null) {
            throw new IllegalArgumentException("businessDate is required for reference date list");
        }
        this.businessDate = businessDate;
        this.fields = Collections.unmodifiableSet(EnumSet.copyOf(
                Objects.requireNonNull(fields, "fields must not be null")));
        if (this.fields.isEmpty()) {
            throw new IllegalArgumentException("fields must not be empty");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
    }

    /**
     * 返回计划类型。
     *
     * @return 计划类型。
     */
    public Type type() { return type; }

    /**
     * 返回计划唯一标识。
     *
     * @return 计划唯一标识。
     */
    public String planId() { return planId; }

    /**
     * 返回语义模型版本。
     *
     * @return 语义模型版本。
     */
    public String semanticModelVersion() { return semanticModelVersion; }

    /**
     * 返回已消歧的实体引用。
     *
     * @return 已消歧的实体引用。
     */
    public Optional<String> entityReference() { return Optional.ofNullable(entityReference); }

    /**
     * 返回日期列表查询使用的业务日期。
     *
     * @return 业务日期；非日期列表计划时为空。
     */
    public Optional<LocalDate> businessDate() { return Optional.ofNullable(businessDate); }

    /**
     * 返回已批准字段集合。
     *
     * @return 已批准字段集合。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Set is an unmodifiable defensive copy")
    public Set<BusinessSemanticQuery.Field> fields() { return fields; }

    /**
     * 返回结果数量上限。
     *
     * @return 结果数量上限。
     */
    public int limit() { return limit; }

    /**
     * 校验文本非空并返回原值。
     *
     * @param value 输入值。
     * @param field 字段名称。
     * @return 校验后的文本。
     */
    private static String requireText(final String value, final String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
