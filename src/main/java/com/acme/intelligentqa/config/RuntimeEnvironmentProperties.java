package com.acme.intelligentqa.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 标识当前运行环境，供启动保护和可观测性标签使用。
 */
@ConfigurationProperties("app.runtime")
@ConstructorBinding
public final class RuntimeEnvironmentProperties {

    /**
     * 运行环境标识。
     */
    private final Stage stage;
    /**
     * 待加载的环境 Profile。
     */
    private final String profile;

    /**
     * 创建 {@code RuntimeEnvironmentProperties} 实例。
     *
     * @param stage 运行环境标识。
     *
     * @param profile 待加载的环境 Profile。
     */
    public RuntimeEnvironmentProperties(final Stage stage, final String profile) {
        this.stage = Objects.requireNonNull(stage, "app.runtime.stage must be configured by an environment profile");
        this.profile = requireText(profile);
    }

    /**
     * 返回运行环境标识。
     *
     * @return 运行环境标识。
     */
    public Stage stage() {
        return stage;
    }

    /**
     * 处理待加载的环境 Profile。
     *
     * @return 待加载的环境 Profile。
     */
    public String profile() {
        return profile;
    }

    /**
     * 校验文本非空并返回原值。
     *
     * @param value 输入值。
     *
     * @return 校验文本非空并返回原值。
     */
    private static String requireText(final String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("app.runtime.profile must be configured by an environment profile");
        }
        return value.trim();
    }

    /**
     * 开发、测试和生产运行环境标识。
     */
    public enum Stage {
        /**
         * 开发环境。
         */
        DEVELOPMENT("dev"),
        /**
         * 测试环境。
         */
        TEST("test"),
        /**
         * 生产环境。
         */
        PRODUCTION("prod");

        /**
         * 待加载的环境 Profile。
         */
        private final String profile;

        /**
         * 创建 {@code Stage} 实例。
         *
         * @param profile 待加载的环境 Profile。
         */
        Stage(final String profile) {
            this.profile = profile;
        }

        /**
         * 处理待加载的环境 Profile。
         *
         * @return 待加载的环境 Profile。
         */
        public String profile() {
            return profile;
        }
    }
}
