package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.ConversationPage;
import com.acme.intelligentqa.domain.port.in.ChatUseCase;
import com.acme.intelligentqa.domain.port.out.ConversationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.AnswerEventHistoryPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排会话生命周期，并确保所有操作按 Owner 隔离。
 */
@Service
public class ConversationService implements ChatUseCase {

    /**
     * 会话名称最大字符数。
     */
    private static final int MAX_TITLE_CHARACTERS = 100;
    /**
     * 单次分页查询数量上限。
     */
    private static final int MAX_PAGE_SIZE = 100;
    /**
     * Owner 字段名。
     */
    private static final String OWNER_ID_FIELD = "ownerId";
    /**
     * 持久化仓储。
     */
    private final ConversationRepositoryPort repository;
    /** 回答执行事件历史端口。 */
    private final AnswerEventHistoryPort eventHistory;
    /**
     * 系统时钟。
     */
    private final Clock clock;

    /**
     * 创建 {@code ConversationService} 实例。
     *
     * @param repository 持久化仓储。
     *
     * @param eventHistory 回答执行事件历史端口。
     *
     * @param clock 系统时钟。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public ConversationService(
            final ConversationRepositoryPort repository,
            final AnswerEventHistoryPort eventHistory,
            final Clock clock) {
        this.repository = repository;
        this.eventHistory = eventHistory;
        this.clock = clock;
    }

    /**
     * 创建并持久化业务对象。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param title 会话名称。
     *
     * @return 创建并持久化业务对象。
     */
    @Override
    @Transactional
    public Conversation create(final String ownerId, final String title) {
        final String validOwner = requireText(ownerId, OWNER_ID_FIELD);
        final String validTitle = validateTitle(title);
        return repository.create(UUID.randomUUID(), validOwner, validTitle, Instant.now(clock));
    }

    /**
     * 查询当前用户的会话列表。
     *
     * @param ownerId 用户所有者 ID。
     * @param cursor 上一页返回的不透明稳定游标，首页为空。
     * @param limit 数量上限。
     * @return 查询当前用户的会话列表。
     */
    @Override
    @Transactional(readOnly = true)
    public ConversationPage list(final String ownerId, final String cursor, final int limit) {
        final int validLimit = boundedLimit(limit);
        final ConversationCursor decoded = decodeCursor(cursor);
        final List<Conversation> records = repository.listByOwner(
                requireText(ownerId, OWNER_ID_FIELD),
                decoded == null ? null : decoded.updatedAt,
                decoded == null ? null : decoded.id,
                validLimit + 1);
        final boolean hasMore = records.size() > validLimit;
        final List<Conversation> items = new ArrayList<>(records.subList(
                0, Math.min(validLimit, records.size())));
        final String nextCursor = hasMore && !items.isEmpty()
                ? encodeCursor(items.get(items.size() - 1)) : null;
        return new ConversationPage(items, nextCursor, hasMore);
    }

