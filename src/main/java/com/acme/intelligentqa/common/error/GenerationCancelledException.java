package com.acme.intelligentqa.common.error;

/** 表示回答生成已被用户停止。 */
public class GenerationCancelledException extends RuntimeException {

    /**
     * 异常类序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 创建 {@code GenerationCancelledException} 实例。
     */
    public GenerationCancelledException() {
        super("answer generation cancellation was requested");
    }
}
