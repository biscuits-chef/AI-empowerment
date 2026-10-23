package com.acme.intelligentqa.adapter.in.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证统一身份认证与会话管理集成行为（未登录拦截 401、账密登录、会话保持与注销）。
 */
@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:qa-unified-auth;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:/db/mybatis-test-schema.sql",
        "management.health.redis.enabled=false",
        "app.qa.cancellation.scan-delay-millis=60000"
})
class UnifiedAuthenticationIntegrationTest {

    /**
     * 随机端口 HTTP 测试客户端。
     */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 普通用户密码管理器。
     */
    @Autowired
    private UserPasswordManager userPasswordManager;

    /**
     * 验证未登录时访问受保护业务接口统一返回 401 未认证。
     */
    @Test
    void rejectsUnauthenticatedAccessWithUnauthorizedStatus() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/chats", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("AUTHENTICATION_REQUIRED")
                || response.getBody().contains("401"));
    }

    /**
     * 验证使用普通用户名与密码登录成功后建立会话，可正常访问自身信息与业务接口。
     */
    @Test
    void authenticatesUserWithUsernameAndPasswordAndPreservesSession() {
        final String username = userPasswordManager.getUsername();
        final String password = userPasswordManager.getPassword();

        final AuthController.LoginRequest loginRequest = new AuthController.LoginRequest(username, password);
        final ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, String.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().contains("\"authenticated\":true"));
        assertTrue(loginResponse.getBody().contains("\"authType\":\"LOCAL\""));
        assertTrue(loginResponse.getBody().contains("\"displayName\":\"普通用户\""));

        final String cookie = loginResponse.getHeaders().getFirst("Set-Cookie");
        assertNotNull(cookie);

        final HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", cookie);
        final HttpEntity<Void> authRequest = new HttpEntity<>(headers);

        final ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET, authRequest, String.class);
        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        assertNotNull(meResponse.getBody());
        assertTrue(meResponse.getBody().contains("\"username\":\"" + username + "\""));
        assertTrue(meResponse.getBody().contains("\"authType\":\"LOCAL\""));

        final ResponseEntity<String> chatsResponse = restTemplate.exchange(
                "/api/v1/chats", HttpMethod.GET, authRequest, String.class);
        assertEquals(HttpStatus.OK, chatsResponse.getStatusCode());
    }

    /**
     * 验证错误的密码无法登录并返回 401。
     */
    @Test
    void rejectsInvalidCredentialsWithUnauthorizedStatus() {
        final AuthController.LoginRequest badRequest = new AuthController.LoginRequest("admin", "wrong-password-123");
        final ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", badRequest, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
