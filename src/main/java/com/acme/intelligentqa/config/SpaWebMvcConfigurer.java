package com.acme.intelligentqa.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * 单页应用（SPA）前端静态资源映射与前端路由回退配置。
 */
@Configuration
public class SpaWebMvcConfigurer implements WebMvcConfigurer {

    /**
     * 静态资源存放路径。
     */
    private static final String STATIC_LOCATION = "classpath:/static/";

    /**
     * 前端单页应用首页文件名。
     */
    private static final String INDEX_PAGE = "index.html";

    /**
     * 注册视图控制器，根路径默认跳转至前端首页。
     *
     * @param registry 视图控制器注册中心。
     */
    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/" + INDEX_PAGE);
    }

    /**
     * 配置静态资源处理器，支持将非 API 路径回退至前端单页首页。
     *
     * @param registry 资源处理器注册中心。
     */
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_LOCATION)
                .resourceChain(true)
                .addResolver(new SpaPathResourceResolver());
    }

    /**
     * 针对单页应用定制的路径资源解析器。
     */
    static final class SpaPathResourceResolver extends PathResourceResolver {

        /**
         * 解析请求的静态资源，并在非接口请求未命中时回退到首页。
         *
         * @param resourcePath 请求的相对资源路径。
         * @param location 静态资源基准位置。
         * @return 匹配到的静态资源对象；未匹配或属于后端接口路径时返回 null。
         * @throws IOException 当解析相对资源发生 I/O 错误时抛出。
         */
        @Override
        protected Resource getResource(final String resourcePath, final Resource location)
                throws IOException {
            final Resource requestedResource = location.createRelative(resourcePath);
            if (requestedResource.exists() && requestedResource.isReadable()) {
                return requestedResource;
            }
            if (!resourcePath.startsWith("api/") && !resourcePath.startsWith("actuator/")) {
                final Resource indexResource = location.createRelative(INDEX_PAGE);
                if (indexResource.exists() && indexResource.isReadable()) {
                    return indexResource;
                }
            }
            return null;
        }
    }
}
