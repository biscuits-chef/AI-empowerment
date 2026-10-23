package com.acme.intelligentqa.adapter.in.web;

import com.acme.intelligentqa.common.error.AuthenticationRequiredException;
import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证全局异常处理器 ApiExceptionHandler 的安全映射与日志捕获。
 */
class ApiExceptionHandlerTest {

    /**
     * 模拟 MVC 请求客户端。
     */
    private MockMvc mockMvc;

    /**
     * 初始化每个测试使用的隔离环境。
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    /**
     * 验证持久化故障转换为 HTTP 503 且包含稳定错误码。
     *
     * @throws Exception 当请求执行失败时抛出。
     */
    @Test
    void mapsPersistenceOperationExceptionToServiceUnavailable() throws Exception {
        mockMvc.perform(get("/test/persistence-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("PERSISTENCE_UNAVAILABLE"))
                .andExpect(jsonPath("$.title").value("Dependency unavailable"))
                .andExpect(jsonPath("$.detail").value("服务暂时无法保存请求，请稍后重试"))
                .andExpect(jsonPath("$.instance").value("/test/persistence-error"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * 验证依赖故障转换为 HTTP 503 且保留业务错误码。
     *
     * @throws Exception 当请求执行失败时抛出。
     */
    @Test
    void mapsDependencyUnavailableExceptionToServiceUnavailable() throws Exception {
        mockMvc.perform(get("/test/dependency-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("LLM_TIMEOUT"))
                .andExpect(jsonPath("$.title").value("Dependency unavailable"))
                .andExpect(jsonPath("$.detail").value("必要依赖当前不可用，请稍后重试"))
                .andExpect(jsonPath("$.instance").value("/test/dependency-error"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    /**
     * 验证未预期系统异常转换为 HTTP 500。
     *
     * @throws Exception 当请求执行失败时抛出。
     */
    @Test
    void mapsUnexpectedExceptionToInternalServerError() throws Exception {
        mockMvc.perform(get("/test/unexpected-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.detail").value("服务内部处理异常，请稍后重试"))
                .andExpect(jsonPath("$.instance").value("/test/unexpected-error"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    /**
     * 验证未认证异常转换为 HTTP 401。
     *
     * @throws Exception 当请求执行失败时抛出。
     */
    @Test
    void mapsAuthenticationRequiredExceptionToUnauthorized() throws Exception {
        mockMvc.perform(get("/test/unauthorized").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.detail").value("请先完成公司统一认证"));
    }

    /**
     * 验证资源未找到异常转换为 HTTP 404。
     *
     * @throws Exception 当请求执行失败时抛出。
     */
    @Test
    void mapsResourceNotFoundExceptionToNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("资源不存在或当前用户无权访问"));
    }

    /**
     * 仅供测试异常映射使用的测试控制器。
     */
    @RestController
    static final class TestExceptionController {

        /**
         * 创建测试控制器。
         */
        TestExceptionController() {
        }

        /**
         * 抛出持久化异常。
         */
        @GetMapping("/test/persistence-error")
        void throwPersistenceError() {
            throw new PersistenceOperationException("database connection failed", new IllegalStateException("timeout"));
        }

        /**
         * 抛出依赖不可用异常。
         */
        @GetMapping("/test/dependency-error")
        void throwDependencyError() {
            throw new DependencyUnavailableException("LLM_TIMEOUT", "model call timed out");
        }

        /**
         * 抛出未预期的运行时异常。
         */
        @GetMapping("/test/unexpected-error")
        void throwUnexpectedError() {
            throw new NullPointerException("unexpected null");
        }

        /**
         * 抛出未认证异常。
         */
        @GetMapping("/test/unauthorized")
        void throwUnauthorized() {
            throw new AuthenticationRequiredException();
        }

        /**
         * 抛出资源未找到异常。
         */
        @GetMapping("/test/not-found")
        void throwNotFound() {
            throw new ResourceNotFoundException("chat-1");
        }
    }
}
