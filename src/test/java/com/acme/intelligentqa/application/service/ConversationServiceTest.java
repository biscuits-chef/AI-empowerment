package com.acme.intelligentqa.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.intelligentqa.common.error.ConversationActiveException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.AgentType;
import com.acme.intelligentqa.domain.port.out.ConversationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.AnswerEventHistoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 验证 ConversationService 的业务行为与边界。
 */
class ConversationServiceTest {

    /**
     * 固定测试时钟时间。
     */
    private static final Instant NOW = Instant.parse("2026-08-19T01:00:00Z");
    /**
     * 持久化仓储。
     */
    private FakeConversationRepository repository;
    /**
     * 被测应用服务。
     */
    private ConversationService service;

    /**
     * 初始化每个测试使用的隔离环境。
     */
    @BeforeEach
    void setUp() {
        repository = new FakeConversationRepository();
        service = new ConversationService(
                repository, new FakeAnswerEventHistory(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * 验证会话管理 HTTP 接口生命周期。
     */
    @Test
    void managesConversationLifecycle() {
        final Conversation created = service.create("user-1", "首个会话");
        repository.messages.add(new ChatMessage(
                UUID.randomUUID(), created.id(), null, ChatMessage.Role.USER, "问题", NOW));

        assertEquals("首个会话", service.get("user-1", created.id()).title());
        assertEquals(1, service.list("user-1", null, 10).items().size());
        assertEquals(1, service.messages("user-1", created.id(), 10).size());
        assertEquals("新名称", service.rename("user-1", created.id(), "新名称").title());

        service.delete("user-1", created.id());
        assertThrows(ResourceNotFoundException.class, () -> service.get("user-1", created.id()));
        assertThrows(ResourceNotFoundException.class, () -> service.delete("user-1", created.id()));
    }

    /**
     * 验证非法输入及错误用户均被拒绝。
     */
    @Test
    void rejectsInvalidInputsAndWrongOwner() {
        final Conversation created = service.create("user-1", "会话");

        assertThrows(IllegalArgumentException.class, () -> service.create("", "会话"));
        assertThrows(IllegalArgumentException.class, () -> service.create("user-1", ""));
        assertThrows(IllegalArgumentException.class, () -> service.rename(
                "user-1", created.id(), String.join("", Collections.nCopies(101, "a"))));
        assertThrows(IllegalArgumentException.class, () -> service.list("user-1", null, 0));
        assertThrows(IllegalArgumentException.class, () -> service.list("user-1", null, 101));
        assertThrows(IllegalArgumentException.class, () -> service.list("user-1", "bad-cursor", 10));
        assertThrows(ResourceNotFoundException.class, () -> service.get("user-2", created.id()));
        assertThrows(ResourceNotFoundException.class, () -> service.rename(
                "user-2", created.id(), "无权修改"));
    }

    /**
     * 验证活动回答存在时会话删除被固定提示拒绝。
     */
    @Test
    void rejectsDeletingConversationWithActiveAnswer() {
        final Conversation created = service.create("user-1", "执行中会话");
        repository.activeConversationId = created.id();

        assertThrows(ConversationActiveException.class, () -> service.delete("user-1", created.id()));
        assertEquals("执行中会话", service.get("user-1", created.id()).title());
    }

    /**
     * 测试使用的内存会话仓储。
     */
    private static final class FakeConversationRepository implements ConversationRepositoryPort {
        /**
         * 待处理值集合。
         */
        private final Map<UUID, Conversation> values = new LinkedHashMap<>();
        /**
         * 会话消息列表。
         */
        private final List<ChatMessage> messages = new ArrayList<>();
        /** 当前包含活动回答的会话 ID。 */
        private UUID activeConversationId;

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
        @Override
        public Conversation create(final UUID id, final String ownerId, final String title, final Instant now) {
            final Conversation conversation = new Conversation(id, ownerId, title, now, now);
            values.put(id, conversation);
            return conversation;
        }

        /**
         * 按首次提问参数创建带固定 Agent 类型的测试会话。
         *
         * @param id 唯一标识。
         * @param ownerId 用户所有者 ID。
         * @param title 会话名称。
         * @param agentType 会话创建时选定且不可变更的 Agent 类型。
         * @param idempotencyKey 首次提问幂等键。
         * @param now 当前时间。
         * @return 已创建的测试会话。
         */
        @Override
        public Conversation createForQuestion(
                final UUID id,
                final String ownerId,
                final String title,
                final AgentType agentType,
                final String idempotencyKey,
                final Instant now) {
            final Conversation conversation = new Conversation(
                    id, ownerId, title, agentType, now, now);
            values.put(id, conversation);
            return conversation;
        }

        /**
         * 按所属用户查询会话列表。
         *
         * @param ownerId 用户所有者 ID。
         * @param beforeUpdatedAt 游标中的最后更新时间。
         * @param beforeId 游标中的最后会话 ID。
         * @param limit 数量上限。
         * @return 按所属用户查询会话列表。
         */
        @Override
        public List<Conversation> listByOwner(
                final String ownerId,
                final Instant beforeUpdatedAt,
                final UUID beforeId,
                final int limit) {
            final List<Conversation> result = new ArrayList<>();
            for (final Conversation conversation : values.values()) {
                if (ownerId.equals(conversation.ownerId()) && result.size() < limit) {
                    result.add(conversation);
                }
            }
            return result;
        }

        /**
         * 判断会话是否仍有未进入终态的回答。
         *
         * @param ownerId 用户所有者 ID。
         * @param conversationId 会话 ID。
         * @return 存在活动回答时返回 true。
         */
        @Override
        public boolean hasActiveAnswer(final String ownerId, final UUID conversationId) {
            return conversationId.equals(activeConversationId) && findActive(ownerId, conversationId).isPresent();
        }

        /**
         * 构建当前用户未删除会话的查询条件。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param conversationId 会话 ID。
         *
         * @return 构建当前用户未删除会话的查询条件。
         */
        @Override
        public Optional<Conversation> findActive(final String ownerId, final UUID conversationId) {
            final Conversation conversation = values.get(conversationId);
            return conversation != null && ownerId.equals(conversation.ownerId())
                    ? Optional.of(conversation) : Optional.empty();
        }

        /**
         * 模拟事务内加锁读取有效会话。
         *
         * @param ownerId 用户所有者 ID。
         * @param conversationId 会话 ID。
         * @return 存在且归属匹配时返回会话。
         */
        @Override
        public Optional<Conversation> findActiveForUpdate(
                final String ownerId,
                final UUID conversationId) {
            return findActive(ownerId, conversationId);
        }

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
        @Override
        public List<ChatMessage> listMessages(final String ownerId, final UUID conversationId, final int limit) {
            return new ArrayList<>(messages.subList(0, Math.min(limit, messages.size())));
        }

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
        @Override
        public Optional<Conversation> rename(
                final String ownerId,
                final UUID conversationId,
                final String title,
                final Instant now) {
            final Optional<Conversation> current = findActive(ownerId, conversationId);
            if (!current.isPresent()) {
                return Optional.empty();
            }
            final Conversation changed = new Conversation(
                    conversationId, ownerId, title, current.get().createdAt(), now);
            values.put(conversationId, changed);
            return Optional.of(changed);
        }

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
        @Override
        public boolean softDelete(final String ownerId, final UUID conversationId, final Instant now) {
            return findActive(ownerId, conversationId).isPresent() && values.remove(conversationId) != null;
        }
    }

    /**
     * 会话服务单元测试使用的空回答事件历史。
     */
    private static final class FakeAnswerEventHistory implements AnswerEventHistoryPort {

        /**
         * 测试中不使用的事件追加操作。
         *
         * @param answerId 回答 ID。
         * @param type 事件类型。
         * @param data 事件数据。
         * @param occurredAt 发生时间。
         * @return 不返回，调用即抛出异常。
         */
        @Override
        public AnswerEvent append(
                final UUID answerId,
                final String type,
                final String data,
                final Instant occurredAt) {
            throw new UnsupportedOperationException("append is not used by conversation tests");
        }

        /**
         * 返回指定回答的空事件列表。
         *
         * @param answerId 回答 ID。
         * @param afterSequence 最后确认序号。
         * @param limit 数量上限。
         * @return 空事件列表。
         */
        @Override
        public List<AnswerEvent> list(final UUID answerId, final long afterSequence, final int limit) {
            return Collections.emptyList();
        }

        /**
         * 判断测试事件序号是否存在。
         *
         * @param answerId 回答 ID。
         * @param sequence 事件序号。
         * @return 始终返回 false。
         */
        @Override
        public boolean contains(final UUID answerId, final long sequence) { return false; }

        /**
         * 返回会话中回答的空展示事件映射。
         *
         * @param ownerId 用户所有者 ID。
         * @param conversationId 会话 ID。
         * @param answerIds 回答 ID 列表。
         * @return 空展示事件映射。
         */
        @Override
        public Map<UUID, List<AnswerEvent>> listDisplayEvents(
                final String ownerId,
                final UUID conversationId,
                final List<UUID> answerIds) {
            return Collections.emptyMap();
        }
    }
}
