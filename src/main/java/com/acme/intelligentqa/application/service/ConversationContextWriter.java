package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.ClarificationRequest;
import com.acme.intelligentqa.domain.model.ConversationContext;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.port.out.ConversationContextRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 以乐观版本保存可信实体来源和待追问状态。
 */
final class ConversationContextWriter {

    /** 会话上下文仓储。 */
    private final ConversationContextRepositoryPort contextRepository;
    /** 系统时钟。 */
    private final Clock clock;

    /**
     * 创建结构化会话上下文写入器。
     *
     * @param contextRepository 会话上下文仓储。
     * @param clock 系统时钟。
     */
    ConversationContextWriter(
            final ConversationContextRepositoryPort contextRepository,
            final Clock clock) {
        this.contextRepository = contextRepository;
        this.clock = clock;
    }

    /**
     * 合并并保存实体来源及待追问状态。
     *
     * @param ownerId 用户所有者 ID。
     * @param answer 回答快照。
     * @param previous 先前保存的结构化上下文。
     * @param intent 查询意图。
     * @param pending 尚未完成的上一轮追问。
     * @param currentEntitySources 本轮问题理解确认的逐实体来源消息 ID。
     */
    void save(
            final String ownerId,
            final AnswerSnapshot answer,
            final ConversationContext previous,
            final QueryIntent intent,
            final ClarificationRequest pending,
            final Map<String, UUID> currentEntitySources) {
        final long expectedVersion = previous == null ? 0L : previous.version();
        final Map<String, String> entities = new LinkedHashMap<>();
        final Map<String, UUID> entitySources = new LinkedHashMap<>();
        if (previous != null) {
            entities.putAll(previous.entities());
            entitySources.putAll(previous.entitySourceMessageIds());
        }
        mergeCurrent(intent, pending, currentEntitySources, entities, entitySources);
        final ConversationContext next = new ConversationContext(
                answer.conversationId(), ownerId, expectedVersion, intent.type(),
                entities, entitySources, answer.questionId(), pending, Instant.now(clock));
        if (!contextRepository.save(next, expectedVersion)) {
            throw new DependencyUnavailableException(
                    "CONVERSATION_CONTEXT_CONFLICT", "conversation context changed concurrently");
        }
    }

    /**
     * 只把具有可信来源的当前实体合并进长期上下文。
     *
     * @param intent 当前查询意图。
     * @param pending 当前待追问状态。
     * @param currentSources 当前可信来源映射。
     * @param entities 待更新实体值映射。
     * @param entitySources 待更新实体来源映射。
     */
    private void mergeCurrent(
            final QueryIntent intent,
            final ClarificationRequest pending,
            final Map<String, UUID> currentSources,
            final Map<String, String> entities,
            final Map<String, UUID> entitySources) {
        if (pending == null) {
            entities.putAll(intent.entities());
            entitySources.putAll(currentSources);
            return;
        }
        // 追问中的待补字段及歧义原文只能留在追问载荷，不能升级为长期可信实体。
        for (final Map.Entry<String, UUID> source : currentSources.entrySet()) {
            final String entityValue = intent.entities().get(source.getKey());
            if (entityValue != null) {
                entities.put(source.getKey(), entityValue);
                entitySources.put(source.getKey(), source.getValue());
            }
        }
    }
}
