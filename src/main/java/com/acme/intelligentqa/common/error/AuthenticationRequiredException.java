package com.acme.intelligentqa.common.error;

/** 表示请求缺少可信认证主体。 */
public class AuthenticationRequiredException extends RuntimeException {

    /**
     * 异常类序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 创建 {@code AuthenticationRequiredException} 实例。
     */
    public AuthenticationRequiredException() {
        super("authenticated user is required");
    }
}
