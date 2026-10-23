package com.acme.intelligentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 定义用户账号密码认证的运行配置。
 */
@ConfigurationProperties("app.auth")
public final class AuthenticationProperties {

    /**
     * 默认用户登录账号。
     */
    public static final String DEFAULT_USERNAME = "admin";

    /**
     * 用户登录账号；未配置时默认为 admin。
     */
    private final String username;
    /**
     * 用户登录密码；未配置时由系统随机生成。
     */
    private final String password;
    /**
     * 静态密码向下兼容字段；未配置时为空字符串。
     */
    private final String adminPassword;

    /**
     * 创建默认认证配置实例。
     */
    public AuthenticationProperties() {
        this(null, null, null);
    }

    /**
     * 创建指定密码的认证配置实例（向下兼容）。
     *
     * @param password 用户登录密码。
     */
    public AuthenticationProperties(final String password) {
        this(null, password, password);
    }

    /**
     * 创建指定用户名和密码的认证配置实例。
     *
     * @param username 用户登录账号。
     * @param password 用户登录密码。
     */
    public AuthenticationProperties(final String username, final String password) {
        this(username, password, password);
    }

    /**
     * 创建认证配置完整实例。
     *
     * @param username 用户登录账号。
     * @param password 用户登录密码。
     * @param adminPassword 静态密码向下兼容字段。
     */
    @ConstructorBinding
    public AuthenticationProperties(
            final String username,
            final String password,
            final String adminPassword) {
        final String resolvedUser = optionalText(username);
        this.username = resolvedUser.isEmpty() ? DEFAULT_USERNAME : resolvedUser;
        final String resolvedPassword = optionalText(password);
        final String resolvedAdminPassword = optionalText(adminPassword);
        this.password = resolvedPassword.isEmpty() ? resolvedAdminPassword : resolvedPassword;
        this.adminPassword = resolvedAdminPassword.isEmpty() ? this.password : resolvedAdminPassword;
    }

    /**
     * 返回用户登录账号。
     *
     * @return 用户登录账号。
     */
    public String username() {
        return username;
    }

    /**
     * 返回用户登录密码；未配置时为空字符串。
     *
     * @return 用户登录密码。
     */
    public String password() {
        return password;
    }

    /**
     * 返回管理员静态密码向下兼容值；未配置时为空字符串。
     *
     * @return 管理员密码兼容值。
     */
    public String adminPassword() {
        return adminPassword;
    }

    /**
     * 将可选文本规范化为空字符串或去除首尾空白后的值。
     *
     * @param value 待规范化文本。
     * @return 规范化后的文本。
     */
    private static String optionalText(final String value) {
        return value == null ? "" : value.trim();
    }
}
