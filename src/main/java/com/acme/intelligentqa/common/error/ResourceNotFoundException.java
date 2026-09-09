package com.acme.intelligentqa.common.error;

/** 表示当前用户无权访问或目标资源不存在。 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 异常类序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 创建 {@code ResourceNotFoundException} 实例。
     *
     * @param message 提示信息。
     */
    public ResourceNotFoundException(final String message) {
        super(message);
    }
}
