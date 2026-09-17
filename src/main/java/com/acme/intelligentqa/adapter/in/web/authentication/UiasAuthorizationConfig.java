package com.acme.intelligentqa.adapter.in.web.authentication;

import com.spdb.speedstudio.uias.authorisation.client.s120030044.NewAuthrQueryClient;
import com.spdb.speedstudio.uias.authorisation.client.s120030044.NewAuthrQueryClient_ESF;
import com.spdb.speedstudio.uias.authorisation.common.SSLManager;
import com.spdb.speedstudio.uias.authorisation.config.properties.AuthorisationProperty;
import com.spdb.speedstudio.uias.authorisation.jaxwsfactory.NewJaxWsproxyBean;
import com.spdb.speedstudio.uias.authorisation.service.NewAuthrQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UIAS 权限中心客户端授权与 ESB 交互配置。
 */
@Configuration
@ConditionalOnClass(name = "org.apache.cxf.jaxws.JaxWsProxyFactoryBean")
public class UiasAuthorizationConfig {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UiasAuthorizationConfig.class);

    /**
     * ESB 服务端地址。
     */
    @Value("${uia.authority.ESB_SERVER_ADDR:http://esb.spdbbiz.com}")
    private String esbServerAddr;

    /**
     * 是否启用 HTTPS 标识。
     */
    @Value("${uia.authority.HttpsFlag:false}")
    private String httpsFlag;

    /**
     * 传输协议标识。
     */
    @Value("${uia.authority.ProtocolFlag:TLSv1.2}")
    private String protocolFlag;

    /**
     * 连接超时时间（毫秒）。
     */
    @Value("${uia.authority.ConnectionTimeout:60000}")
    private int connectionTimeout;

    /**
     * 接收超时时间（毫秒）。
     */
    @Value("${uia.authority.ReceiveTimeout:60000}")
    private int receiveTimeout;

    /**
     * 是否通过 ESF 转发标识。
     */
    @Value("${uia.authority.byESF:true}")
    private String byESF;

    /**
     * 创建 {@code UiasAuthorizationConfig} 实例。
     */
    public UiasAuthorizationConfig() {
    }

    /**
     * 装配 UIAS 权限属性配置 Bean。
     *
     * @return UIAS 权限属性配置实例。
     */
    @Bean
    public AuthorisationProperty authorisationProperty() {
        LOGGER.info("初始化UIAS授权配置属性");

        final String actualEsbServerAddr = getEsbServerAddr();
        final String actualHttpsFlag = httpsFlag != null ? httpsFlag : "false";
        final String actualByESF = byESF != null ? byESF : "true";

        final AuthorisationProperty property = new AuthorisationProperty();
        property.setESB_SERVER_ADDR(actualEsbServerAddr);
        property.setHttpsFlag(actualHttpsFlag);
        property.setProtocolFlag(protocolFlag);
        property.setConnectionTimeout(connectionTimeout);
        property.setReceiveTimeout(receiveTimeout);
        property.setByESF(Boolean.valueOf(actualByESF));

        LOGGER.info("UIAS授权配置: ESB={}, HTTPS={}, Timeout={}ms, ByESF={}",
                actualEsbServerAddr, actualHttpsFlag, connectionTimeout, actualByESF);

        return property;
    }

    /**
     * 装配 SSL 管理器 Bean。
     *
     * @return SSL 管理器实例。
     */
    @Bean
    public SSLManager sslManager() {
        LOGGER.info("初始化SSLManager");
        return new SSLManager();
    }

    /**
     * 装配 JAX-WS 代理工厂 Bean。
     *
     * @return JAX-WS 代理工厂实例。
     */
    @Bean
    public NewJaxWsproxyBean newJaxWsproxyBean() {
        LOGGER.info("初始化NewJaxWsproxyBean");
        return new NewJaxWsproxyBean();
    }

    /**
     * 装配原生 UIAS 权限查询客户端 Bean。
     *
     * @param newJaxWsproxyBean JAX-WS 代理工厂。
     * @return UIAS 权限查询客户端实例。
     */
    @Bean
    public NewAuthrQueryClient newAuthrQueryClient(final NewJaxWsproxyBean newJaxWsproxyBean) {
        LOGGER.info("初始化NewAuthrQueryClient");
        return new NewAuthrQueryClient();
    }

    /**
     * 装配基于 ESF 的 UIAS 权限查询客户端 Bean。
     *
     * @return 基于 ESF 的 UIAS 权限查询客户端实例。
     */
    @Bean
    public NewAuthrQueryClient_ESF newAuthrQueryClientEsf() {
        LOGGER.info("初始化NewAuthrQueryClient_ESF");
        return new NewAuthrQueryClient_ESF();
    }

    /**
     * 装配 UIAS 权限查询服务 Bean。
     *
     * @param authorisationProperty UIAS 权限属性配置。
     * @param newAuthrQueryClient UIAS 权限查询客户端。
     * @return UIAS 权限查询服务实例。
     */
    @Bean
    public NewAuthrQueryService newAuthrQueryService(
            final AuthorisationProperty authorisationProperty,
            final NewAuthrQueryClient newAuthrQueryClient) {
        LOGGER.info("初始化NewAuthrQueryService");
        return new NewAuthrQueryService(authorisationProperty);
    }

    /**
     * 获取实际使用的 ESB 服务端地址。
     *
     * @return 实际使用的 ESB 服务端地址。
     */
    private String getEsbServerAddr() {
        final String envValue = System.getenv("ESB_SERVER_ADDR");
        if (envValue != null && !envValue.trim().isEmpty()) {
            LOGGER.info("使用环境变量中的ESB_SERVER_ADDR: {}", envValue);
            return envValue;
        }
        LOGGER.info("使用配置文件中的ESB_SERVER_ADDR: {}", esbServerAddr);
        return esbServerAddr;
    }
}
