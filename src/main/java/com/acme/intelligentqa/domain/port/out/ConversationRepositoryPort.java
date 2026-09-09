package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话与消息持久化的出站端口。
 */
public interface ConversationRepositoryPort {

    /**
     * 创建并持久化业务对象。
     *
     * @param id 唯一标识。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param title 会话名称。
     *
     * @param now 当前时间。
     *
     * @return 创建并持久化业务对象。
     */
    Conversation create(UUID id, String ownerId, String title, Instant now);

    /**
     * 按所属用户查询会话列表。
     *
     * @param ownerId 用户所有者 ID。
     * @param beforeUpdatedAt 上一页末项更新时间，首页为空。
     * @param beforeId 上一页末项 ID，首页为空。
     * @param limit 数量上限。
     * @return 按所属用户查询会话列表。
     */
    List<Conversation> listByOwner(
            String ownerId,
            Instant beforeUpdatedAt,
            UUID beforeId,
            int limit);

    /**
     * 判断会话是否仍有未进入终态的回答。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @return 存在活动回答时返回 true。
     */
    boolean hasActiveAnswer(String ownerId, UUID conversationId);

    /**
     * 构建当前用户未删除会话的查询条件。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @return 构建当前用户未删除会话的查询条件。
     */
    Optional<Conversation> findActive(String ownerId, UUID conversationId);

    /**
     * 在当前事务中锁定并读取有效会话，串行化问题提交与删除。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @return 存在且归属匹配时返回加锁后的会话。
     */
    Optional<Conversation> findActiveForUpdate(String ownerId, UUID conversationId);

    /**
     * 读取会话的有界历史消息。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param limit 数量上限。
     *
     * @return 读取会话的有界历史消息。
     */
    List<ChatMessage> listMessages(String ownerId, UUID conversationId, int limit);

    /**
     * 修改当前用户的会话名称。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param title 会话名称。
     *
     * @param now 当前时间。
     *
     * @return 修改当前用户的会话名称。
     */
    Optional<Conversation> rename(String ownerId, UUID conversationId, String title, Instant now);

    /**
     * 按用户归属对会话执行逻辑删除。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param now 当前时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean softDelete(String ownerId, UUID conversationId, Instant now);
}
