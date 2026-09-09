package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.ChatMessagePersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.MessageAttachmentPersistenceRecord;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.MessageAttachment;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.out.ConversationRepositoryPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

/**
 * 基于 MyBatis-Plus 持久化会话并按 Owner 查询有界历史。
 */
@Repository
public class MybatisPlusConversationRepository implements ConversationRepositoryPort {

    /**
     * 会话表映射器。
     */
    private final ConversationMapper conversationMapper;

    /**
     * 创建 {@code MybatisPlusConversationRepository} 实例。
     *
     * @param conversationMapper 会话表映射器。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected mapper is retained and not exposed")
    public MybatisPlusConversationRepository(final ConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }

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
        final ConversationPersistenceRecord record = new ConversationPersistenceRecord();
        record.setId(id.toString());
        record.setOwnerId(ownerId);
        record.setTitle(title);
        record.setCreatedAt(Timestamp.from(now));
        record.setUpdatedAt(Timestamp.from(now));
        requireOne(execute(() -> conversationMapper.insert(record), "failed to create conversation"),
                "failed to create conversation");
        return new Conversation(id, ownerId, title, now, now);
    }

    /**
     * 按所属用户查询会话列表。
     *
     * @param ownerId 用户所有者 ID。
     * @param beforeUpdatedAt 上一页末项更新时间，首页为空。
     * @param beforeId 上一页末项 ID，首页为空。
     * @param limit 数量上限。
     * @return 按所属用户查询会话列表。
     */
    @Override
    public List<Conversation> listByOwner(
            final String ownerId,
            final Instant beforeUpdatedAt,
            final UUID beforeId,
            final int limit) {
        final List<ConversationPersistenceRecord> records = execute(
                () -> conversationMapper.selectByOwner(
                        ownerId,
                        beforeUpdatedAt == null ? null : Timestamp.from(beforeUpdatedAt),
                        beforeId == null ? null : beforeId.toString(),
                        limit),
                "failed to list conversations");
        final List<Conversation> conversations = new ArrayList<>(records.size());
        for (final ConversationPersistenceRecord record : records) {
            conversations.add(toDomain(record));
        }
        return conversations;
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
        return execute(
                () -> conversationMapper.countActiveAnswers(ownerId, conversationId.toString()),
                "failed to inspect active answers") > 0;
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
        final LambdaQueryWrapper<ConversationPersistenceRecord> query =
                new LambdaQueryWrapper<ConversationPersistenceRecord>()
                        .eq(ConversationPersistenceRecord::getId, conversationId.toString())
                        .eq(ConversationPersistenceRecord::getOwnerId, ownerId)
                        .isNull(ConversationPersistenceRecord::getDeletedAt);
        return Optional.ofNullable(execute(
                        () -> conversationMapper.selectOne(query),
                        "failed to read conversation"))
                .map(MybatisPlusConversationRepository::toDomain);
    }

    /**
     * 在当前事务中锁定并读取有效会话。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @return 存在且归属匹配时返回加锁后的会话。
     */
    @Override
    public Optional<Conversation> findActiveForUpdate(
            final String ownerId,
            final UUID conversationId) {
        final ConversationPersistenceRecord record = execute(
                () -> conversationMapper.selectActiveForUpdate(
                        ownerId, conversationId.toString()),
                "failed to lock conversation");
        return record == null ? Optional.empty() : Optional.of(toDomain(record));
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
        final List<ChatMessagePersistenceRecord> records = new ArrayList<>(execute(
                () -> conversationMapper.selectMessages(ownerId, conversationId.toString(), limit),
                "failed to list messages"));
        Collections.reverse(records);
        final Map<String, List<MessageAttachment>> attachmentsByMessage = loadAttachments(
                ownerId, conversationId, records);
        final List<ChatMessage> messages = new ArrayList<>(records.size());
        for (final ChatMessagePersistenceRecord record : records) {
            messages.add(toDomain(record, attachmentsByMessage.getOrDefault(
                    record.getId(), Collections.<MessageAttachment>emptyList())));
        }
        return messages;
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
        final LambdaUpdateWrapper<ConversationPersistenceRecord> update = activeConversation(
                ownerId, conversationId)
                .set(ConversationPersistenceRecord::getTitle, title)
                .set(ConversationPersistenceRecord::getUpdatedAt, Timestamp.from(now));
        if (execute(() -> conversationMapper.update(null, update), "failed to rename conversation") == 0) {
            return Optional.empty();
        }
        return findActive(ownerId, conversationId);
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
        final Timestamp deletedAt = Timestamp.from(now);
        final LambdaUpdateWrapper<ConversationPersistenceRecord> update = activeConversation(
                ownerId, conversationId)
                .set(ConversationPersistenceRecord::getDeletedAt, deletedAt)
                .set(ConversationPersistenceRecord::getUpdatedAt, deletedAt);
        return execute(() -> conversationMapper.update(null, update), "failed to delete conversation") == 1;
    }

