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
        assertEquals("sqlite", source.getProperty("app.datasource.type"));
        assertEquals("jdbc:sqlite:./data/intelligent_qa.db", source.getProperty("spring.datasource.url"));
        assertEquals("false", source.getProperty("spring.flyway.enabled"));
        assertEquals("always", source.getProperty("spring.sql.init.mode"));
    }

    /**
     * 验证开发环境通过开关切换到 MySQL 数据源。
     */
    @Test
    void developmentProfileSwitchesToMysqlViaDatasourceToggle() {
        final MockEnvironment environment = new MockEnvironment()
                .withProperty("DEV_DB_TYPE", "mysql")
                .withProperty("DEV_DB_URL", "jdbc:mysql://10.0.0.1:3306/qa_formal");
        environment.setActiveProfiles("dev");

        processor.postProcessEnvironment(environment, new SpringApplication());
        final PropertySource<?> source = environment.getPropertySources().get(
                XmlApplicationEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);

        assertEquals("mysql", source.getProperty("app.datasource.type"));
        assertEquals("jdbc:mysql://10.0.0.1:3306/qa_formal", source.getProperty("spring.datasource.url"));
        assertEquals("true", source.getProperty("spring.flyway.enabled"));
        assertEquals("never", source.getProperty("spring.sql.init.mode"));
    }

    /**
     * 验证开发环境通过 MySQL 地址前缀自动智能切换为 MySQL 模式。
     */
    @Test
    void developmentProfileAutoDetectsMysqlFromUrlPrefix() {
        final MockEnvironment environment = new MockEnvironment()
                .withProperty("DEV_DB_URL", "jdbc:mysql://localhost:3306/auto_qa");
        environment.setActiveProfiles("dev");

        processor.postProcessEnvironment(environment, new SpringApplication());
        final PropertySource<?> source = environment.getPropertySources().get(
                XmlApplicationEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);

        assertEquals("mysql", source.getProperty("app.datasource.type"));
        assertEquals("jdbc:mysql://localhost:3306/auto_qa", source.getProperty("spring.datasource.url"));
        assertEquals("true", source.getProperty("spring.flyway.enabled"));
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
     * 验证在开发环境激活 SQLite 时，能自动创建数据库文件的父级目录。
     */
    @Test
    void developmentProfileEnsuresSqliteParentDirectoryExists() {
        final String customDir = "target/auto-created-data-" + System.currentTimeMillis();
        final String sqliteUrl = "jdbc:sqlite:" + customDir + "/test.db?busy_timeout=3000";
        final MockEnvironment environment = new MockEnvironment()
                .withProperty("DEV_DB_URL", sqliteUrl);
        environment.setActiveProfiles("dev");

        final java.io.File dir = new java.io.File(customDir);
        org.junit.jupiter.api.Assertions.assertFalse(dir.exists());

        processor.postProcessEnvironment(environment, new SpringApplication());

        org.junit.jupiter.api.Assertions.assertTrue(dir.exists());
    }

    /**
     * 加载指定 XML 配置资源。
     *
     * @param profile 待加载的环境 Profile。
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
