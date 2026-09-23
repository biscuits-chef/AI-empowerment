package com.acme.intelligentqa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

/**
 * 验证 SpaWebMvcConfigurer 的前端单页回退与静态资源映射逻辑。
 */
class SpaWebMvcConfigurerTest {

    /**
     * 被测配置类。
     */
    private final SpaWebMvcConfigurer configurer = new SpaWebMvcConfigurer();

    /**
     * 验证注册根路径跳转至 index.html。
     */
    @Test
    void addViewControllersRegistersRootToIndexPage() {
        final org.springframework.context.ApplicationContext context =
                mock(org.springframework.context.ApplicationContext.class);
        final ViewControllerRegistry registry = new ViewControllerRegistry(context);
        configurer.addViewControllers(registry);
        assertNotNull(registry);
    }

    /**
     * 验证静态资源请求存在时直接返回该资源。
     *
     * @throws IOException 当解析相对资源发生 I/O 错误时抛出。
     */
    @Test
    void getResourceReturnsRequestedResourceWhenExists() throws IOException {
        final SpaWebMvcConfigurer.SpaPathResourceResolver resolver =
                new SpaWebMvcConfigurer.SpaPathResourceResolver();
        final Resource location = mock(Resource.class);
        final Resource target = new ByteArrayResource("content".getBytes());

        when(location.createRelative("assets/test.js")).thenReturn(target);

        final Resource result = resolver.getResource("assets/test.js", location);
        assertEquals(target, result);
    }

    /**
     * 验证非接口且文件不存在的前端路由请求回退至 index.html。
     *
     * @throws IOException 当解析相对资源发生 I/O 错误时抛出。
     */
    @Test
    void getResourceFallsBackToIndexPageForNonApiRoutes() throws IOException {
        final SpaWebMvcConfigurer.SpaPathResourceResolver resolver =
                new SpaWebMvcConfigurer.SpaPathResourceResolver();
        final Resource location = mock(Resource.class);
        final Resource nonExistent = mock(Resource.class);
        final Resource indexPage = new ByteArrayResource("<html></html>".getBytes());

        when(location.createRelative("chat/session-123")).thenReturn(nonExistent);
        when(nonExistent.exists()).thenReturn(false);
        when(location.createRelative("index.html")).thenReturn(indexPage);

        final Resource result = resolver.getResource("chat/session-123", location);
        assertEquals(indexPage, result);
    }

    /**
     * 验证以 api/ 开头的未命中请求不回退至 index.html 而是返回 null。
     *
     * @throws IOException 当解析相对资源发生 I/O 错误时抛出。
     */
    @Test
    void getResourceDoesNotFallBackForApiRequests() throws IOException {
        final SpaWebMvcConfigurer.SpaPathResourceResolver resolver =
                new SpaWebMvcConfigurer.SpaPathResourceResolver();
        final Resource location = mock(Resource.class);
        final Resource nonExistent = mock(Resource.class);

        when(location.createRelative("api/questions")).thenReturn(nonExistent);
        when(nonExistent.exists()).thenReturn(false);

        final Resource result = resolver.getResource("api/questions", location);
        assertNull(result);
    }

    /**
     * 验证以 actuator/ 开头的未命中请求不回退至 index.html 而是返回 null。
     *
     * @throws IOException 当解析相对资源发生 I/O 错误时抛出。
     */
    @Test
    void getResourceDoesNotFallBackForActuatorRequests() throws IOException {
        final SpaWebMvcConfigurer.SpaPathResourceResolver resolver =
                new SpaWebMvcConfigurer.SpaPathResourceResolver();
        final Resource location = mock(Resource.class);
        final Resource nonExistent = mock(Resource.class);

        when(location.createRelative("actuator/health")).thenReturn(nonExistent);
        when(nonExistent.exists()).thenReturn(false);

        final Resource result = resolver.getResource("actuator/health", location);
        assertNull(result);
    }
}
