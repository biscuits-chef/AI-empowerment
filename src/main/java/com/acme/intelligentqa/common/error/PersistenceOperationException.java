package com.acme.intelligentqa.common.error;

/** 表示持久化操作未满足预期的一致性条件。 */
public class PersistenceOperationException extends RuntimeException {

    /**
     * 异常类序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 创建 {@code PersistenceOperationException} 实例。
     *
     * @param message 提示信息。
     *
     * @param cause 异常原因。
     */
    public PersistenceOperationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
