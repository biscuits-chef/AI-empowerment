package com.acme.intelligentqa.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 意图识别得到的查询类型、置信度、实体和待消歧候选。
 */
public final class QueryIntent {

    /** 第一阶段允许执行的受控查询类型。 */
    public enum Type {
        /**
         * 查询指定产品的两种投资经理口径。
         */
        PRODUCT_INVESTMENT_MANAGER_QUERY,
        /**
         * 查询指定产品的产品经理。
         */
        PRODUCT_MANAGER_QUERY,
        /**
         * 查询指定业务日期命中定开基准日或产品到期日的产品。
         */
        PRODUCT_REFERENCE_DATE_LIST_QUERY,
        /**
         * 产品及交易基础信息查询。
         */
        PRODUCT_TRADE_BASIC_INFO,
        /**
         * 最新产品文档及关键要素查询。
         */
        PRODUCT_LATEST_DOCUMENT_INFO,
        /**
         * 当前阶段不支持的查询意图。
         */
        UNSUPPORTED
    }

    /**
     * 类型。
     */
    private final Type type;
    /**
     * 置信度。
     */
    private final double confidence;
    /**
     * 结构化实体。
     */
    private final Map<String, String> entities;
    /**
     * 尚未确认的实体字段集合。
     */
    private final Set<String> unresolvedEntities;
    /**
     * 实体候选列表。
     */
    private final Map<String, List<EntityCandidate>> candidates;

    /**
     * 创建 {@code QueryIntent} 实例。
     *
     * @param type 类型。
     *
     * @param confidence 置信度。
     *
     * @param entities 结构化实体。
     */
    public QueryIntent(final Type type, final double confidence, final Map<String, String> entities) {
        this(type, confidence, entities, Collections.emptySet(), Collections.emptyMap());
    }

    /**
     * 创建 {@code QueryIntent} 实例。
     *
     * @param type 类型。
     *
     * @param confidence 置信度。
     *
     * @param entities 结构化实体。
     *
     * @param unresolvedEntities 尚未确认的实体字段集合。
     *
     * @param candidates 实体候选列表。
     */
    public QueryIntent(
            final Type type,
            final double confidence,
            final Map<String, String> entities,
            final Set<String> unresolvedEntities,
            final Map<String, List<EntityCandidate>> candidates) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (confidence < 0.0D || confidence > 1.0D) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        this.confidence = confidence;
        this.entities = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(entities, "entities must not be null")));
        this.unresolvedEntities = Collections.unmodifiableSet(new HashSet<>(
                Objects.requireNonNull(unresolvedEntities, "unresolvedEntities must not be null")));
        final Map<String, List<EntityCandidate>> candidateCopy = new LinkedHashMap<>();
        for (final Map.Entry<String, List<EntityCandidate>> entry : Objects.requireNonNull(
                candidates, "candidates must not be null").entrySet()) {
            candidateCopy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        this.candidates = Collections.unmodifiableMap(candidateCopy);
    }

    /**
     * 返回类型。
     *
     * @return 类型。
     */
    public Type type() { return type; }
    /**
     * 返回置信度。
     *
     * @return 置信度。
     */
    public double confidence() { return confidence; }

    /**
     * 返回结构化实体。
     *
     * @return 结构化实体。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Map is an unmodifiable defensive copy")
    public Map<String, String> entities() { return entities; }

    /**
     * 处理尚未确认的实体字段集合。
     *
     * @return 尚未确认的实体字段集合。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Set is an unmodifiable defensive copy")
    public Set<String> unresolvedEntities() { return unresolvedEntities; }

    /**
     * 返回实体候选列表。
     *
     * @return 实体候选列表。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Map and lists are defensive copies")
    public Map<String, List<EntityCandidate>> candidates() { return candidates; }

    /**
     * 使用已消歧实体创建新的不可变意图。
     *
     * @param resolvedEntities 已消歧的结构化实体。
     *
     * @return 使用已消歧实体创建新的不可变意图。
     */
    public QueryIntent withEntities(final Map<String, String> resolvedEntities) {
        return new QueryIntent(type, confidence, resolvedEntities, Collections.emptySet(), Collections.emptyMap());
    }
}
