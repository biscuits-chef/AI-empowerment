package com.acme.intelligentqa.adapter.in.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.config.AuthenticationProperties;
import org.junit.jupiter.api.Test;

/**
 * 验证普通用户密码管理器 UserPasswordManager 的密码生成与比对行为。
 */
class UserPasswordManagerTest {

    /**
     * 验证未配置静态密码时自动生成 16 位随机安全密码且默认用户名为 admin。
     */
    @Test
    void generatesSecurePasswordWhenNotConfigured() {
        final AuthenticationProperties properties = new AuthenticationProperties();
        final UserPasswordManager manager = new UserPasswordManager(properties);

        assertEquals("admin", manager.getUsername());
        assertNotNull(manager.getPassword());
        assertEquals(16, manager.getPassword().length());
        assertTrue(manager.isRandomlyGenerated());
        assertTrue(manager.verify("admin", manager.getPassword()));
        assertTrue(manager.verify("ADMIN", manager.getPassword()));
        assertFalse(manager.verify("admin", "wrong-password"));
        assertFalse(manager.verify("other-unknown-user", manager.getPassword()));
        assertFalse(manager.verify(null, manager.getPassword()));
        assertFalse(manager.verify("admin", null));
    }

    /**
     * 验证配置自定义用户名与密码时优先使用配置值。
     */
    @Test
    void usesConfiguredUsernameAndPasswordWhenSpecified() {
        final AuthenticationProperties properties = new AuthenticationProperties(
                "customuser",
                "MyCustomPass123!");
        final UserPasswordManager manager = new UserPasswordManager(properties);

        assertEquals("customuser", manager.getUsername());
        assertEquals("MyCustomPass123!", manager.getPassword());
        assertFalse(manager.isRandomlyGenerated());
        assertTrue(manager.verify("customuser", "MyCustomPass123!"));
        assertTrue(manager.verify("admin", "MyCustomPass123!"));
        assertFalse(manager.verify("customuser", "other-pass"));
    }
}