    /**
     * 读取当前用户的目标业务对象。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @return 读取当前用户的目标业务对象。
     */
    @Override
    @Transactional(readOnly = true)
    public Conversation get(final String ownerId, final UUID conversationId) {
        return find(ownerId, conversationId);
    }

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
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> messages(final String ownerId, final UUID conversationId, final int limit) {
        find(ownerId, conversationId);
        final List<ChatMessage> messages = repository.listMessages(
                ownerId, conversationId, boundedLimit(limit));
        final List<UUID> answerIds = new ArrayList<>();
        for (final ChatMessage message : messages) {
            if (message.answerId() != null) {
                answerIds.add(message.answerId());
            }
        }
        final Map<UUID, List<AnswerEvent>> events = eventHistory.listDisplayEvents(
                ownerId, conversationId, answerIds);
        final List<ChatMessage> restored = new ArrayList<>(messages.size());
        for (final ChatMessage message : messages) {
            restored.add(new ChatMessage(
                    message.id(), message.conversationId(), message.answerId(), message.answerStatus(),
                    message.role(), message.content(), message.createdAt(), message.attachments(),
                    message.answerId() == null
                            ? Collections.<AnswerEvent>emptyList()
                            : events.getOrDefault(message.answerId(), Collections.<AnswerEvent>emptyList())));
        }
        return restored;
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
     * @return 修改当前用户的会话名称。
     */
    @Override
    @Transactional
    public Conversation rename(final String ownerId, final UUID conversationId, final String title) {
        return repository.rename(
                        requireText(ownerId, OWNER_ID_FIELD),
                        Objects.requireNonNull(conversationId, "conversationId must not be null"),
                        validateTitle(title),
                        Instant.now(clock))
                .orElseThrow(() -> notFound(conversationId));
    }

    /**
     * 逻辑删除当前用户的会话。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     */
    @Override
    @Transactional
    public void delete(final String ownerId, final UUID conversationId) {
        final String validOwner = requireText(ownerId, OWNER_ID_FIELD);
        final UUID validConversationId = Objects.requireNonNull(
                conversationId, "conversationId must not be null");
        repository.findActiveForUpdate(validOwner, validConversationId)
                .orElseThrow(() -> notFound(validConversationId));
        if (repository.hasActiveAnswer(validOwner, validConversationId)) {
            throw new com.acme.intelligentqa.common.error.ConversationActiveException();
        }
        if (!repository.softDelete(validOwner, validConversationId, Instant.now(clock))) {
            throw notFound(conversationId);
        }
    }

    /**
     * 把最后一条会话的稳定排序键编码为不透明游标。
     *
     * @param conversation 当前页最后一条会话。
     * @return URL 安全的不透明游标。
     */
    private String encodeCursor(final Conversation conversation) {
        final String value = conversation.updatedAt().toString() + "|" + conversation.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析并校验客户端传入的不透明游标。
     *
     * @param cursor 客户端上一页返回的游标。
     * @return 排序边界；空游标返回 null。
     */
    private ConversationCursor decodeCursor(final String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }
        try {
            final String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            final int separator = decoded.lastIndexOf('|');
            if (separator <= 0 || separator == decoded.length() - 1) {
                throw new IllegalArgumentException("invalid conversation cursor");
            }
            return new ConversationCursor(
                    Instant.parse(decoded.substring(0, separator)),
                    UUID.fromString(decoded.substring(separator + 1)));
        } catch (final IllegalArgumentException exception) {
            throw new IllegalArgumentException("invalid conversation cursor", exception);
        }
    }

    /**
     * 会话游标解码后的稳定排序边界。
     */
    private static final class ConversationCursor {
        /** 最后一条会话更新时间。 */
        private final Instant updatedAt;
        /** 最后一条会话 ID。 */
        private final UUID id;

        /**
         * 创建会话排序边界。
         *
         * @param updatedAt 最后一条会话更新时间。
         * @param id 最后一条会话 ID。
         */
        ConversationCursor(final Instant updatedAt, final UUID id) {
            this.updatedAt = updatedAt;
            this.id = id;
        }
    }

    /**
     * 查询满足用户隔离条件的目标记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @return 查询满足用户隔离条件的目标记录。
     */
    private Conversation find(final String ownerId, final UUID conversationId) {
        return repository.findActive(
                        requireText(ownerId, OWNER_ID_FIELD),
                        Objects.requireNonNull(conversationId, "conversationId must not be null"))
                .orElseThrow(() -> notFound(conversationId));
    }

    /**
     * 校验会话名称非空且未超过字符上限。
     *
     * @param title 会话名称。
     *
     * @return 校验会话名称非空且未超过字符上限。
     */
    private String validateTitle(final String title) {
        final String value = requireText(title, "title");
        if (value.length() > MAX_TITLE_CHARACTERS) {
            throw new IllegalArgumentException("title exceeds 100 characters");
        }
        return value;
    }

    /**
     * 将查询条数限制在允许范围内。
     *
     * @param limit 数量上限。
     *
     * @return 将查询条数限制在允许范围内。
     */
    private int boundedLimit(final int limit) {
        if (limit <= 0 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        return limit;
    }

    /**
     * 校验文本非空并返回原值。
     *
     * @param value 输入值。
     *
     * @param field 字段名称。
     *
     * @return 校验文本非空并返回原值。
     */
    private static String requireText(final String value, final String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /**
     * 创建资源不存在或不可访问的统一异常。
     *
     * @param conversationId 会话 ID。
     *
     * @return 创建资源不存在或不可访问的统一异常。
     */
    private ResourceNotFoundException notFound(final UUID conversationId) {
        return new ResourceNotFoundException("conversation not found: " + conversationId);
    }
}
