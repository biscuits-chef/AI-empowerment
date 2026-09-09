package com.acme.intelligentqa.adapter.in.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证开发环境固定模拟用户能够访问受认证保护的业务接口。
 */
@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:qa-dev-auth;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:/db/mybatis-plus-test-schema.sql",
        "management.health.redis.enabled=false",
        "app.qa.cancellation.scan-delay-millis=60000"
})
class DevelopmentMockAuthenticationIntegrationTest {

    /**
     * 随机端口 HTTP 测试客户端。
     */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 验证前端无需传递用户标识即可访问会话列表。
     */
    @Test
    void injectsConfiguredMockUserIntoBusinessRequest() {
        final ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/chats", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("{\"items\":[],\"nextCursor\":null,\"hasMore\":false}", response.getBody());
    }
}
