package com.acme.intelligentqa.common.error;

/** 表示客户端请求的事件序号已超出当前可重放窗口。 */
public final class EventReplayGapException extends RuntimeException {

    /**
     * 异常类序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 创建 {@code EventReplayGapException} 实例。
     */
    public EventReplayGapException() {
        super("answer event history is incomplete; reload the persisted answer snapshot");
    }
}
