package com.acme.intelligentqa.common.error;

/**
 * 会话仍有活动回答时拒绝删除。
 */
public final class ConversationActiveException extends RuntimeException {

    /** 创建固定且可安全展示的活动会话冲突异常。 */
    public ConversationActiveException() {
        super("会话正在执行，请停止后删除");
    }
}
