package com.acme.intelligentqa;

import com.acme.intelligentqa.config.AuthenticationProperties;
import com.acme.intelligentqa.config.BusinessQueryProperties;
import com.acme.intelligentqa.config.CancellationProperties;
import com.acme.intelligentqa.config.CompanyModelProperties;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.config.RuntimeEnvironmentProperties;
import com.acme.intelligentqa.config.TemporaryFileProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能问答与智能审核服务的 Spring Boot 启动入口。
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        QaProperties.class,
        AuthenticationProperties.class,
        BusinessQueryProperties.class,
        CompanyModelProperties.class,
        CancellationProperties.class,
        TemporaryFileProperties.class,
        RuntimeEnvironmentProperties.class
})
public class IntelligentQaApplication {

    /**
     * 启动 Spring Boot 智能问答服务。
     *
     * @param args 应用启动参数。
     */
    public static void main(final String[] args) {
        SpringApplication.run(IntelligentQaApplication.class, args);
    }
}
