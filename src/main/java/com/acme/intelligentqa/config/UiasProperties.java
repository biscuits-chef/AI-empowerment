package com.acme.intelligentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * UIAS认证过滤器配置属性。
 */
@ConfigurationProperties("app.uias")
@ConstructorBinding
public final class UiasProperties {

    /**
     * 重定向URL，用户认证成功后重定向的地址。
     */
    private final String redirectUrl;

    /**
     * Token类型，默认为SAMLResponse。
     */
    private final String tokenType;

    /**
     * 域名，认证域名。
     */
    private final String realmName;

    /**
     * 数据中心名称。
     */
    private final String dcName;

    /**
     * 证书路径，用于验证SAML响应的证书。
     */
    private final String certPath;

    /**
     * 是否验证接收者，默认为true。
     */
    private final boolean checkRecipient;

    /**
     * 验证错误页面，认证失败时跳转的页面。
     */
    private final String validationErrorpage;

    /**
     * Filter名称，用于过滤器的注册。
     */
    private final String filterName;

    /**
     * Filter顺序，数字越小优先级越高。
     */
    private final int filterOrder;

    /**
     * URL模式，过滤器应用的URL模式。
     */
    private final String urlPattern;

    /**
     * 创建UIAS认证过滤器配置。
     *
     * @param redirectUrl 重定向URL。
     * @param tokenType Token类型。
     * @param realmName 域名。
     * @param dcName 数据中心名称。
     * @param certPath 证书路径。
     * @param checkRecipient 是否验证接收者。
     * @param validationErrorpage 验证错误页面。
     * @param filterName Filter名称。
     * @param filterOrder Filter顺序。
     * @param urlPattern URL模式。
     */
    public UiasProperties(
            final String redirectUrl,
            final String tokenType,
            final String realmName,
            final String dcName,
            final String certPath,
            final boolean checkRecipient,
            final String validationErrorpage,
            final String filterName,
            final int filterOrder,
            final String urlPattern) {
        this.redirectUrl = requireNonEmpty(redirectUrl, "redirect-url");
        this.tokenType = tokenType == null ? "SAMLResponse" : tokenType.trim();
        this.realmName = requireNonEmpty(realmName, "realm-name");
        this.dcName = requireNonEmpty(dcName, "dc-name");
        this.certPath = certPath == null ? "classpath:idp-2023-test.cer" : certPath.trim();
        this.checkRecipient = checkRecipient;
        this.validationErrorpage = validationErrorpage == null
                ? "/api/login?error=validation"
                : validationErrorpage.trim();
        this.filterName = filterName == null ? "UIASAuthFilter" : filterName.trim();
        this.filterOrder = filterOrder;
        this.urlPattern = urlPattern == null ? "/*" : urlPattern.trim();
    }

    /**
     * 返回重定向URL。
     *
     * @return 重定向URL。
     */
    public String redirectUrl() {
        return redirectUrl;
    }

    /**
     * 返回Token类型。
     *
     * @return Token类型。
     */
    public String tokenType() {
        return tokenType;
    }

    /**
     * 返回域名。
     *
     * @return 域名。
     */
    public String realmName() {
        return realmName;
    }

    /**
     * 返回数据中心名称。
     *
     * @return 数据中心名称。
     */
    public String dcName() {
        return dcName;
    }

    /**
     * 返回证书路径。
     *
     * @return 证书路径。
     */
    public String certPath() {
        return certPath;
    }

    /**
     * 返回是否验证接收者。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    public boolean checkRecipient() {
        return checkRecipient;
    }

    /**
     * 返回验证错误页面。
     *
     * @return 验证错误页面。
     */
    public String validationErrorpage() {
        return validationErrorpage;
    }

    /**
     * 返回Filter名称。
     *
     * @return Filter名称。
     */
    public String filterName() {
        return filterName;
    }

    /**
     * 返回Filter顺序。
     *
     * @return Filter顺序。
     */
    public int filterOrder() {
        return filterOrder;
    }

    /**
     * 返回URL模式。
     *
     * @return URL模式。
     */
    public String urlPattern() {
        return urlPattern;
    }

    /**
     * 校验字符串非空。
     *
     * @param value 输入值。
     * @param propertyName 属性名称。
     * @return 规范化后的字符串。
     */
    private static String requireNonEmpty(final String value, final String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("app.uias." + propertyName + " must not be blank");
        }
        return value.trim();
    }
}
