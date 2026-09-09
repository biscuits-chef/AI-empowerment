package com.acme.intelligentqa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * 验证认证配置的规范化和安全校验。
 */
class AuthenticationPropertiesTest {

    /**
     * 验证模拟模式规范化固定用户标识。
     */
    @Test
    void normalizesMockUserId() {
        final AuthenticationProperties properties = new AuthenticationProperties(
                AuthenticationProperties.Mode.MOCK, "  dev-user-001  ");

        assertEquals(AuthenticationProperties.Mode.MOCK, properties.mode());
        assertEquals("dev-user-001", properties.mockUserId());
    }

    /**
     * 验证模拟模式拒绝空用户标识。
     */
    @Test
    void rejectsBlankMockUserId() {
        assertThrows(IllegalArgumentException.class, () -> new AuthenticationProperties(
                AuthenticationProperties.Mode.MOCK, "  "));
    }

    /**
     * 验证公司认证模式允许不配置模拟用户标识。
     */
    @Test
    void allowsMissingMockUserIdInCorporateMode() {
        final AuthenticationProperties properties = new AuthenticationProperties(
                AuthenticationProperties.Mode.CORPORATE, null);

        assertEquals("", properties.mockUserId());
    }

    /**
     * 验证认证模式为必填配置。
     */
    @Test
    void rejectsMissingAuthenticationMode() {
        assertThrows(NullPointerException.class, () -> new AuthenticationProperties(null, "user"));
    }
}
