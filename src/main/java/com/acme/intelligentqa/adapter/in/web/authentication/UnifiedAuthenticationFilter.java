package com.acme.intelligentqa.adapter.in.web.authentication;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 统一身份认证过滤器，从服务端 Session 提取当前登录用户并包装为 Servlet 原生 Principal。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public final class UnifiedAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 会话中保存本地登录用户名的键名。
     */
    public static final String SESSION_AUTH_USER_KEY = "AUTH_USER";

    /**
     * 会话中保存 UIAS 用户名的键名。
     */
    public static final String SESSION_UIAS_USER_KEY = "uiasUserName";

    /**
     * 创建统一身份认证过滤器。
     */
    public UnifiedAuthenticationFilter() {
    }

    /**
     * 检查当前请求是否携带 Session 登录身份并注入用户主体。
     *
     * @param request HTTP 请求。
     * @param response HTTP 响应。
     * @param filterChain 后续过滤器链。
     * @throws ServletException 后续处理失败时抛出。
     * @throws IOException 输入输出失败时抛出。
     */
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        final Principal existingPrincipal = request.getUserPrincipal();
        if (existingPrincipal != null && existingPrincipal.getName() != null
                && !existingPrincipal.getName().trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        final HttpSession session = request.getSession(false);
        if (session != null) {
            final Object authUser = session.getAttribute(SESSION_AUTH_USER_KEY);
            if (authUser instanceof String && !((String) authUser).trim().isEmpty()) {
                filterChain.doFilter(new AuthenticatedRequestWrapper(request, ((String) authUser).trim()), response);
                return;
            }
            final Object uiasUser = session.getAttribute(SESSION_UIAS_USER_KEY);
            if (uiasUser instanceof String && !((String) uiasUser).trim().isEmpty()) {
                filterChain.doFilter(new AuthenticatedRequestWrapper(request, ((String) uiasUser).trim()), response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 为 Servlet API 注入已认证用户主体的请求包装器。
     */
    private static final class AuthenticatedRequestWrapper extends HttpServletRequestWrapper {

        /**
         * 已认证用户主体。
         */
        private final Principal principal;

        /**
         * 创建包装请求实例。
         *
         * @param request 原始 HTTP 请求。
         * @param username 认证用户名。
         */
        private AuthenticatedRequestWrapper(final HttpServletRequest request, final String username) {
            super(request);
            this.principal = new ImmutableUserPrincipal(username);
        }

        /**
         * 返回当前请求关联的用户主体。
         *
         * @return 当前用户主体。
         */
        @Override
        public Principal getUserPrincipal() {
            return this.principal;
        }

        /**
         * 返回当前远程认证用户名。
         *
         * @return 远程用户名。
         */
        @Override
        public String getRemoteUser() {
            return this.principal.getName();
        }
    }

    /**
     * 不可变用户主体实现。
     */
    private static final class ImmutableUserPrincipal implements Principal {

        /**
         * 用户名。
         */
        private final String name;

        /**
         * 创建不可变用户主体。
         *
         * @param name 用户名。
         */
        private ImmutableUserPrincipal(final String name) {
            this.name = name;
        }

        /**
         * 返回主体名称。
         *
         * @return 主体名称。
         */
        @Override
        public String getName() {
            return this.name;
        }
    }
}
