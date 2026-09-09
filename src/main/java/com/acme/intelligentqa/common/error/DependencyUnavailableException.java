package com.acme.intelligentqa.common.error;

/** 表示知识库、数据库或模型等必要依赖当前不可用。 */
public class DependencyUnavailableException extends RuntimeException {

    /**
     * 异常类序列化版本号。
     */
    private static final long serialVersionUID = 1L;
    /**
     * 错误码。
     */
    private final String errorCode;

    /**
     * 创建 {@code DependencyUnavailableException} 实例。
     *
     * @param errorCode 错误码。
     *
     * @param message 提示信息。
     */
    public DependencyUnavailableException(final String errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建 {@code DependencyUnavailableException} 实例。
     *
     * @param errorCode 错误码。
     *
     * @param message 提示信息。
     *
     * @param cause 异常原因。
     */
    public DependencyUnavailableException(
            final String errorCode,
            final String message,
            final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 返回错误码。
     *
     * @return 错误码。
     */
    public String errorCode() {
        return errorCode;
    }
}
