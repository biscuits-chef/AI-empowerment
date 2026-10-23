package com.acme.intelligentqa.adapter.in.web.authentication;

import com.acme.intelligentqa.common.error.AuthenticationRequiredException;
import com.acme.intelligentqa.common.error.BadCredentialsException;
import com.acme.intelligentqa.config.UiasProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供管理员账密登录、UIAS SSO 跳转、登出及当前登录身份查询接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * 普通用户密码管理器。
     */
    private final UserPasswordManager userPasswordManager;

    /**
     * UIAS 认证配置属性。
     */
    private final UiasProperties uiasProperties;

    /**
     * 创建认证控制器实例。
     *
     * @param userPasswordManager 普通用户密码管理器。
     * @param uiasProperties UIAS 认证配置属性。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected dependencies are retained and not exposed")
    public AuthController(
            final UserPasswordManager userPasswordManager,
            final UiasProperties uiasProperties) {
        this.userPasswordManager = userPasswordManager;
        this.uiasProperties = uiasProperties;
    }

    /**
     * 校验普通用户用户名和密码，成功后建立 Session 认证会话。
     *
     * @param request 登录请求体。
     * @param httpRequest HTTP 原生请求。
     * @return 登录成功用户信息。
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody final LoginRequest request,
            final HttpServletRequest httpRequest) {
        final String username = request != null && request.getUsername() != null
                ? request.getUsername().trim() : "";
        final String password = request != null ? request.getPassword() : null;
        if (username.isEmpty() || !userPasswordManager.verify(username, password)) {
            throw new BadCredentialsException();
        }
        final HttpSession session = httpRequest.getSession(true);
        session.setAttribute(UnifiedAuthenticationFilter.SESSION_AUTH_USER_KEY, username);
        session.setAttribute(UnifiedAuthenticationFilter.SESSION_UIAS_USER_KEY, username);
        session.setAttribute("subject", username);

        return ResponseEntity.ok(new LoginResponse(
                true,
                username,
                "普通用户",
                "LOCAL"));
    }

    /**
     * 注销当前用户的会话认证状态。
     *
     * @param httpRequest HTTP 原生请求。
     * @return HTTP 204 无内容响应。
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(final HttpServletRequest httpRequest) {
        final HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 从当前 HTTP 会话中解析已认证的用户名。
     *
     * @param session 当前 HTTP 会话。
     * @return 解析得到的用户名；若未找到则返回空字符串。
     */
    private String resolveUsernameFromSession(final HttpSession session) {
        if (session == null) {
            return "";
        }
        final Object authUser = session.getAttribute(UnifiedAuthenticationFilter.SESSION_AUTH_USER_KEY);
        if (authUser instanceof String && !((String) authUser).trim().isEmpty()) {
            return ((String) authUser).trim();
        }
        final Object uiasUser = session.getAttribute(UnifiedAuthenticationFilter.SESSION_UIAS_USER_KEY);
        if (uiasUser instanceof String && !((String) uiasUser).trim().isEmpty()) {
            return ((String) uiasUser).trim();
        }
        return "";
    }

    /**
     * 从当前请求的主体凭据中解析用户名。
     *
     * @param principal 当前请求的用户主体。
     * @return 解析得到的用户名；若未找到则返回空字符串。
     */
    private String resolveUsernameFromPrincipal(final Principal principal) {
        if (principal == null || principal.getName() == null) {
            return "";
        }
        return principal.getName().trim();
    }

    /**
     * 获取当前登录人的身份信息；未登录时抛出认证异常返回 401。
     *
     * @param principal 当前请求的用户主体。
     * @param httpRequest HTTP 原生请求。
     * @return 当前登录用户信息。
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(
            final Principal principal,
            final HttpServletRequest httpRequest) {
        final HttpSession session = httpRequest.getSession(false);
        final String sessionUser = resolveUsernameFromSession(session);
        final String username = sessionUser.isEmpty() ? resolveUsernameFromPrincipal(principal) : sessionUser;

        if (username.isEmpty()) {
            throw new AuthenticationRequiredException();
        }

        final boolean isLocalUser = session != null
                && session.getAttribute(UnifiedAuthenticationFilter.SESSION_AUTH_USER_KEY) != null;
        return ResponseEntity.ok(new LoginResponse(
                true,
                username,
                isLocalUser ? "普通用户" : "企业用户",
                isLocalUser ? "LOCAL" : "UIAS"));
    }

    /**
     * 获取当前环境配置的 UIAS SSO 单点登录跳转地址。
     *
     * @param httpRequest HTTP 原生请求。
     * @return UIAS SSO 跳转地址响应。
     */
    @GetMapping("/uias/login-url")
    public ResponseEntity<UiasLoginUrlResponse> uiasLoginUrl(final HttpServletRequest httpRequest) {
        final String redirectUrl = uiasProperties.redirectUrl();
        final String referer = httpRequest.getHeader("Referer");
        final String ssoTarget = referer != null && !referer.trim().isEmpty() ? referer.trim() : "/";
        final String delimiter = redirectUrl.contains("?") ? "&" : "?";
        final String fullUrl = redirectUrl + delimiter + "ssotarget=" + ssoTarget;
        return ResponseEntity.ok(new UiasLoginUrlResponse(fullUrl));
    }

    /**
     * 账密登录请求入参。
     */
    public static final class LoginRequest {

        /**
         * 登录用户名。
         */
        private String username;

        /**
         * 登录明文密码。
         */
        private String password;

        /**
         * 创建空登录请求。
         */
        public LoginRequest() {
        }

        /**
         * 创建登录请求。
         *
         * @param username 登录用户名。
         * @param password 登录明文密码。
         */
        public LoginRequest(final String username, final String password) {
            this.username = username;
            this.password = password;
        }

        /**
         * 获取登录用户名。
         *
         * @return 登录用户名。
         */
        public String getUsername() {
            return username;
        }

        /**
         * 设置登录用户名。
         *
         * @param username 登录用户名。
         */
        public void setUsername(final String username) {
            this.username = username;
        }

        /**
         * 获取登录明文密码。
         *
         * @return 登录明文密码。
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置登录明文密码。
         *
         * @param password 登录明文密码。
         */
        public void setPassword(final String password) {
            this.password = password;
        }
    }

    /**
     * 登录态与用户信息响应。
     */
    public static final class LoginResponse {

        /**
         * 是否已认证。
         */
        private final boolean authenticated;

        /**
         * 用户唯一标识/账号。
         */
        private final String username;

        /**
         * 用户显示名称。
         */
        private final String displayName;

        /**
         * 认证方式（ADMIN 或 UIAS）。
         */
        private final String authType;

        /**
         * 创建登录响应。
         *
         * @param authenticated 是否已认证。
         * @param username 用户唯一标识。
         * @param displayName 用户显示名称。
         * @param authType 认证方式。
         */
        public LoginResponse(
                final boolean authenticated,
                final String username,
                final String displayName,
                final String authType) {
            this.authenticated = authenticated;
            this.username = username;
            this.displayName = displayName;
            this.authType = authType;
        }

        /**
         * 返回是否已认证。
         *
         * @return 是否已认证。
         */
        public boolean isAuthenticated() {
            return authenticated;
        }

        /**
         * 返回用户名。
         *
         * @return 用户名。
         */
        public String getUsername() {
            return username;
        }

        /**
         * 返回用户显示名称。
         *
         * @return 用户显示名称。
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * 返回认证方式。
         *
         * @return 认证方式。
         */
        public String getAuthType() {
            return authType;
        }
    }

    /**
     * UIAS 跳转地址响应。
     */
    public static final class UiasLoginUrlResponse {

        /**
         * UIAS SSO 跳转地址。
         */
        private final String loginUrl;

        /**
         * 创建 UIAS 跳转地址响应。
         *
         * @param loginUrl UIAS SSO 跳转地址。
         */
        public UiasLoginUrlResponse(final String loginUrl) {
            this.loginUrl = loginUrl;
        }

        /**
         * 返回 UIAS SSO 跳转地址。
         *
         * @return UIAS SSO 跳转地址。
         */
        public String getLoginUrl() {
            return loginUrl;
        }
    }
}
