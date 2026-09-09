package com.acme.intelligentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.application.service.AnswerCancellationService;
import com.acme.intelligentqa.application.service.QuestionAnswerService;
import com.acme.intelligentqa.config.RuntimeEnvironmentGuard;
import com.acme.intelligentqa.config.RuntimeEnvironmentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证 IntelligentQaApplication 的业务行为与边界。
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:qa-context;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:/db/mybatis-plus-test-schema.sql",
        "app.qa.demo-mode=true",
        "app.qa.cancellation.scan-delay-millis=60000"
})
class IntelligentQaApplicationTest {

    /**
     * 被测问答编排服务。
     */
    @Autowired
    private QuestionAnswerService questionAnswerService;

    /**
     * 被测停止服务。
     */
    @Autowired
    private AnswerCancellationService answerCancellationService;

    /**
     * 内嵌 Web 服务器工厂。
     */
    @Autowired
    private ServletWebServerFactory webServerFactory;

    /**
     * 运行环境配置。
     */
    @Autowired
    private RuntimeEnvironmentProperties runtimeEnvironmentProperties;

    /**
     * 装配并执行运行环境保护校验。
     */
    @Autowired
    private RuntimeEnvironmentGuard runtimeEnvironmentGuard;

    /**
     * HTTP 客户端。
     */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 验证完整应用上下文使用 Jetty 成功启动。
     */
    @Test
    void startsCompleteApplicationContextWithJetty() {
        assertNotNull(questionAnswerService);
        assertNotNull(answerCancellationService);
        assertEquals(JettyServletWebServerFactory.class, webServerFactory.getClass());
        assertEquals(RuntimeEnvironmentProperties.Stage.TEST, runtimeEnvironmentProperties.stage());
        assertEquals("test", runtimeEnvironmentProperties.profile());
        assertEquals("test", runtimeEnvironmentGuard.activeProfile());
        final ResponseEntity<String> info = restTemplate.getForEntity("/actuator/info", String.class);
        assertTrue(info.getStatusCode().is2xxSuccessful());
        final String responseBody = info.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("\"environment\":\"TEST\""));
        assertTrue(responseBody.contains("\"active-profile\":\"test\""));
    }

    /**
     * 验证测试环境不会注入开发模拟用户。
     */
    @Test
    void keepsBusinessApiUnauthenticatedWithoutCorporatePrincipal() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/chats", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
