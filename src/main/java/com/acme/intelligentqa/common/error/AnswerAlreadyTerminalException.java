package com.acme.intelligentqa.common.error;

/** 表示回答已进入终态，不能再次执行当前操作。 */
public class AnswerAlreadyTerminalException extends RuntimeException {

    /**
     * 异常类序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 创建 {@code AnswerAlreadyTerminalException} 实例。
     *
     * @param status 业务状态。
     */
    public AnswerAlreadyTerminalException(final String status) {
        super("answer is already terminal: " + status);
    }
}
