package com.acme.intelligentqa.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

/**
 * 服务启动就绪后自动打印所有生效的应用配置信息。
 */
@Component
public class StartupConfigurationLogger implements ApplicationRunner {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupConfigurationLogger.class);

    /**
     * 密码脱敏展示标记。
     */
    private static final String MASKED_VALUE = "******";

    /**
     * Spring 运行环境上下文。
     */
    private final ConfigurableEnvironment environment;

    /**
     * 创建配置打印器实例。
     *
     * @param environment Spring 运行环境上下文。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected environment is retained and not exposed")
    public StartupConfigurationLogger(final ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    /**
     * 在服务启动完成时输出当前环境与全部配置项。
     *
     * @param args 启动命令行参数。
     */
    @Override
    public void run(final ApplicationArguments args) {
        LOGGER.info("==================== [服务启动生效配置清单] ====================");
        LOGGER.info(">>> 激活环境 Profile: {}", Arrays.toString(environment.getActiveProfiles()));
        LOGGER.info(">>> 企业单点登录 (app.uias.enabled): {}", environment.getProperty("app.uias.enabled", "false"));

        final Map<String, String> properties = collectApplicationProperties();
        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            LOGGER.info("[配置] {} = {}", entry.getKey(), entry.getValue());
        }
        LOGGER.info("==================== [共加载 {} 项应用配置] ====================", properties.size());
    }

    /**
     * 汇总并排序所有非底层 JVM 的应用级配置属性。
     *
     * @return 按字典序排序的配置项键值映射。
     */
    private Map<String, String> collectApplicationProperties() {
        final Map<String, String> properties = new TreeMap<>();
        for (final PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource<?>) {
                final EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) propertySource;
                for (final String propertyName : enumerable.getPropertyNames()) {
                    if (isApplicationProperty(propertyName) && !properties.containsKey(propertyName)) {
                        final String rawValue = environment.getProperty(propertyName);
                        properties.put(propertyName, maskIfSensitive(propertyName, rawValue));
                    }
                }
            }
        }
        return properties;
    }

    /**
     * 判定是否属于业务与框架关心的应用配置，过滤底层 JVM 及系统环境属性。
     *
     * @param name 属性名称。
     * @return 属于应用配置返回 true，属于底层环境变量返回 false。
     */
    private boolean isApplicationProperty(final String name) {
        return !name.startsWith("java.")
                && !name.startsWith("sun.")
                && !name.startsWith("os.")
                && !name.startsWith("user.")
                && !name.startsWith("file.")
                && !name.startsWith("path.")
                && !name.startsWith("line.")
                && !name.startsWith("awt.")
                && !name.startsWith("jdk.");
    }

    /**
     * 对包含密钥或密码的敏感配置项执行掩码处理。
     *
     * @param name 属性键名。
     * @param value 属性原始值。
     * @return 脱敏后的文本。
     */
    private String maskIfSensitive(final String name, final String value) {
        if (value == null) {
            return "null";
        }
        final String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("password")
                || lower.contains("secret")
                || lower.contains("credentials")
                || lower.endsWith(".key")
                || lower.endsWith("-key")) {
            return MASKED_VALUE;
        }
        return value;
    }
}
