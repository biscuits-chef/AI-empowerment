package com.acme.intelligentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 一期业务语义查询通道的启用状态、模型版本和结果上限配置。
 */
@ConfigurationProperties("app.business-query")
@ConstructorBinding
public final class BusinessQueryProperties {

    /** 是否启用真实业务查询通道。 */
    private final boolean enabled;
    /** 单次查询允许返回的最大记录数。 */
    private final int maximumRows;
    /** 已发布的业务语义模型版本。 */
    private final String semanticModelVersion;

    /**
     * 创建一期业务语义查询配置。
     *
     * @param enabled 是否启用真实业务查询通道。
     * @param maximumRows 单次查询允许返回的最大记录数。
     * @param semanticModelVersion 已发布的业务语义模型版本。
     */
    public BusinessQueryProperties(
            final boolean enabled,
            final int maximumRows,
            final String semanticModelVersion) {
        if (maximumRows <= 0 || maximumRows > 1000) {
            throw new IllegalArgumentException("app.business-query.maximum-rows must be between 1 and 1000");
        }
        if (semanticModelVersion == null || semanticModelVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("app.business-query.semantic-model-version must not be blank");
        }
        this.enabled = enabled;
        this.maximumRows = maximumRows;
        this.semanticModelVersion = semanticModelVersion;
    }

    /**
     * 返回是否启用真实业务查询通道。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    public boolean enabled() { return enabled; }

    /**
     * 返回单次查询允许返回的最大记录数。
     *
     * @return 单次查询允许返回的最大记录数。
     */
    public int maximumRows() { return maximumRows; }

    /**
     * 返回已发布的业务语义模型版本。
     *
     * @return 已发布的业务语义模型版本。
     */
    public String semanticModelVersion() { return semanticModelVersion; }
}
