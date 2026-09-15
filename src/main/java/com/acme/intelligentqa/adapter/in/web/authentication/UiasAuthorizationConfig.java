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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UiasAuthorizationConfig {

    private static final Logger logger = LoggerFactory.getLogger(UiasAuthorizationConfig.class);

    @Value("${uia.authority.ESB_SERVER_ADDR:http://esb.spdbbiz.com}")
    private String esbServerAddr;

    @Value("${uia.authority.HttpsFlag:false}")
    private String httpsFlag;

    @Value("${uia.authority.ProtocolFlag:TLSv1.2}")
    private String protocolFlag;

    @Value("${uia.authority.ConnectionTimeout:60000}")
    private int connectionTimeout;

    @Value("${uia.authority.ReceiveTimeout:60000}")
    private int receiveTimeout;

    @Value("${uia.authority.byESF:true}")
    private String byESF;

    @Bean
    public AuthorisationProperty authorisationProperty() {
        logger.info("初始化UIAS授权配置属性");

        String actualEsbServerAddr = getEsbServerAddr();
        String actualHttpsFlag = httpsFlag != null ? httpsFlag : "false";
        String actualByESF = byESF != null ? byESF : "true";
        
        AuthorisationProperty property = new AuthorisationProperty();
        property.setESB_SERVER_ADDR(actualEsbServerAddr);
        property.setHttpsFlag(actualHttpsFlag);
        property.setProtocolFlag(protocolFlag);
        property.setConnectionTimeout(connectionTimeout);
        property.setReceiveTimeout(receiveTimeout);
        property.setByESF(Boolean.valueOf(actualByESF));

        logger.info("UIAS授权配置: ESB={}, HTTPS={}, Timeout={}ms, ByESF={}", 
                    actualEsbServerAddr, actualHttpsFlag, connectionTimeout, actualByESF);

        return property;
    }

    @Bean
    public SSLManager sslManager() {
        logger.info("初始化SSLManager");
        
        SSLManager sslManager = new SSLManager();
        
        return sslManager;
    }

    @Bean
    public NewJaxWsproxyBean newJaxWsproxyBean() {
        logger.info("初始化NewJaxWsproxyBean");
        
        NewJaxWsproxyBean bean = new NewJaxWsproxyBean();
        
        return bean;
    }

    @Bean
    public NewAuthrQueryClient newAuthrQueryClient(final NewJaxWsproxyBean newJaxWsproxyBean) {
        logger.info("初始化NewAuthrQueryClient");
        
        NewAuthrQueryClient client = new NewAuthrQueryClient();
        
        return client;
    }

    @Bean
    public NewAuthrQueryClient_ESF newAuthrQueryClientEsf() {
        logger.info("初始化NewAuthrQueryClient_ESF");
        
        NewAuthrQueryClient_ESF client = new NewAuthrQueryClient_ESF();
        
        return client;
    }

    @Bean
    public NewAuthrQueryService newAuthrQueryService(final AuthorisationProperty authorisationProperty,
                                                     final NewAuthrQueryClient newAuthrQueryClient) {
        logger.info("初始化NewAuthrQueryService");
        
        NewAuthrQueryService service = new NewAuthrQueryService(authorisationProperty);
        
        return service;
    }

    private String getEsbServerAddr() {
        String envValue = System.getenv("ESB_SERVER_ADDR");
        if (envValue != null && !envValue.trim().isEmpty()) {
            logger.info("使用环境变量中的ESB_SERVER_ADDR: {}", envValue);
            return envValue;
        }
        logger.info("使用配置文件中的ESB_SERVER_ADDR: {}", esbServerAddr);
        return esbServerAddr;
    }
}
