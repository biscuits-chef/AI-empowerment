package com.acme.intelligentqa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 验证用户账号密码认证配置的规范化与解析行为。
 */
class AuthenticationPropertiesTest {

    /**
     * 验证未配置任何参数时默认用户名为 admin 且密码为空字符串。
     */
    @Test
    void resolvesDefaultUsernameWhenNotConfigured() {
        final AuthenticationProperties properties = new AuthenticationProperties();

        assertEquals("admin", properties.username());
        assertEquals("", properties.password());
        assertEquals("", properties.adminPassword());
    }

    /**
     * 验证配置自定义用户名与密码时去除首尾空白。
     */
    @Test
    void normalizesConfiguredUsernameAndPassword() {
        final AuthenticationProperties properties = new AuthenticationProperties(
                "  custom_user  ", "  pass123!  ");

        assertEquals("custom_user", properties.username());
        assertEquals("pass123!", properties.password());
        assertEquals("pass123!", properties.adminPassword());
    }

    /**
     * 验证兼容单参数密码构造器。
     */
    @Test
    void supportsSinglePasswordConstructor() {
        final AuthenticationProperties properties = new AuthenticationProperties("secretPass999");

        assertEquals("admin", properties.username());
        assertEquals("secretPass999", properties.password());
        assertEquals("secretPass999", properties.adminPassword());
    }

    /**
     * 验证向下兼容的 adminPassword 在未配置 password 时生效。
     */
    @Test
    void fallsBackToAdminPasswordWhenPasswordOmitted() {
        final AuthenticationProperties properties = new AuthenticationProperties(
                "admin", null, "compatPass888");

        assertEquals("admin", properties.username());
        assertEquals("compatPass888", properties.password());
        assertEquals("compatPass888", properties.adminPassword());
    }
}
