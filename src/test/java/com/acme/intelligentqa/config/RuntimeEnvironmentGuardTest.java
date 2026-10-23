package com.acme.intelligentqa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * 验证 RuntimeEnvironmentGuard 的业务行为与边界。
 */
class RuntimeEnvironmentGuardTest {

    /**
     * 验证唯一且匹配的环境 Profile 可以启动。
     */
    @Test
    void acceptsSingleMatchingEnvironmentProfile() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        final RuntimeEnvironmentGuard guard = guard(
                RuntimeEnvironmentProperties.Stage.TEST,
                environment);

        assertEquals("test", guard.activeProfile());
    }

    /**
     * 验证未选择运行环境时拒绝启动。
     */
    @Test
    void rejectsMissingEnvironmentProfile() {
        final MockEnvironment environment = new MockEnvironment();

        assertThrows(IllegalStateException.class, () -> guard(
                RuntimeEnvironmentProperties.Stage.TEST, environment));
    }

    /**
     * 验证同时启用多个环境时拒绝启动。
     */
    @Test
    void rejectsMultipleEnvironmentProfiles() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "prod");

        assertThrows(IllegalStateException.class, () -> guard(
                RuntimeEnvironmentProperties.Stage.DEVELOPMENT, environment));
    }

    /**
     * 验证 Profile 与环境标识不一致时拒绝启动。
     */
    @Test
    void rejectsProfileAndStageMismatch() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> guard(
                RuntimeEnvironmentProperties.Stage.TEST, environment));
    }

    /**
     * 验证生产环境不能通过外部配置开启演示模式。
     */
    @Test
    void rejectsDemoModeOverrideInProduction() {
        final MockEnvironment environment = new MockEnvironment()
                .withProperty("app.qa.demo-mode", "true");
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> guard(
                RuntimeEnvironmentProperties.Stage.PRODUCTION, environment));
    }

    /**
     * 验证开发环境允许启动。
     */
    @Test
    void acceptsDevelopmentEnvironment() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        final RuntimeEnvironmentGuard guard = guard(
                RuntimeEnvironmentProperties.Stage.DEVELOPMENT, environment);

        assertEquals("dev", guard.activeProfile());
    }

    /**
     * 验证模型 HTTP 超时预算超过停止租约时拒绝启动。
     */
    @Test
    void rejectsCancellationLeaseShorterThanModelTimeoutBudget() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        final CompanyModelProperties companyModel = new CompanyModelProperties(
                true, true, "https://model.example.internal/api/v1", 3000, 120000, 10000);
        final CancellationProperties cancellation = new CancellationProperties(
                1000, 10000, 2000, 30000, 3, 10);

        assertThrows(IllegalStateException.class, () -> new RuntimeEnvironmentGuard(
                properties(RuntimeEnvironmentProperties.Stage.PRODUCTION),
                companyModel, cancellation, environment));
    }

    /**
     * 创建具备安全默认依赖配置的运行环境保护器。
     *
     * @param stage 运行环境标识。
     * @param environment Spring 运行环境。
     * @return 已完成校验的运行环境保护器。
     */
    private RuntimeEnvironmentGuard guard(
            final RuntimeEnvironmentProperties.Stage stage,
            final MockEnvironment environment) {
        return new RuntimeEnvironmentGuard(
                properties(stage),
                new CompanyModelProperties(false, false, "", 3000, 120000, 10000),
                new CancellationProperties(1000, 130000, 2000, 30000, 3, 10),
                environment);
    }

    /**
     * 处理配置参数。
     *
     * @param stage 运行环境标识。
     * @return 配置参数。
     */
    private RuntimeEnvironmentProperties properties(final RuntimeEnvironmentProperties.Stage stage) {
        return new RuntimeEnvironmentProperties(stage, stage.profile());
    }
}
