package com.acme.intelligentqa.config;

import com.acme.intelligentqa.domain.service.PhaseOneBusinessQueryPlanner;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessSemanticCatalog;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessSemanticParser;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessSqlCompiler;
import java.time.Clock;
import java.util.concurrent.Executor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

/**
 * 装配核心端口实现及应用运行所需的基础 Bean。
 */
@Configuration(proxyBeanMethods = false)
public class CoreConfiguration {

    /**
     * 创建一期可计算业务语义目录。
     *
     * @return 一期可计算业务语义目录。
     */
    @Bean
    PhaseOneBusinessSemanticCatalog phaseOneBusinessSemanticCatalog() {
        return new PhaseOneBusinessSemanticCatalog();
    }

    /**
     * 创建一期业务语义解析器。
     *
     * @param clock 用于解析“今日”等相对日期的系统时钟。
     * @return 一期业务语义解析器。
     */
    @Bean
    PhaseOneBusinessSemanticParser phaseOneBusinessSemanticParser(final Clock clock) {
        return new PhaseOneBusinessSemanticParser(clock);
    }

    /**
     * 创建一期确定性业务查询规划器。
     *
     * @param catalog 一期可计算业务语义目录。
     * @return 一期确定性业务查询规划器。
     */
    @Bean
    PhaseOneBusinessQueryPlanner phaseOneBusinessQueryPlanner(
            final PhaseOneBusinessSemanticCatalog catalog) {
        return new PhaseOneBusinessQueryPlanner(catalog);
    }

    /**
     * 创建一期受控 SQL 编译器。
     *
     * @return 一期受控 SQL 编译器。
     */
    @Bean
    PhaseOneBusinessSqlCompiler phaseOneBusinessSqlCompiler() {
        return new PhaseOneBusinessSqlCompiler();
    }

    /**
     * 装配并执行运行环境保护校验。
     *
     * @param properties 配置参数。
     *
     * @param authenticationProperties 认证配置。
     *
     * @param companyModelProperties 公司模型配置。
     *
     * @param cancellationProperties 停止任务配置。
     *
     * @param environment Spring 运行环境。
     *
     * @return 装配并执行运行环境保护校验。
     */
    @Bean
    RuntimeEnvironmentGuard runtimeEnvironmentGuard(
            final RuntimeEnvironmentProperties properties,
            final AuthenticationProperties authenticationProperties,
            final CompanyModelProperties companyModelProperties,
            final CancellationProperties cancellationProperties,
            final Environment environment) {
        return new RuntimeEnvironmentGuard(
                properties, authenticationProperties, companyModelProperties,
                cancellationProperties, environment);
    }

    /**
     * 创建以 UTC 为基准的系统时钟。
     *
     * @return 创建以 UTC 为基准的系统时钟。
     */
    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    /**
     * 创建有界的回答生成线程池。
     *
     * @param properties 配置参数。
     *
     * @return 创建有界的回答生成线程池。
     */
    @Bean(name = "qaExecutor")
    Executor qaExecutor(final QaProperties properties) {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("qa-generation-");
        executor.setCorePoolSize(properties.executorCoreSize());
        executor.setMaxPoolSize(properties.executorMaxSize());
        executor.setQueueCapacity(properties.executorQueueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    /**
     * 处理带超时配置的公司模型 HTTP 客户端。
     *
     * @param builder 提示词文本构造器。
     *
     * @param properties 配置参数。
     *
     * @return 带超时配置的公司模型 HTTP 客户端。
     */
    @Bean(name = "companyModelRestTemplate")
    RestTemplate companyModelRestTemplate(
            final RestTemplateBuilder builder,
            final CompanyModelProperties properties) {
        return builder
                .setConnectTimeout(java.time.Duration.ofMillis(properties.connectTimeoutMillis()))
                .setReadTimeout(java.time.Duration.ofMillis(properties.readTimeoutMillis()))
                .build();
    }
}
