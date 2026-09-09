package com.acme.intelligentqa.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/**
 * 从用户问题转换得到的一期标准化业务语义查询。
 */
public final class BusinessSemanticQuery {

    /** 一期允许执行的标准业务查询操作。 */
    public enum Operation {
        /** 按已确认实体查询字段。 */
        ENTITY_LOOKUP,
        /** 按业务日期查询基准日或到期日产品列表。 */
        REFERENCE_DATE_LIST
    }

    /** 一期允许查询的业务实体。 */
    public enum Subject {
        /** 产品实体。 */
        PRODUCT,
        /** 交易实体。 */
        TRADE
    }

    /** 一期语义模型允许返回的标准字段。 */
    public enum Field {
        /** 产品代码。 */
        PRODUCT_CODE,
        /** 产品名称。 */
        PRODUCT_NAME,
        /** 产品经理。 */
        PRODUCT_MANAGER,
        /** 投资经理。 */
        INVESTMENT_MANAGER,
        /** 监管口径投资经理。 */
        REGULATORY_INVESTMENT_MANAGER,
        /** 产品部口径投资经理。 */
        DEPARTMENT_INVESTMENT_MANAGER,
        /** 产品形式。 */
        PRODUCT_FORM,
        /** 产品状态。 */
        PRODUCT_STATUS,
        /** 开始或成立日期。 */
        START_DATE,
        /** 结束或到期日期。 */
        END_DATE,
        /** 管理费率。 */
        MANAGEMENT_FEE_RATE,
        /** 产品分层。 */
        PRODUCT_TIER,
        /** 费率调整计划。 */
        FEE_ADJUSTMENT_PLAN,
        /** 交易流水号。 */
        TRADE_SERIAL_NUMBER,
        /** 交易员姓名。 */
        TRADER_NAME,
        /** 交易日期。 */
        TRADE_DATE,
        /** 产品到期日期。 */
        PRODUCT_MATURITY_DATE,
        /** 定开基准日期。 */
        PERIODIC_OPEN_BASE_DATE,
        /** 数据快照分区日期。 */
        SNAPSHOT_DATE
    }

    /** 查询操作。 */
    private final Operation operation;
    /** 查询主体。 */
    private final Subject subject;
    /** 已消歧的实体引用。 */
    private final String entityReference;
    /** 日期列表查询使用的业务日期。 */
    private final LocalDate businessDate;
    /** 标准化返回字段。 */
    private final Set<Field> fields;
    /** 请求结果上限。 */
    private final int limit;

    /**
     * 创建标准化业务语义查询。
     *
     * @param subject 查询主体。
     * @param entityReference 已消歧的实体引用。
     * @param fields 标准化返回字段。
     * @param limit 请求结果上限。
     */
    public BusinessSemanticQuery(
            final Subject subject,
            final String entityReference,
            final Set<Field> fields,
            final int limit) {
        this(Operation.ENTITY_LOOKUP, subject, entityReference, null, fields, limit);
    }

    /**
     * 创建标准化业务语义查询。
     *
     * @param operation 查询操作。
     * @param subject 查询主体。
     * @param entityReference 已消歧的实体引用；日期列表查询允许为空。
     * @param businessDate 日期列表查询使用的业务日期；实体查询允许为空。
     * @param fields 标准化返回字段。
     * @param limit 请求结果上限。
     */
    private BusinessSemanticQuery(
            final Operation operation,
            final Subject subject,
            final String entityReference,
            final LocalDate businessDate,
            final Set<Field> fields,
            final int limit) {
        this.operation = Objects.requireNonNull(operation, "operation must not be null");
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        validateEntityReference(operation, entityReference);
        validateBusinessDate(operation, businessDate);
        final Set<Field> copiedFields = copyAndValidateFields(fields);
        validateLimit(limit);
        this.entityReference = entityReference == null ? null : entityReference.trim();
        this.businessDate = businessDate;
        this.fields = Collections.unmodifiableSet(copiedFields);
        this.limit = limit;
    }

    /**
     * 校验实体查询必须携带非空实体引用。
     *
     * @param operation 查询操作。
     * @param entityReference 实体引用。
     */
    private static void validateEntityReference(
            final Operation operation,
            final String entityReference) {
        if (operation == Operation.ENTITY_LOOKUP
                && (entityReference == null || entityReference.trim().isEmpty())) {
            throw new IllegalArgumentException("entityReference must not be blank");
        }
    }

    /**
     * 校验日期列表查询必须携带业务日期。
     *
     * @param operation 查询操作。
     * @param businessDate 业务日期。
     */
    private static void validateBusinessDate(
            final Operation operation,
            final LocalDate businessDate) {
        if (operation == Operation.REFERENCE_DATE_LIST && businessDate == null) {
            throw new IllegalArgumentException("businessDate is required for reference date list");
        }
    }

    /**
     * 防御性复制并校验标准字段集合。
     *
     * @param fields 标准字段集合。
     * @return 防御性复制后的字段集合。
     */
    private static Set<Field> copyAndValidateFields(final Set<Field> fields) {
        final Set<Field> copiedFields = EnumSet.copyOf(
                Objects.requireNonNull(fields, "fields must not be null"));
        if (copiedFields.isEmpty()) {
            throw new IllegalArgumentException("fields must not be empty");
        }
        return copiedFields;
    }

    /**
     * 校验查询结果上限必须为正数。
     *
     * @param limit 查询结果上限。
     */
    private static void validateLimit(final int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }

    /**
     * 创建指定业务日期的基准日或到期日产品列表查询。
     *
     * @param businessDate 查询业务日期。
     * @param fields 标准化返回字段。
     * @param limit 请求结果上限。
     * @return 日期列表语义查询。
     */
    public static BusinessSemanticQuery referenceDateList(
            final LocalDate businessDate,
            final Set<Field> fields,
            final int limit) {
        return new BusinessSemanticQuery(
                Operation.REFERENCE_DATE_LIST, Subject.PRODUCT, null, businessDate, fields, limit);
    }

    /**
     * 返回查询操作。
     *
     * @return 查询操作。
     */
    public Operation operation() { return operation; }

    /**
     * 返回查询主体。
     *
     * @return 查询主体。
     */
    public Subject subject() { return subject; }

    /**
     * 返回已消歧的实体引用。
     *
     * @return 已消歧的实体引用。
     */
    public Optional<String> entityReference() { return Optional.ofNullable(entityReference); }

    /**
     * 返回日期列表查询使用的业务日期。
     *
     * @return 业务日期；非日期列表查询时为空。
     */
    public Optional<LocalDate> businessDate() { return Optional.ofNullable(businessDate); }

    /**
     * 返回标准化字段集合。
     *
     * @return 标准化字段集合。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Set is an unmodifiable defensive copy")
    public Set<Field> fields() { return fields; }

    /**
     * 返回请求结果上限。
     *
     * @return 请求结果上限。
     */
    public int limit() { return limit; }
}
