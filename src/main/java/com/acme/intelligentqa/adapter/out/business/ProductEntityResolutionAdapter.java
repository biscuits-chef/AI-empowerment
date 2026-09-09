package com.acme.intelligentqa.adapter.out.business;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.BusinessQueryProperties;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import com.acme.intelligentqa.domain.port.out.ProductEntityResolutionPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * 使用产品主题表最新快照解析产品代码、名称、简称或全称。
 */
@Component
public final class ProductEntityResolutionAdapter implements ProductEntityResolutionPort {

    /** 业务日期使用的公司时区。 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    /** 查询能力配置。 */
    private final BusinessQueryProperties businessProperties;
    /** 问答容量与演示模式配置。 */
    private final QaProperties qaProperties;
    /** 产品主题表 MyBatis 映射器。 */
    private final BusinessQueryMapper queryMapper;
    /** 用于确定当前业务日的系统时钟。 */
    private final Clock clock;

    /**
     * 创建产品实体解析适配器。
     *
     * @param businessProperties 查询能力配置。
     * @param qaProperties 问答容量与演示模式配置。
     * @param queryMapper 产品主题表 MyBatis 映射器。
     * @param clock 用于确定当前业务日的系统时钟。
     */
    public ProductEntityResolutionAdapter(
            final BusinessQueryProperties businessProperties,
            final QaProperties qaProperties,
            final BusinessQueryMapper queryMapper,
            final Clock clock) {
        this.businessProperties = Objects.requireNonNull(
                businessProperties, "businessProperties must not be null");
        this.qaProperties = Objects.requireNonNull(qaProperties, "qaProperties must not be null");
        this.queryMapper = Objects.requireNonNull(queryMapper, "queryMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 在最新有效快照中解析产品实体。
     *
     * @param ownerId 用户所有者 ID，用于保持端口权限上下文完整。
     * @param productReference 用户输入的产品代码或名称引用。
     * @return 有界且去重的产品候选列表。
     */
    @Override
    public List<EntityCandidate> resolve(final String ownerId, final String productReference) {
        requireText(ownerId, "ownerId");
        final String reference = requireText(productReference, "productReference");
        if (!businessProperties.enabled()) {
            if (qaProperties.demoMode()) {
                return Collections.singletonList(new EntityCandidate(reference, reference));
            }
            throw new DependencyUnavailableException(
                    "BUSINESS_QUERY_UNCONFIGURED", "approved business query catalog is not configured");
        }
        try {
            final String businessDate = LocalDate.now(clock.withZone(BUSINESS_ZONE))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            final String partitionDate = queryMapper.selectLatestPartitionDate(businessDate);
            if (partitionDate == null || partitionDate.trim().isEmpty()) {
                return Collections.emptyList();
            }
            return candidates(queryMapper.selectProductCandidates(
                    reference, partitionDate, qaProperties.maxContextItems() + 1));
        } catch (final DataAccessException exception) {
            throw new DependencyUnavailableException(
                    "BUSINESS_QUERY_FAILED", "approved business entity resolution failed", exception);
        }
    }

    /**
     * 将物理查询记录转换为去重且有界的产品候选。
     *
     * @param rows 产品候选物理记录。
     * @return 去重后的产品候选列表。
     */
    private List<EntityCandidate> candidates(final List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        final Map<String, EntityCandidate> values = new LinkedHashMap<>();
        for (final Map<String, Object> row : rows) {
            final String productCode = textValue(row, "productCode");
            final String productName = textValue(row, "productName");
            if (productCode != null) {
                final String label = productName == null
                        ? productCode : productName + "（" + productCode + "）";
                values.put(productCode, new EntityCandidate(
                        productCode, label, productName == null ? productCode : productName));
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(values.values()));
    }

    /**
     * 兼容 JDBC 结果别名大小写并读取非空文本。
     *
     * @param row 查询结果行。
     * @param column 固定结果别名。
     * @return 非空文本，不存在时返回空值。
     */
    private String textValue(final Map<String, Object> row, final String column) {
        for (final Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(column) && entry.getValue() != null) {
                final String value = entry.getValue().toString().trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /**
     * 校验文本非空并返回规范化值。
     *
     * @param value 待校验文本。
     * @param field 字段名称。
     * @return 去除首尾空格后的文本。
     */
    private String requireText(final String value, final String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
