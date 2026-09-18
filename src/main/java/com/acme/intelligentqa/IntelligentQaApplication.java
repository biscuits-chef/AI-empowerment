package com.acme.intelligentqa;

import com.acme.intelligentqa.config.AuthenticationProperties;
import com.acme.intelligentqa.config.BusinessQueryProperties;
import com.acme.intelligentqa.config.CancellationProperties;
import com.acme.intelligentqa.config.CompanyModelProperties;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.config.RagProperties;
import com.acme.intelligentqa.config.RuntimeEnvironmentProperties;
import com.acme.intelligentqa.config.TemporaryFileProperties;
import com.acme.intelligentqa.config.UiasProperties;
import com.joyintech.datahub.springboot.config.DataHubAutoConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
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
        RagProperties.class,
        CancellationProperties.class,
        TemporaryFileProperties.class,
        RuntimeEnvironmentProperties.class,
        UiasProperties.class
})
@Import({ DataHubAutoConfig.class})
public class IntelligentQaApplication {

    /**
     * 启动 Spring Boot 智能问答服务。
     * 若未在命令行或环境变量显式指定环境 Profile，则缺省激活 dev 开发环境以方便本地直接启动调试。
     *
     * @param args 应用启动参数。
     */
    public static void main(final String[] args) {
        if (System.getProperty("spring.profiles.active") == null
                && System.getenv("SPRING_PROFILES_ACTIVE") == null
                && (args == null || !hasProfileArg(args))) {
            System.setProperty("spring.profiles.active", "dev");
        }
        SpringApplication.run(IntelligentQaApplication.class, args);
    }

    /**
     * 检查启动入参中是否已指定 spring.profiles.active。
     *
     * @param args 命令行参数数组。
     * @return 若已指定环境 Profile 则返回 true，否则返回 false。
     */
    private static boolean hasProfileArg(final String[] args) {
        for (final String arg : args) {
            if (arg != null && arg.contains("spring.profiles.active")) {
                return true;
            }
        }
        return false;
    }
}
