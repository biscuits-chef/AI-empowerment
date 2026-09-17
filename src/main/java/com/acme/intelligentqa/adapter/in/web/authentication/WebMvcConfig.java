package com.acme.intelligentqa.adapter.in.web.authentication;

import com.acme.intelligentqa.config.UiasProperties;
import com.spdb.speedstudio.uias.authentication.filter.SAMLAuthTomcatFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * Web MVC配置，包含UIAS认证过滤器配置。
 */
@Configuration
@ConditionalOnClass(name = "org.opensaml.xml.validation.ValidationException")
public class WebMvcConfig {

    /**
     * UIAS认证过滤器配置属性。
     */
    private final UiasProperties uiasProperties;

    /**
     * 创建Web MVC配置。
     *
     * @param uiasProperties UIAS认证过滤器配置属性。
     */
    public WebMvcConfig(final UiasProperties uiasProperties) {
        this.uiasProperties = uiasProperties;
    }

    /**
     * 配置UIAS认证过滤器。
     *
     * @return UIAS认证过滤器的注册Bean。
     */
    @Bean
    public FilterRegistrationBean<SAMLAuthTomcatFilter> uiasFilter() {
        FilterRegistrationBean<SAMLAuthTomcatFilter> registration = new FilterRegistrationBean<>();

        SAMLAuthTomcatFilter filter = new SAMLAuthTomcatFilter();

        registration.setFilter(filter);
        registration.addUrlPatterns(uiasProperties.urlPattern());
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.setName(uiasProperties.filterName());
        registration.setOrder(uiasProperties.filterOrder());

        registration.addInitParameter("redirectURL", uiasProperties.redirectUrl());
        registration.addInitParameter("token-type", uiasProperties.tokenType());
        registration.addInitParameter("realm-name", uiasProperties.realmName());
        registration.addInitParameter("dcName", uiasProperties.dcName());
        registration.addInitParameter("cert_path", uiasProperties.certPath());
        registration.addInitParameter("checkRecipient", String.valueOf(uiasProperties.checkRecipient()));
        registration.addInitParameter("validationErrorpage", uiasProperties.validationErrorpage());

        return registration;
    }
}
