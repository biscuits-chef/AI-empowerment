package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.common.error.IdempotencyConflictException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.AgentType;
import com.acme.intelligentqa.domain.port.out.ConversationRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 解析统一提问应创建新会话还是使用已有会话。
 */
final class QuestionSubmissionConversationResolver {

    /** 首次问题生成的会话标题最大 Unicode 字符数。 */
    private static final int MAXIMUM_TITLE_CODE_POINTS = 100;
    /** 会话仓储。 */
    private final ConversationRepositoryPort conversationRepository;
    /** 系统时钟。 */
    private final Clock clock;

    /**
     * 创建统一提问会话解析器。
     *
     * @param conversationRepository 会话仓储。
     * @param clock 系统时钟。
     */
    QuestionSubmissionConversationResolver(
            final ConversationRepositoryPort conversationRepository,
            final Clock clock) {
        this.conversationRepository = conversationRepository;
        this.clock = clock;
    }

    /**
     * 创建或锁定本次提问所属会话。
     *
     * @param ownerId 已校验的用户所有者 ID。
     * @param conversationId 已有会话 ID；首次提问时为空。
     * @param question 已校验的用户问题。
     * @param requestedAgentType 首次提问选择的 Agent 类型；已有会话可为空。
     * @param idempotencyKey 已校验的提问幂等键。
     * @return 会话、创建语义和统一业务时间。
     */
    Resolution resolve(
            final String ownerId,
            final UUID conversationId,
            final String question,
            final AgentType requestedAgentType,
            final String idempotencyKey) {
        final Instant now = Instant.now(clock);
        if (conversationId == null) {
            final AgentType agentType = Objects.requireNonNull(
                    requestedAgentType, "agentType must be provided for a new conversation");
            agentType.requireAvailable();
            final Conversation conversation = conversationRepository.createForQuestion(
                    UUID.randomUUID(), ownerId, title(question), agentType, idempotencyKey, now);
            requireUnchangedAgentType(conversation, agentType);
            return new Resolution(conversation, true, now);
        }
        final Conversation conversation = conversationRepository.findActiveForUpdate(ownerId, conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "conversation not found: " + conversationId));
        if (conversationRepository.findActiveByCreationKey(ownerId, idempotencyKey).isPresent()) {
            throw new IdempotencyConflictException();
        }
        if (requestedAgentType != null) {
            requireUnchangedAgentType(conversation, requestedAgentType);
        }
        return new Resolution(conversation, false, now);
    }

    /**
     * 校验请求没有尝试改变会话创建时确定的 Agent 类型。
     *
     * @param conversation 已持久化的会话。
     * @param requestedAgentType 请求携带的 Agent 类型。
     */
    private void requireUnchangedAgentType(
            final Conversation conversation,
            final AgentType requestedAgentType) {
        if (conversation.agentType() != requestedAgentType) {
            throw new IllegalArgumentException("agent type cannot be changed within a conversation");
        }
    }

    /**
     * 锁定已有会话并校验用户归属。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 已有会话 ID。
     * @return 已锁定并通过用户归属校验的会话。
     */
    Conversation lockExisting(final String ownerId, final UUID conversationId) {
        return conversationRepository.findActiveForUpdate(
                        ApplicationSupport.requireText(ownerId, "ownerId"),
                        Objects.requireNonNull(conversationId, "conversationId must not be null"))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "conversation not found: " + conversationId));
    }

    /**
     * 使用首次问题的前一百个 Unicode 字符生成会话名称。
     *
     * @param question 已通过校验的首次问题。
     * @return 去除首尾空白且不超过一百个 Unicode 字符的会话名称。
     */
    private String title(final String question) {
        final String value = question.trim();
        final int codePoints = value.codePointCount(0, value.length());
        if (codePoints <= MAXIMUM_TITLE_CODE_POINTS) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, MAXIMUM_TITLE_CODE_POINTS));
    }

    /**
     * 统一提问使用的会话解析结果。
     */
    static final class Resolution {
        /** 提问所属会话。 */
        private final Conversation conversation;
        /** 是否采用首次提问创建会话语义。 */
        private final boolean created;
        /** 会话与回答共享的业务时间。 */
        private final Instant now;

        /**
         * 创建会话解析结果。
         *
         * @param conversation 提问所属会话。
         * @param created 是否采用首次提问创建会话语义。
         * @param now 会话与回答共享的业务时间。
         */
        Resolution(final Conversation conversation, final boolean created, final Instant now) {
            this.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
            this.created = created;
            this.now = Objects.requireNonNull(now, "now must not be null");
        }

        /** @return 提问所属会话。 */
        Conversation conversation() { return conversation; }
        /** @return 采用首次提问创建会话语义时返回 true。 */
        boolean created() { return created; }
        /** @return 会话与回答共享的业务时间。 */
        Instant now() { return now; }
    }
}
