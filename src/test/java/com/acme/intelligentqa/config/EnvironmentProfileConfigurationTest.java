package com.acme.intelligentqa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

/**
 * 验证 EnvironmentProfileConfiguration 的业务行为与边界。
 */
class EnvironmentProfileConfigurationTest {

    /**
     * XML 环境配置后处理器。
     */
    private final XmlApplicationEnvironmentPostProcessor processor =
            new XmlApplicationEnvironmentPostProcessor();

    /**
     * 验证开发环境使用开发标识及本地默认配置。
     */
    @Test
    void developmentProfileUsesDevelopmentIdentityAndLocalDefaults() {
        final PropertySource<?> source = load("dev");

        assertEquals("intelligent-qa-audit-service", source.getProperty("spring.application.name"));
        assertEquals("DEVELOPMENT", source.getProperty("app.runtime.stage"));
        assertEquals("dev", source.getProperty("app.runtime.profile"));
        assertEquals("${DEV_AUTH_MODE:mock}", source.getProperty("app.auth.mode"));
        assertEquals("${DEV_MOCK_USER_ID:dev-user-001}", source.getProperty("app.auth.mock-user-id"));
        assertEquals("${DEV_QA_DEMO_MODE:true}", source.getProperty("app.qa.demo-mode"));
    }

    /**
     * 验证测试环境使用测试标识和专属变量。
     */
    @Test
    void testProfileUsesTestIdentityAndTestVariables() {
        final PropertySource<?> source = load("test");

        assertEquals("TEST", source.getProperty("app.runtime.stage"));
        assertEquals("test", source.getProperty("app.runtime.profile"));
        assertEquals("corporate", source.getProperty("app.auth.mode"));
        assertEquals("${TEST_DB_URL}", source.getProperty("spring.datasource.url"));
        assertEquals("${TEST_QA_DEMO_MODE:false}", source.getProperty("app.qa.demo-mode"));
    }

    /**
     * 验证生产环境没有默认凭据且禁用演示模式。
     */
    @Test
    void productionProfileHasNoCredentialDefaultsAndDisablesDemoMode() {
        final PropertySource<?> source = load("prod");

        assertEquals("PRODUCTION", source.getProperty("app.runtime.stage"));
        assertEquals("prod", source.getProperty("app.runtime.profile"));
        assertEquals("corporate", source.getProperty("app.auth.mode"));
        assertEquals("${PROD_DB_URL}", source.getProperty("spring.datasource.url"));
        assertEquals("${PROD_DB_USERNAME}", source.getProperty("spring.datasource.username"));
        assertEquals("${PROD_DB_PASSWORD}", source.getProperty("spring.datasource.password"));
        assertEquals("${PROD_FLYWAY_TARGET}", source.getProperty("spring.flyway.target"));
        assertEquals("false", source.getProperty("app.qa.demo-mode"));
    }

    /**
     * 验证外部环境变量优先于 XML 默认值。
     */
    @Test
    void externalVariablesOverrideXmlDefaults() {
        final MockEnvironment environment = new MockEnvironment()
                .withProperty("DEV_QA_DEMO_MODE", "false");
        environment.setActiveProfiles("dev");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertEquals("false", environment.getProperty("app.qa.demo-mode"));
    }

    /**
     * 加载指定 XML 配置资源。
     *
     * @param profile 待加载的环境 Profile。
     *
     * @return 加载指定 XML 配置资源。
     */
    private PropertySource<?> load(final String profile) {
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        processor.postProcessEnvironment(environment, new SpringApplication());
        return environment.getPropertySources().get(
                XmlApplicationEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);
    }
}
