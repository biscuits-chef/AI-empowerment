package com.acme.intelligentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 公司 HiAgent 模型接口的地址、开关和超时限制配置。
 */
@ConfigurationProperties("app.company-model")
@ConstructorBinding
public final class CompanyModelProperties {

    /**
     * 启用开关。
     */
    private final boolean enabled;
    /**
     * 真实流事件契约是否已经取得样例并通过契约测试。
     */
    private final boolean streamContractVerified;
    /**
     * 服务基础地址。
     */
    private final String baseUrl;
    /**
     * 连接超时时间（毫秒）。
     */
    private final int connectTimeoutMillis;
    /**
     * 读取超时时间（毫秒）。
     */
    private final int readTimeoutMillis;
    /**
     * 请求最大字符数。
     */
    private final int maxRequestCharacters;

    /**
     * 创建 {@code CompanyModelProperties} 实例。
     *
     * @param enabled 启用开关。
     *
     * @param streamContractVerified 真实流事件契约是否已经验证。
     *
     * @param baseUrl 服务基础地址。
     *
     * @param connectTimeoutMillis 连接超时时间（毫秒）。
     *
     * @param readTimeoutMillis 读取超时时间（毫秒）。
     *
     * @param maxRequestCharacters 请求最大字符数。
     */
    public CompanyModelProperties(
            final boolean enabled,
            final boolean streamContractVerified,
            final String baseUrl,
            final int connectTimeoutMillis,
            final int readTimeoutMillis,
            final int maxRequestCharacters) {
        validatePositiveLimits(connectTimeoutMillis, readTimeoutMillis, maxRequestCharacters);
        validateStreamContract(enabled, streamContractVerified);
        final String normalized = normalizeAndValidateBaseUrl(enabled, baseUrl);
        this.enabled = enabled;
        this.streamContractVerified = streamContractVerified;
        this.baseUrl = normalized;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.maxRequestCharacters = maxRequestCharacters;
    }

    /**
     * 返回启用开关。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    public boolean enabled() { return enabled; }
    /**
     * 返回真实流事件契约是否已经验证。
     *
     * @return 已取得真实样例并通过契约测试时返回 true。
     */
    public boolean streamContractVerified() { return streamContractVerified; }
    /**
     * 返回服务基础地址。
     *
     * @return 服务基础地址。
     */
    public String baseUrl() { return baseUrl; }
    /**
     * 返回连接超时时间（毫秒）。
     *
     * @return 连接超时时间（毫秒）。
     */
    public int connectTimeoutMillis() { return connectTimeoutMillis; }
    /**
     * 返回读取超时时间（毫秒）。
     *
     * @return 读取超时时间（毫秒）。
     */
    public int readTimeoutMillis() { return readTimeoutMillis; }
    /**
     * 返回请求最大字符数。
     *
     * @return 请求最大字符数。
     */
    public int maxRequestCharacters() { return maxRequestCharacters; }

    /**
     * 校验所有容量及超时限制为正数。
     *
     * @param connectTimeoutMillis 连接超时时间（毫秒）。
     *
     * @param readTimeoutMillis 读取超时时间（毫秒）。
     *
     * @param maxRequestCharacters 请求最大字符数。
     */
    private static void validatePositiveLimits(
            final int connectTimeoutMillis,
            final int readTimeoutMillis,
            final int maxRequestCharacters) {
        if (connectTimeoutMillis <= 0 || readTimeoutMillis <= 0 || maxRequestCharacters <= 0) {
            throw new IllegalArgumentException("company model limits must be positive");
        }
    }

    /**
     * 在启用真实模型前校验流事件契约已经由责任人确认。
     *
     * @param enabled 公司模型是否启用。
     * @param streamContractVerified 真实流事件契约是否已经验证。
     */
    private static void validateStreamContract(
            final boolean enabled,
            final boolean streamContractVerified) {
        if (enabled && !streamContractVerified) {
            throw new IllegalArgumentException(
                    "app.company-model.stream-contract-verified must be true when company model is enabled");
        }
    }

    /**
     * 规范化并校验公司模型服务地址。
     *
     * @param enabled 启用开关。
     *
     * @param baseUrl 服务基础地址。
     *
     * @return 规范化并校验公司模型服务地址。
     */
    private static String normalizeAndValidateBaseUrl(final boolean enabled, final String baseUrl) {
        final String normalized = baseUrl == null ? "" : removeTrailingSlash(baseUrl.trim());
        if (enabled && !(normalized.startsWith("http://") || normalized.startsWith("https://"))) {
            throw new IllegalArgumentException("app.company-model.base-url must be an HTTP(S) URL");
        }
        return normalized;
    }

    /**
     * 去除服务基础地址末尾的斜线。
     *
     * @param value 输入值。
     *
     * @return 去除服务基础地址末尾的斜线。
     */
    private static String removeTrailingSlash(final String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
