package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.ConversationContext;
import java.util.Optional;
import java.util.UUID;

/**
 * 以 Owner 和会话为边界保存结构化上下文的出站端口。
 */
public interface ConversationContextRepositoryPort {

    /**
     * 查询满足用户隔离条件的目标记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @return 查询满足用户隔离条件的目标记录。
     */
    Optional<ConversationContext> find(String ownerId, UUID conversationId);

    /**
     * 持久化当前结构化会话状态。
     *
     * @param context 结构化会话上下文。
     *
     * @param expectedVersion 期望的乐观锁版本号。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean save(ConversationContext context, long expectedVersion);
}
