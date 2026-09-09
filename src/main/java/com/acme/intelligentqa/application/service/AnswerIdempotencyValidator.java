package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.common.error.IdempotencyConflictException;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.port.out.AnswerRepositoryPort;
import java.util.List;
import java.util.UUID;

/**
 * 对回答创建和重新生成请求执行持久化幂等指纹核对。
 */
final class AnswerIdempotencyValidator {

    /** 回答仓储。 */
    private final AnswerRepositoryPort answerRepository;
    /** 问题附件校验与关联协调器。 */
    private final QuestionFileCoordinator questionFileCoordinator;

    /**
     * 创建回答幂等指纹校验器。
     *
     * @param answerRepository 回答仓储。
     * @param questionFileCoordinator 问题附件校验与关联协调器。
     */
    AnswerIdempotencyValidator(
            final AnswerRepositoryPort answerRepository,
            final QuestionFileCoordinator questionFileCoordinator) {
        this.answerRepository = answerRepository;
        this.questionFileCoordinator = questionFileCoordinator;
    }

    /**
     * 校验重复提交与已持久化问题具有相同的业务请求指纹。
     *
     * @param ownerId 当前认证用户标识。
     * @param existing 幂等键已经关联的回答。
     * @param conversationId 当前请求的会话 ID。
     * @param question 当前请求的问题原文。
     * @param files 当前请求的附件用途快照。
     * @return 可安全复用的已有回答。
     */
    AnswerSnapshot requireMatchingSubmission(
            final String ownerId,
            final AnswerSnapshot existing,
            final UUID conversationId,
            final String question,
            final List<QuestionFileReference> files) {
        final String storedQuestion = answerRepository.findQuestion(ownerId, existing.id())
                .orElseThrow(() -> new DependencyUnavailableException(
                        "IDEMPOTENCY_RECORD_INCOMPLETE", "idempotent answer has no question"));
        if (!existing.conversationId().equals(conversationId)
                || existing.regeneratedFromAnswerId() != null
                || !storedQuestion.equals(question)
                || !questionFileCoordinator.matchesQuestionReferences(existing.questionId(), files)) {
            throw new IdempotencyConflictException();
        }
        return existing;
    }

    /**
     * 校验重复重新生成请求仍指向同一个原回答。
     *
     * @param existing 幂等键已经关联的回答。
     * @param original 当前请求指定的原回答。
     * @return 可安全复用的已有重新生成回答。
     */
    AnswerSnapshot requireMatchingRegeneration(
            final AnswerSnapshot existing,
            final AnswerSnapshot original) {
        if (!existing.conversationId().equals(original.conversationId())
                || !original.id().equals(existing.regeneratedFromAnswerId())) {
            throw new IdempotencyConflictException();
        }
        return existing;
    }
}
