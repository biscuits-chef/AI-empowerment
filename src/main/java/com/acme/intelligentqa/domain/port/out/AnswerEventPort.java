package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.AnswerEvent;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 发布、订阅和重放回答事件的出站端口。
 */
public interface AnswerEventPort {

    /**
     * 按回答序号发布一个流式事件。
     *
     * @param answerId 回答 ID。
     *
     * @param type 类型。
     *
     * @param data 一个 SSE 事件的多行数据内容。
     */
    void publish(UUID answerId, String type, String data);

    /**
     * 从指定序号订阅回答事件。
     *
     * @param answerId 回答 ID。
     *
     * @param afterSequence 最后确认的事件序号。
     *
     * @param consumer 事件订阅回调。
     *
     * @return 从指定序号订阅回答事件。
     */
    Subscription subscribe(UUID answerId, long afterSequence, Consumer<AnswerEvent> consumer);

    /**
     * 可关闭的回答事件订阅句柄。
     */
    interface Subscription {
        /**
         * 移除当前事件订阅并释放资源。
         */
        void close();
    }
}
