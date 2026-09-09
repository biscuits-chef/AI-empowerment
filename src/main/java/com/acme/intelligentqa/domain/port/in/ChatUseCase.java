package com.acme.intelligentqa.domain.port.in;

import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.ConversationPage;
import java.util.List;
import java.util.UUID;

/**
 * 会话管理及历史消息查询的入站用例。
 */
public interface ChatUseCase {

    /**
     * 创建并持久化业务对象。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param title 会话名称。
     *
     * @return 创建并持久化业务对象。
     */
    Conversation create(String ownerId, String title);

    /**
     * 查询当前用户的会话列表。
     *
     * @param ownerId 用户所有者 ID。
     * @param cursor 上一页返回的不透明稳定游标，首页为空。
     * @param limit 数量上限。
     * @return 查询当前用户的会话列表。
     */
    ConversationPage list(String ownerId, String cursor, int limit);

    /**
     * 读取当前用户的目标业务对象。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @return 读取当前用户的目标业务对象。
     */
    Conversation get(String ownerId, UUID conversationId);

    /**
     * 处理会话消息列表。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param limit 数量上限。
     *
     * @return 会话消息列表。
     */
    List<ChatMessage> messages(String ownerId, UUID conversationId, int limit);

    /**
     * 修改当前用户的会话名称。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param title 会话名称。
     *
     * @return 修改当前用户的会话名称。
     */
    Conversation rename(String ownerId, UUID conversationId, String title);

    /**
     * 逻辑删除当前用户的会话。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     */
    void delete(String ownerId, UUID conversationId);
}
