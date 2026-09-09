package com.acme.intelligentqa.domain.port.in;

import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import java.util.UUID;

/**
 * 停止回答生成的入站用例。
 */
public interface AnswerCancellationUseCase {

    /**
     * 停止回答的业务原因。
     */
    enum CancellationReason {
        /**
         * 由用户主动请求停止。
         */
        USER_REQUESTED
    }

    /**
     * 申请停止指定回答并返回当前状态。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param reason 原因。
     *
     * @return 申请停止指定回答并返回当前状态。
     */
    AnswerSnapshot cancel(
            String ownerId,
            UUID answerId,
            String idempotencyKey,
            CancellationReason reason);
}
