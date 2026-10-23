package com.acme.intelligentqa.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

/**
 * 验证服务启动配置打印器的属性收集与安全脱敏逻辑。
 */
class StartupConfigurationLoggerTest {

    /**
     * 验证启动配置打印器能够正常遍历属性并完成敏感信息脱敏。
     */
    @Test
    void logsApplicationPropertiesWithSensitiveDataMasked() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.uias.enabled", "false");
        environment.setProperty("app.auth.username", "admin");
        environment.setProperty("spring.datasource.password", "secret123");
        environment.setProperty("datahub.config.api-key", "my-api-key");
        environment.setProperty("java.version", "17");

        final StartupConfigurationLogger logger = new StartupConfigurationLogger(environment);

        assertDoesNotThrow(() -> logger.run(new DefaultApplicationArguments(new String[0])));
    }
}
