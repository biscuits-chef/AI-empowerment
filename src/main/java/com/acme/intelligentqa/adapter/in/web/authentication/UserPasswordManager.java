package com.acme.intelligentqa.adapter.in.web.authentication;

import com.acme.intelligentqa.config.AuthenticationProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 管理普通用户账号密码的配置、随机生成、安全比对及启动日志输出。
 */
@Component
public class UserPasswordManager {

    /**
     * 默认普通用户登录账号。
     */
    public static final String DEFAULT_USERNAME = "admin";

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPasswordManager.class);

    /**
     * 随机字符集（排除易混淆字符 0, O, 1, l, I 以提升可读性）。
     */
    private static final String PASSWORD_CHARS =
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz!@#$%^&*";

    /**
     * 默认生成的随机密码长度。
     */
    private static final int DEFAULT_PASSWORD_LENGTH = 16;

    /**
     * 生效的普通用户账号。
     */
    private final String username;

    /**
     * 生效的登录密码。
     */
    private final String password;

    /**
     * 是否为启动时系统随机生成。
     */
    private final boolean randomlyGenerated;

    /**
     * 创建用户密码管理器实例。
     *
     * @param properties 认证配置。
     */
    public UserPasswordManager(final AuthenticationProperties properties) {
        final String configuredUser = properties != null ? properties.username() : null;
        this.username = (configuredUser != null && !configuredUser.trim().isEmpty())
                ? configuredUser.trim() : DEFAULT_USERNAME;

        final String configuredPassword = properties != null ? properties.password() : null;
        if (configuredPassword != null && !configuredPassword.trim().isEmpty()) {
            this.password = configuredPassword.trim();
            this.randomlyGenerated = false;
        } else {
            this.password = generateSecurePassword();
            this.randomlyGenerated = true;
        }
    }

    /**
     * 在组件装配完成后向控制台打印普通用户凭据提示。
     */
    @PostConstruct
    public void printStartupNotice() {
        LOGGER.info("================================================================================");
        LOGGER.info("[系统安全认证] 普通用户账号已就绪:");
        LOGGER.info("  用户名: {}", this.username);
        LOGGER.info("  密  码: {} ({})",
                this.password,
                this.randomlyGenerated ? "启动随机生成，可通过环境变量 APP_AUTH_PASSWORD 指定" : "配置文件已指定");
        LOGGER.info("================================================================================");
    }

    /**
     * 校验给定的用户名和密码是否为合法用户凭据。
     *
     * @param inputUsername 待校验的用户名。
     * @param rawPassword 待校验的明文密码。
     * @return 校验通过返回 true；否则返回 false。
     */
    public boolean verify(final String inputUsername, final String rawPassword) {
        if (inputUsername == null || rawPassword == null) {
            return false;
        }
        final String trimmedUser = inputUsername.trim();
        if (trimmedUser.isEmpty() || !matchesUsername(trimmedUser)) {
            return false;
        }
        final byte[] expected = this.password.getBytes(StandardCharsets.UTF_8);
        final byte[] actual = rawPassword.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * 检查输入的用户名是否匹配配置的账号或默认 admin 账号。
     *
     * @param inputUser 待校验的用户名。
     * @return 匹配返回 true；否则返回 false。
     */
    private boolean matchesUsername(final String inputUser) {
        return this.username.equalsIgnoreCase(inputUser)
                || DEFAULT_USERNAME.equalsIgnoreCase(inputUser);
    }

    /**
     * 返回当前配置的主用户名。
     *
     * @return 用户名。
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * 返回当前生效的密码。
     *
     * @return 当前密码明文。
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * 返回当前密码是否为启动随机生成。
     *
     * @return 若为随机生成返回 true；若为配置指定返回 false。
     */
    public boolean isRandomlyGenerated() {
        return this.randomlyGenerated;
    }

    /**
     * 生成高强度密码字符串。
     *
     * @return 16位随机安全密码。
     */
    private static String generateSecurePassword() {
        final SecureRandom random = new SecureRandom();
        final StringBuilder builder = new StringBuilder(DEFAULT_PASSWORD_LENGTH);
        for (int i = 0; i < DEFAULT_PASSWORD_LENGTH; i++) {
            final int index = random.nextInt(PASSWORD_CHARS.length());
            builder.append(PASSWORD_CHARS.charAt(index));
        }
        return builder.toString();
    }
}
