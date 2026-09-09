package com.acme.intelligentqa.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 定义公司统一认证与开发模拟认证的运行配置。
 */
@ConfigurationProperties("app.auth")
@ConstructorBinding
public final class AuthenticationProperties {

    /**
     * 当前认证模式。
     */
    private final Mode mode;
    /**
     * 开发模拟用户的固定用户标识。
     */
    private final String mockUserId;

    /**
     * 创建认证配置。
     *
     * @param mode 当前认证模式。
     *
     * @param mockUserId 开发模拟用户的固定用户标识。
     */
    public AuthenticationProperties(final Mode mode, final String mockUserId) {
        this.mode = Objects.requireNonNull(mode, "app.auth.mode must be configured");
        this.mockUserId = mode == Mode.MOCK ? requireText(mockUserId) : optionalText(mockUserId);
    }

    /**
     * 返回当前认证模式。
     *
     * @return 当前认证模式。
     */
    public Mode mode() {
        return mode;
    }

    /**
     * 返回开发模拟用户的固定用户标识。
     *
     * @return 开发模拟用户的固定用户标识；公司认证模式下可能为空字符串。
     */
    public String mockUserId() {
        return mockUserId;
    }

    /**
     * 校验必填文本并返回去除首尾空白后的值。
     *
     * @param value 待校验文本。
     *
     * @return 去除首尾空白后的文本。
     */
    private static String requireText(final String value) {
        final String normalized = optionalText(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("app.auth.mock-user-id must be configured in mock mode");
        }
        return normalized;
    }

    /**
     * 将可选文本规范化为空字符串或去除首尾空白后的值。
     *
     * @param value 待规范化文本。
     *
     * @return 规范化后的文本。
     */
    private static String optionalText(final String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 系统支持的认证模式。
     */
    public enum Mode {
        /**
         * 公司统一认证模式。
         */
        CORPORATE,
        /**
         * 仅供开发环境使用的固定模拟用户模式。
         */
        MOCK
    }
}
