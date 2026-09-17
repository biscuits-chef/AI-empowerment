package com.acme.intelligentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.application.service.AnswerCancellationService;
import com.acme.intelligentqa.application.service.QuestionAnswerService;
import com.acme.intelligentqa.config.RuntimeEnvironmentGuard;
import com.acme.intelligentqa.config.RuntimeEnvironmentProperties;
import java.util.HashSet;
import java.util.Set;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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
        "spring.sql.init.schema-locations=classpath:/db/mybatis-test-schema.sql",
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
     * Spring MVC 实际生效的请求映射注册表。
     */
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * MyBatis-Plus 会话工厂。
     */
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

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

    /**
     * 验证 MyBatis-Plus 的安全运行参数与 Mapper 映射语句已实际装配。
     */
    @Test
    void configuresMybatisPlusRuntimeAndMapperStatements() {
        final org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        assertTrue(configuration instanceof com.baomidou.mybatisplus.core.MybatisConfiguration);
        assertFalse(configuration.isMapUnderscoreToCamelCase());
        assertEquals(LocalCacheScope.STATEMENT, configuration.getLocalCacheScope());
        assertEquals(Integer.valueOf(100), configuration.getDefaultFetchSize());
        assertEquals(Integer.valueOf(5), configuration.getDefaultStatementTimeout());
        assertEquals(JdbcType.NULL, configuration.getJdbcTypeForNull());
        final MappedStatement answerInsert = configuration.getMappedStatement(
                "com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerMapper.insert");
        final MappedStatement eventInsert = configuration.getMappedStatement(
                "com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerEventMapper.insert");
        assertNotNull(answerInsert);
        assertNotNull(eventInsert);
    }

    /**
     * 验证所有面向前端的业务接口仅注册 GET 或 POST 方法。
     */
    @Test
    void restrictsFrontendBusinessApiToGetAndPost() {
        final Set<String> businessPaths = new HashSet<>();
        requestMappingHandlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackage().getName()
                    .startsWith("com.acme.intelligentqa.adapter.in.web")) {
                return;
            }
            final Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
            assertTrue(!methods.isEmpty(), "面向前端的接口必须显式声明 HTTP 方法: " + mapping);
            assertTrue(methods.stream().allMatch(this::isAllowedFrontendMethod),
                    "面向前端的接口只能使用 GET 或 POST: " + mapping);
            mapping.getPatternValues().forEach(path -> assertTrue(businessPaths.add(path),
                    "面向前端的接口路径必须全局唯一，不能依靠 HTTP 方法区分: " + path));
        });
        assertBusinessMapping("/api/v1/questions/submission", RequestMethod.POST);
        assertBusinessMapping("/api/v1/chats", RequestMethod.GET);
        assertBusinessMapping("/api/v1/chats/{chatId}/rename", RequestMethod.POST);
        assertBusinessMapping("/api/v1/chats/{chatId}/deletion", RequestMethod.POST);
        assertBusinessMapping("/api/v1/answers/{answerId}/feedback", RequestMethod.POST);
        assertNoBusinessMapping("/api/v1/chats/creation");
        assertNoBusinessMapping("/api/v1/chats/{chatId}/questions");
        assertNoBusinessMapping("/api/v1/chats/{chatId}/files/upload");
        assertNoBusinessMapping("/api/v1/chats/{chatId}/files");
        assertNoBusinessMapping("/api/v1/chats/{chatId}/files/{fileId}/deletion");
    }

    /**
     * 判断请求方法是否符合前端接口方法白名单。
     *
     * @param requestMethod Spring MVC 请求方法。
     * @return 方法为 GET 或 POST 时返回 true。
     */
    private boolean isAllowedFrontendMethod(final RequestMethod requestMethod) {
        return requestMethod == RequestMethod.GET || requestMethod == RequestMethod.POST;
    }

    /**
     * 断言指定一期范围外的业务路径没有注册。
     *
     * @param pathPattern 不应注册的业务路径模板。
     */
    private void assertNoBusinessMapping(final String pathPattern) {
        final boolean registered = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .anyMatch(pathPattern::equals);
        assertFalse(registered, "第一阶段不应注册文件上传接口: " + pathPattern);
    }

    /**
     * 断言指定业务路径注册了预期请求方法。
     *
     * @param pathPattern 业务路径模板。
     * @param requestMethod 预期请求方法。
     */
    private void assertBusinessMapping(final String pathPattern, final RequestMethod requestMethod) {
        final boolean mappingExists = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(mapping -> mapping.getPatternValues().contains(pathPattern)
                        && mapping.getMethodsCondition().getMethods().contains(requestMethod));
        assertTrue(mappingExists, "缺少业务接口映射: " + requestMethod + " " + pathPattern);
    }
}
