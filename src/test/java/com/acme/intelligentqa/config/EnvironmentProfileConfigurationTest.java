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
        final String expectedUrl = "${TEST_DB_URL:jdbc:mysql://21.37.78.195:8891/intelligent_qa"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC"
                + "&allowPublicKeyRetrieval=true&useSSL=false}";
        assertEquals(expectedUrl, source.getProperty("spring.datasource.url"));
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
                .withProperty("DEV_QA_DEMO_MODE", "false")
                .withProperty("DEV_LOG_PATH", "/custom/log/path");
        environment.setActiveProfiles("dev");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertEquals("false", environment.getProperty("app.qa.demo-mode"));
    }

    /**
     * 验证开发环境配置 DataHub 专属开发变量及公共调优参数。
     */
    @Test
    void developmentProfileConfiguresDevDataHubProperties() {
        final PropertySource<?> source = load("dev");

        assertEquals("${DEV_DATAHUB_ENABLED:false}",
                source.getProperty("datahub.config.enable"));
        assertEquals("${DEV_DATAHUB_APP_CODE:}", source.getProperty("datahub.config.app-code"));
        assertEquals("${DEV_DATAHUB_SERVER_NAME:${spring.application.name}}",
                source.getProperty("datahub.config.server-name"));
        assertEquals("${DEV_DATAHUB_API_KEY:}", source.getProperty("datahub.config.api-key"));
        assertEquals("${DEV_DATAHUB_CONF_URL:}", source.getProperty("datahub.config.conf-url"));
        assertEquals("${DEV_DATAHUB_CONF_KEY:}", source.getProperty("datahub.config.conf-key"));
        assertEquals("${DEV_DATAHUB_HEALTH_URL:}", source.getProperty("datahub.config.health-url"));
        assertEquals("${DEV_DATAHUB_OBS_ENDPOINT:}", source.getProperty("datahub.config.endpoint"));
        assertEquals("${DEV_DATAHUB_OBS_ACCESS_KEY:}", source.getProperty("datahub.config.access-key"));
        assertEquals("${DEV_DATAHUB_OBS_SECRET_KEY:}", source.getProperty("datahub.config.secret-key"));
        assertEquals("${DEV_DATAHUB_OBS_BUCKET_NAME:}", source.getProperty("datahub.config.bucket-name"));
        assertEquals("${DEV_DATAHUB_OBS_OBJECT_PATH:}", source.getProperty("datahub.config.object-path"));
        assertEquals("${DEV_DATAHUB_TEMP_DIRECTORY:/tmp/datahub/file}",
                source.getProperty("datahub.config.temp-directory"));

        assertEquals("${DATAHUB_CONNECT_TIMEOUT_SEC:5}",
                source.getProperty("datahub.config.connect-timeout-sec"));
        assertEquals("${DATAHUB_READ_TIMEOUT_SEC:5}",
                source.getProperty("datahub.config.read-timeout-sec"));
        assertEquals("${DATAHUB_INITIAL_DELAY:10}", source.getProperty("datahub.config.initial-delay"));
        assertEquals("${DATAHUB_PERIOD:30}", source.getProperty("datahub.config.period"));
        assertEquals("${DATAHUB_STACK_TRACE_LIMIT:5}",
                source.getProperty("datahub.config.stack-trace-limit"));
        assertEquals("${DATAHUB_NOTIFY_IF_EMPTY:N}",
                source.getProperty("datahub.config.notify-if-empty"));
        assertEquals("${DATAHUB_OBS_UPLOAD_TASK_NUM:4}",
                source.getProperty("datahub.config.obs-upload-conf.task-num"));
        assertEquals("${DATAHUB_OBS_UPLOAD_PART_SIZE:50}",
                source.getProperty("datahub.config.obs-upload-conf.part-size"));
        assertEquals("${DATAHUB_OBS_UPLOAD_LISTENER:false}",
                source.getProperty("datahub.config.obs-upload-conf.listener"));
        assertEquals("${DATAHUB_OBS_UPLOAD_ENABLE_CHECKPOINT:true}",
                source.getProperty("datahub.config.obs-upload-conf.enable-checkpoint"));
    }

    /**
     * 验证测试环境配置 DataHub 专属测试变量。
     */
    @Test
    void testProfileConfiguresTestDataHubProperties() {
        final PropertySource<?> source = load("test");

        assertEquals("${TEST_DATAHUB_ENABLED:false}",
                source.getProperty("datahub.config.enable"));
        assertEquals("${TEST_DATAHUB_APP_CODE:}", source.getProperty("datahub.config.app-code"));
        assertEquals("${TEST_DATAHUB_SERVER_NAME:${spring.application.name}}",
                source.getProperty("datahub.config.server-name"));
        assertEquals("${TEST_DATAHUB_CONF_URL:}", source.getProperty("datahub.config.conf-url"));
        assertEquals("${TEST_DATAHUB_TEMP_DIRECTORY:/tmp/datahub/file}",
                source.getProperty("datahub.config.temp-directory"));
    }

    /**
     * 验证生产环境配置 DataHub 专属生产变量且默认关闭。
     */
    @Test
    void productionProfileConfiguresProdDataHubProperties() {
        final PropertySource<?> source = load("prod");

        assertEquals("${PROD_DATAHUB_ENABLED:false}", source.getProperty("datahub.config.enable"));
        assertEquals("${PROD_DATAHUB_APP_CODE:}", source.getProperty("datahub.config.app-code"));
        assertEquals("${PROD_DATAHUB_SERVER_NAME:${spring.application.name}}",
                source.getProperty("datahub.config.server-name"));
        assertEquals("${PROD_DATAHUB_CONF_URL:}", source.getProperty("datahub.config.conf-url"));
        assertEquals("${PROD_DATAHUB_TEMP_DIRECTORY:/tmp/datahub/file}",
                source.getProperty("datahub.config.temp-directory"));
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