    /**
     * 处理当前有效会话查询条件。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @return 当前有效会话查询条件。
     */
    private LambdaUpdateWrapper<ConversationPersistenceRecord> activeConversation(
            final String ownerId,
            final UUID conversationId) {
        return new LambdaUpdateWrapper<ConversationPersistenceRecord>()
                .eq(ConversationPersistenceRecord::getId, conversationId.toString())
                .eq(ConversationPersistenceRecord::getOwnerId, ownerId)
                .isNull(ConversationPersistenceRecord::getDeletedAt);
    }

    /**
     * 执行停止任务的远程调用与状态收敛。
     *
     * @param operation 当前数据库操作名称。
     *
     * @param message 提示信息。
     *
     * @return 执行停止任务的远程调用与状态收敛。
     */
    private <T> T execute(final Supplier<T> operation, final String message) {
        try {
            return operation.get();
        } catch (final DataAccessException exception) {
            throw new PersistenceOperationException(message, exception);
        }
    }

    /**
     * 校验数据库操作恰好影响一行。
     *
     * @param affectedRows SQL 实际影响行数。
     *
     * @param message 提示信息。
     */
    private void requireOne(final int affectedRows, final String message) {
        if (affectedRows != 1) {
            throw new PersistenceOperationException(
                    message, new IllegalStateException("expected one affected row but was " + affectedRows));
        }
    }

    /**
     * 将数据库记录转换为领域对象。
     *
     * @param record 持久化记录。
     *
     * @return 将数据库记录转换为领域对象。
     */
    private static Conversation toDomain(final ConversationPersistenceRecord record) {
        return new Conversation(
                UUID.fromString(record.getId()),
                record.getOwnerId(),
                record.getTitle(),
                record.createdAtInstant(),
                record.updatedAtInstant());
    }

    /**
     * 将数据库记录转换为领域对象。
     *
     * @param record 持久化记录。
     *
     * @param attachments 随用户消息提交的附件元数据。
     *
     * @return 将数据库记录转换为领域对象。
     */
    private static ChatMessage toDomain(
            final ChatMessagePersistenceRecord record,
            final List<MessageAttachment> attachments) {
        final String answerId = record.getAnswerId();
        final String answerStatus = record.getAnswerStatus();
        return new ChatMessage(
                UUID.fromString(record.getId()),
                UUID.fromString(record.getConversationId()),
                answerId == null ? null : UUID.fromString(answerId),
                answerStatus == null ? null : AnswerSnapshot.Status.valueOf(answerStatus),
                ChatMessage.Role.valueOf(record.getRole()),
                record.getContent(),
                record.createdAtInstant(),
                attachments);
    }

    /**
     * 批量读取当前有界历史中用户消息的附件，避免逐条查询和越界加载。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param messages 已读取的有界历史消息。
     * @return 以用户消息 ID 为键的附件元数据。
     */
    private Map<String, List<MessageAttachment>> loadAttachments(
            final String ownerId,
            final UUID conversationId,
            final List<ChatMessagePersistenceRecord> messages) {
        final List<String> userMessageIds = new ArrayList<>();
        for (final ChatMessagePersistenceRecord message : messages) {
            if (ChatMessage.Role.USER.name().equals(message.getRole())) {
                userMessageIds.add(message.getId());
            }
        }
        if (userMessageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final List<MessageAttachmentPersistenceRecord> records = execute(
                () -> conversationMapper.selectMessageAttachments(
                        ownerId, conversationId.toString(), userMessageIds),
                "failed to list message attachments");
        final Map<String, List<MessageAttachment>> grouped = new LinkedHashMap<>();
        for (final MessageAttachmentPersistenceRecord record : records) {
            grouped.computeIfAbsent(record.getMessageId(), key -> new ArrayList<>())
                    .add(toDomain(record));
        }
        return grouped;
    }

    /**
     * 将附件联查记录转换为安全的领域元数据。
     *
     * @param record 附件联查记录。
     * @return 用户消息附件元数据。
     */
    private static MessageAttachment toDomain(final MessageAttachmentPersistenceRecord record) {
        return new MessageAttachment(
                UUID.fromString(record.getFileId()),
                record.getOriginalName(),
                record.getContentType(),
                record.getSizeBytes(),
                TemporaryFile.Usage.valueOf(record.getUsageType()),
                TemporaryFile.Status.valueOf(record.getStatus()));
    }
}
