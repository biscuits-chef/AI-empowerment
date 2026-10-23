package com.acme.intelligentqa.adapter.in.web.authentication;

import com.spdb.speedstudio.uias.authentication.filter.SAMLAuthTomcatFilter;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 包装 UIAS 原生 SAMLAuthTomcatFilter，放行静态资源、认证接口并避免已登录用户被二次重定向。
 */
public class UiasDelegatingFilter extends OncePerRequestFilter {

    /**
     * UIAS 原生 Tomcat SAML 过滤器。
     */
    private final SAMLAuthTomcatFilter target;

    /**
     * 创建 UIAS 代理过滤器实例。
     *
     * @param target UIAS 原生过滤器。
     */
    public UiasDelegatingFilter(final SAMLAuthTomcatFilter target) {
        this.target = target;
    }

    /**
     * 初始化被包装的原生过滤器。
     *
     * @throws ServletException 初始化失败时抛出。
     */
    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        this.target.init(getFilterConfig());
    }

    /**
     * 销毁被包装的原生过滤器。
     */
    @Override
    public void destroy() {
        super.destroy();
        this.target.destroy();
    }

    /**
     * 静态资源路径前缀白名单。
     */
    private static final String[] STATIC_PREFIXES = new String[] {"/assets/", "/favicon."};

    /**
     * 静态资源文件扩展名白名单。
     */
    private static final String[] STATIC_EXTENSIONS = new String[] {".js", ".css", ".png", ".ico", ".svg"};

    /**
     * 判断指定请求 URI 是否属于静态前端资源。
     *
     * @param uri 请求资源标识符。
     * @return 若属于静态前端资源返回 true；否则返回 false。
     */
    private boolean isStaticResource(final String uri) {
        if ("/".equals(uri) || "/index.html".equals(uri)) {
            return true;
        }
        for (final String prefix : STATIC_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        for (final String extension : STATIC_EXTENSIONS) {
            if (uri.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前会话是否包含已认证的用户状态。
     *
     * @param session 当前 HTTP 会话。
     * @return 若已建立合法认证会话返回 true；否则返回 false。
     */
    private boolean hasAuthenticatedSession(final HttpSession session) {
        if (session == null) {
            return false;
        }
        final boolean hasAuthUser = session.getAttribute(UnifiedAuthenticationFilter.SESSION_AUTH_USER_KEY) != null;
        final boolean hasSubject = session.getAttribute("subject") != null;
        return hasAuthUser || hasSubject;
    }

    /**
     * 判断当前请求是否应跳过 UIAS 过滤拦截。
     *
     * @param request HTTP 请求。
     * @return 若跳过返回 true；否则进入 UIAS 过滤。
     */
    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        // 0. 携带 SAML 响应参数的回调请求必须交由 UIAS 原生 Filter 校验并建立会话
        final String samlResponse = request.getParameter("SAMLResponse");
        if (samlResponse != null && !samlResponse.trim().isEmpty()) {
            return false;
        }

        final String uri = request.getRequestURI();
        // 1. 放行静态资源与 SPA 路由文件
        if (isStaticResource(uri)) {
            return true;
        }
        // 2. 放行认证接口与监控探针
        if (uri.startsWith("/api/v1/auth/") || uri.startsWith("/actuator/")) {
            return true;
        }
        // 3. 如果当前会话已建立 admin 认证或 UIAS 认证，直接放行
        if (hasAuthenticatedSession(request.getSession(false))) {
            return true;
        }
        // 4. 普通异步 API 请求且未携带 SAML 响应时放行，交由接口返回 401 触发前端登录，避免浏览器 fetch 收到 302 发生跨域异常
        return uri.startsWith("/api/");
    }

    /**
     * 执行 UIAS 代理过滤。
     *
     * @param request HTTP 请求。
     * @param response HTTP 响应。
     * @param filterChain 后续过滤器链。
     * @throws ServletException Servlet 处理异常。
     * @throws IOException 输入输出异常。
     */
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        target.doFilter(request, response, filterChain);
    }
}
