package com.acme.intelligentqa.common.error;

/**
 * 用户名或密码错误异常。
 */
public class BadCredentialsException extends RuntimeException {

    /**
     * 创建 {@code BadCredentialsException} 实例。
     */
    public BadCredentialsException() {
        super("用户名或密码错误");
    }
}
