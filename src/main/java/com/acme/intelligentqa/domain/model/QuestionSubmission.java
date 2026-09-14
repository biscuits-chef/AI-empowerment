package com.acme.intelligentqa.domain.model;

import java.util.Objects;

/**
 * 一次统一提问操作返回的会话与回答受理结果。
 */
public final class QuestionSubmission {

    /** 提问所属的会话。 */
    private final Conversation conversation;
    /** 已经持久化的回答受理快照。 */
    private final AnswerSnapshot answer;
    /** 本次操作是否采用首次提问创建会话语义。 */
    private final boolean conversationCreated;

    /**
     * 创建统一提问结果。
     *
     * @param conversation 提问所属的会话。
     * @param answer 已经持久化的回答受理快照。
     * @param conversationCreated 本次操作是否采用首次提问创建会话语义。
     */
    public QuestionSubmission(
            final Conversation conversation,
            final AnswerSnapshot answer,
            final boolean conversationCreated) {
        this.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
        this.answer = Objects.requireNonNull(answer, "answer must not be null");
        this.conversationCreated = conversationCreated;
        if (!conversation.id().equals(answer.conversationId())) {
            throw new IllegalArgumentException("conversation and answer must belong together");
        }
    }

    /** @return 提问所属的会话。 */
    public Conversation conversation() { return conversation; }

    /** @return 已经持久化的回答受理快照。 */
    public AnswerSnapshot answer() { return answer; }

    /** @return 本次操作采用首次提问创建会话语义时返回 true。 */
    public boolean conversationCreated() { return conversationCreated; }
}
