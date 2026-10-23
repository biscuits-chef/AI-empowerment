package com.acme.intelligentqa.adapter.in.web.authentication;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证统一身份认证过滤器 UnifiedAuthenticationFilter 的 Principal 注入行为。
 */
class UnifiedAuthenticationFilterTest {

    /**
     * 验证无会话时保持未认证状态。
     *
     * @throws ServletException Servlet 异常。
     * @throws IOException IO 异常。
     */
    @Test
    void leavesPrincipalNullWhenNoSession() throws ServletException, IOException {
        final UnifiedAuthenticationFilter filter = new UnifiedAuthenticationFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/chats");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest() == null ? null : ((HttpServletRequest) chain.getRequest()).getUserPrincipal());
    }

    /**
     * 验证存在本地 admin 登录态时正确注入 admin 主体。
     *
     * @throws ServletException Servlet 异常。
     * @throws IOException IO 异常。
     */
    @Test
    void injectsAdminPrincipalWhenAuthUserPresent() throws ServletException, IOException {
        final UnifiedAuthenticationFilter filter = new UnifiedAuthenticationFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/chats");
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(UnifiedAuthenticationFilter.SESSION_AUTH_USER_KEY, "admin");
        request.setSession(session);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean[] executed = new boolean[1];
        final FilterChain chain = (req, res) -> {
            final HttpServletRequest httpReq = (HttpServletRequest) req;
            final Principal principal = httpReq.getUserPrincipal();
            assertNotNull(principal);
            assertEquals("admin", principal.getName());
            assertEquals("admin", httpReq.getRemoteUser());
            executed[0] = true;
        };

        filter.doFilter(request, response, chain);
        assertEquals(true, executed[0]);
    }

    /**
     * 验证存在 UIAS 用户名时正确注入企业工号主体。
     *
     * @throws ServletException Servlet 异常。
     * @throws IOException IO 异常。
     */
    @Test
    void injectsUiasPrincipalWhenUiasUserPresent() throws ServletException, IOException {
        final UnifiedAuthenticationFilter filter = new UnifiedAuthenticationFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/chats");
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(UnifiedAuthenticationFilter.SESSION_UIAS_USER_KEY, "E10086");
        request.setSession(session);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean[] executed = new boolean[1];
        final FilterChain chain = (req, res) -> {
            final HttpServletRequest httpReq = (HttpServletRequest) req;
            final Principal principal = httpReq.getUserPrincipal();
            assertNotNull(principal);
            assertEquals("E10086", principal.getName());
            executed[0] = true;
        };

        filter.doFilter(request, response, chain);
        assertEquals(true, executed[0]);
    }
}
