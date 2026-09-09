package com.acme.intelligentqa.adapter.in.web.authentication;

import com.acme.intelligentqa.config.AuthenticationProperties;
import java.io.IOException;
import java.security.Principal;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 在开发模拟认证模式下为业务接口注入服务端固定用户主体。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "mock")
public final class DevelopmentMockAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 业务接口路径前缀。
     */
    private static final String API_PATH_PREFIX = "/api/";
    /**
     * 开发模拟用户主体。
     */
    private final Principal mockPrincipal;

    /**
     * 创建开发模拟认证过滤器。
     *
     * @param properties 认证配置。
     */
    public DevelopmentMockAuthenticationFilter(final AuthenticationProperties properties) {
        this.mockPrincipal = new FixedMockPrincipal(properties.mockUserId());
    }

    /**
     * 判断当前请求是否无需注入模拟用户。
     *
     * @param request HTTP 请求。
     *
     * @return 非业务接口返回 {@code true}。
     */
    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PATH_PREFIX);
    }

    /**
     * 使用服务端固定用户包装业务请求后继续过滤器链。
     *
     * @param request HTTP 请求。
     *
     * @param response HTTP 响应。
     *
     * @param filterChain 后续过滤器链。
     *
     * @throws ServletException 后续 Servlet 处理失败时抛出。
     *
     * @throws IOException HTTP 输入输出失败时抛出。
     */
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        // 模拟模式始终使用服务端配置值，避免浏览器伪造 Header 或远程用户覆盖身份。
        filterChain.doFilter(new MockAuthenticatedRequest(request, mockPrincipal), response);
    }

    /**
     * 为 Servlet API 提供固定模拟用户的请求包装器。
     */
    private static final class MockAuthenticatedRequest extends HttpServletRequestWrapper {

        /**
         * 固定模拟用户主体。
         */
        private final Principal principal;

        /**
         * 创建带固定用户主体的请求包装器。
         *
         * @param request 原始 HTTP 请求。
         *
         * @param principal 固定模拟用户主体。
         */
        private MockAuthenticatedRequest(
                final HttpServletRequest request,
                final Principal principal) {
            super(request);
            this.principal = principal;
        }

        /**
         * 返回固定模拟用户主体。
         *
         * @return 固定模拟用户主体。
         */
        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        /**
         * 返回固定模拟用户标识。
         *
         * @return 固定模拟用户标识。
         */
        @Override
        public String getRemoteUser() {
            return principal.getName();
        }
    }

    /**
     * 表示只包含固定用户标识的开发模拟主体。
     */
    private static final class FixedMockPrincipal implements Principal {

        /**
         * 固定用户标识。
         */
        private final String name;

        /**
         * 创建固定模拟用户主体。
         *
         * @param name 固定用户标识。
         */
        private FixedMockPrincipal(final String name) {
            this.name = name;
        }

        /**
         * 返回固定用户标识。
         *
         * @return 固定用户标识。
         */
        @Override
        public String getName() {
            return name;
        }
    }
}
