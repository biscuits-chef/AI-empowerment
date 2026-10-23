package com.acme.intelligentqa.adapter.in.web.authentication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.intelligentqa.adapter.in.web.ApiExceptionHandler;
import com.acme.intelligentqa.config.AuthenticationProperties;
import com.acme.intelligentqa.config.UiasProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 验证认证控制器 AuthController 的账密校验、Session 生命周期及 UIAS 引导行为。
 */
class AuthControllerTest {

    /**
     * 测试用密码。
     */
    private static final String TEST_PASSWORD = "UserTestPassword123!";

    /**
     * 模拟 MVC 客户端。
     */
    private MockMvc mockMvc;

    /**
     * 初始化测试控制器与环境。
     */
    @BeforeEach
    void setUp() {
        final AuthenticationProperties authProps = new AuthenticationProperties("admin", TEST_PASSWORD);
        final UserPasswordManager passwordManager = new UserPasswordManager(authProps);
        final UiasProperties uiasProps = new UiasProperties(
                "https://uias.spdb.com/login",
                "SAMLResponse",
                "realm",
                "dc",
                "classpath:idp-test.cer",
                true,
                "/error",
                "UIASAuthFilter",
                1,
                "/*");
        final AuthController controller = new AuthController(passwordManager, uiasProps);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    /**
     * 验证使用正确的 admin 用户名和密码登录成功并建立会话。
     *
     * @throws Exception 当执行请求异常时抛出。
     */
    @Test
    void loginSucceedsWithUserCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.displayName").value("普通用户"))
                .andExpect(jsonPath("$.authType").value("LOCAL"));
    }

    /**
     * 验证密码错误时拒绝登录并返回标准 401 凭据错误响应。
     *
     * @throws Exception 当执行请求异常时抛出。
     */
    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"WrongPassword!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("用户名或密码错误"));
    }

    /**
     * 验证非法或未知账号拒绝登录。
     *
     * @throws Exception 当执行请求异常时抛出。
     */
    @Test
    void loginRejectsUnknownUsername() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown_hacker\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    /**
     * 验证获取当前用户在未登录时返回 401。
     *
     * @throws Exception 当执行请求异常时抛出。
     */
    @Test
    void meReturnsUnauthorizedWhenNoSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    /**
     * 验证已登录会话能够正确查询当前用户信息。
     *
     * @throws Exception 当执行请求异常时抛出。
     */
    @Test
    void meReturnsUserWhenSessionAuthenticated() throws Exception {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(UnifiedAuthenticationFilter.SESSION_AUTH_USER_KEY, "admin");

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.displayName").value("普通用户"))
                .andExpect(jsonPath("$.authType").value("LOCAL"));
    }

    /**
     * 验证注销成功并清除会话。
     *
     * @throws Exception 当执行请求异常时抛出。
     */
    @Test
    void logoutInvalidatesSession() throws Exception {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(UnifiedAuthenticationFilter.SESSION_AUTH_USER_KEY, "admin");

        mockMvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isNoContent());
    }

    /**
     * 验证获取 UIAS 跳转地址。
     *
     * @throws Exception 当执行请求异常时抛出。
     */
    @Test
    void returnsUiasLoginUrl() throws Exception {
        mockMvc.perform(get("/api/v1/auth/uias/login-url")
                        .header("Referer", "http://localhost:8080/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginUrl").isNotEmpty());
    }
}
