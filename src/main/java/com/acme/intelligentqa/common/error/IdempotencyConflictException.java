package com.acme.intelligentqa.common.error;

/**
 * 表示同一个幂等键被用于不同的业务请求。
 */
public class IdempotencyConflictException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 创建幂等请求冲突异常。
     */
    public IdempotencyConflictException() {
        super("相同幂等键不能用于不同的请求内容");
    }
}
