package com.acme.intelligentqa.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * 在 Spring 环境初始化早期加载公共及分环境 XML Properties 配置。
 */
public final class XmlApplicationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * XML 配置属性源名称。
     */
    static final String PROPERTY_SOURCE_NAME = "xmlApplicationConfig";
    /**
     * 公共 XML 配置资源路径。
     */
    private static final String BASE_CONFIG = "application.xml";
    /**
     * 允许启用的环境 Profile 集合。
     */
    private static final Set<String> ENVIRONMENT_PROFILES = new HashSet<>(
            Arrays.asList("dev", "test", "prod"));

    /**
     * 在容器启动前加载并校验 XML 环境配置。
     *
     * @param environment Spring 运行环境。
     *
     * @param application Spring 应用实例。
     */
    @Override
    public void postProcessEnvironment(
            final ConfigurableEnvironment environment,
            final SpringApplication application) {
        final Properties merged = load(BASE_CONFIG);
        for (final String profile : environment.getActiveProfiles()) {
            if (ENVIRONMENT_PROFILES.contains(profile)) {
                merged.putAll(load("application-" + profile + ".xml"));
            }
        }
        final PropertiesPropertySource propertySource =
                new PropertiesPropertySource(PROPERTY_SOURCE_NAME, merged);
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().replace(PROPERTY_SOURCE_NAME, propertySource);
        } else {
            environment.getPropertySources().addLast(propertySource);
        }
    }

    /**
     * 返回配置后处理器执行优先级。
     *
     * @return 配置后处理器执行优先级。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 11;
    }

    /**
     * 加载指定 XML 配置资源。
     *
     * @param resourceName XML 配置资源名称。
     *
     * @return 加载指定 XML 配置资源。
     */
    private Properties load(final String resourceName) {
        final Resource resource = new ClassPathResource(resourceName);
        if (!resource.exists()) {
            throw new IllegalStateException("required XML configuration is missing: " + resourceName);
        }
        try {
            return PropertiesLoaderUtils.loadProperties(resource);
        } catch (final IOException exception) {
            throw new IllegalStateException("failed to load XML configuration: " + resourceName, exception);
        }
    }
}
