package com.acme.intelligentqa.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.env.Environment;

/**
 * 在容器启动前校验环境 Profile 唯一性及生产安全约束。
 */
public final class RuntimeEnvironmentGuard {

    /**
     * 允许启用的环境 Profile 集合。
     */
    private static final Set<String> ENVIRONMENT_PROFILES = new HashSet<>(
            Arrays.asList("dev", "test", "prod"));
    /**
     * 当前环境 Profile。
     */
    private final String activeProfile;

    /**
     * 创建 {@code RuntimeEnvironmentGuard} 实例。
     *
     * @param properties 配置参数。
     *
     * @param authenticationProperties 认证配置。
     *
     * @param companyModelProperties 公司模型配置。
     *
     * @param cancellationProperties 停止任务配置。
     *
     * @param environment Spring 运行环境。
     */
    public RuntimeEnvironmentGuard(
            final RuntimeEnvironmentProperties properties,
            final AuthenticationProperties authenticationProperties,
            final CompanyModelProperties companyModelProperties,
            final CancellationProperties cancellationProperties,
            final Environment environment) {
        this.activeProfile = findSingleEnvironmentProfile(environment.getActiveProfiles());
        validateProfileConsistency(properties, activeProfile);
        validateProductionDemoMode(properties, environment);
        validateAuthenticationMode(properties, authenticationProperties);
        validateCancellationLease(companyModelProperties, cancellationProperties);
    }

    /**
     * 校验停止任务租约覆盖模型 HTTP 调用的最坏超时预算。
     *
     * @param companyModelProperties 公司模型配置。
     * @param cancellationProperties 停止任务配置。
     */
    private static void validateCancellationLease(
            final CompanyModelProperties companyModelProperties,
            final CancellationProperties cancellationProperties) {
        final long callTimeoutMillis = (long) companyModelProperties.connectTimeoutMillis()
                + companyModelProperties.readTimeoutMillis();
        if (companyModelProperties.enabled()
                && cancellationProperties.leaseMillis() <= callTimeoutMillis) {
            // 租约若先于远端调用超时，会允许另一副本重复执行停止请求。
            throw new IllegalStateException(
                    "cancellation lease must exceed company model HTTP timeout budget");
        }
    }

    /**
     * 校验运行阶段与启用的 Profile 是否一致。
     *
     * @param properties 运行环境配置。
     *
     * @param activeProfile 当前环境 Profile。
     */
    private static void validateProfileConsistency(
            final RuntimeEnvironmentProperties properties,
            final String activeProfile) {
        if (!properties.stage().profile().equals(activeProfile)
                || !properties.profile().equals(activeProfile)) {
            throw new IllegalStateException("environment profile does not match app.runtime configuration");
        }
    }

    /**
     * 校验生产环境不能开启演示回答。
     *
     * @param properties 运行环境配置。
     *
     * @param environment Spring 运行环境。
     */
    private static void validateProductionDemoMode(
            final RuntimeEnvironmentProperties properties,
            final Environment environment) {
        if (properties.stage() == RuntimeEnvironmentProperties.Stage.PRODUCTION
                && environment.getProperty("app.qa.demo-mode", Boolean.class, false)) {
            throw new IllegalStateException("demo mode must not be enabled in production");
        }
    }

    /**
     * 校验模拟认证只能在开发环境使用。
     *
     * @param properties 运行环境配置。
     *
     * @param authenticationProperties 认证配置。
     */
    private static void validateAuthenticationMode(
            final RuntimeEnvironmentProperties properties,
            final AuthenticationProperties authenticationProperties) {
        if (properties.stage() != RuntimeEnvironmentProperties.Stage.DEVELOPMENT
                && authenticationProperties.mode() == AuthenticationProperties.Mode.MOCK) {
            throw new IllegalStateException("mock authentication is only allowed in development");
        }
    }

    /**
     * 处理当前环境 Profile。
     *
     * @return 当前环境 Profile。
     */
    public String activeProfile() {
        return activeProfile;
    }

    /**
     * 识别唯一启用的运行环境 Profile。
     *
     * @param activeProfiles 已启用的环境 Profile 列表。
     *
     * @return 识别唯一启用的运行环境 Profile。
     */
    private static String findSingleEnvironmentProfile(final String[] activeProfiles) {
        String matchedProfile = null;
        for (final String profile : activeProfiles) {
            if (ENVIRONMENT_PROFILES.contains(profile)) {
                if (matchedProfile != null) {
                    throw new IllegalStateException("only one of dev, test or prod may be active");
                }
                matchedProfile = profile;
            }
        }
        if (matchedProfile == null) {
            throw new IllegalStateException("one of dev, test or prod must be active");
        }
        return matchedProfile;
    }
}
