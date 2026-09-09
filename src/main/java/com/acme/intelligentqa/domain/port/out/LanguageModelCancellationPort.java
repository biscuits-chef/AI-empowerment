package com.acme.intelligentqa.domain.port.out;

/**
 * 请求公司大模型停止指定生成任务的出站端口。
 */
public interface LanguageModelCancellationPort {

    /**
     * 请求公司模型停止远端生成。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param providerMessageId 公司模型侧消息 ID。
     */
    void stop(String ownerId, String providerMessageId);
}
