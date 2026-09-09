package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.in.QuestionAnswerUseCase;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * 保存回答内容、状态、幂等键和用户反馈的持久化端口。
 */
public interface AnswerRepositoryPort {

    /**
     * 按用户和幂等键查询已受理结果。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 按用户和幂等键查询已受理结果。
     */
    Optional<AnswerSnapshot> findByIdempotencyKey(String ownerId, String idempotencyKey);

    /**
     * 创建并持久化业务对象。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param questionId 问题 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param traceId 链路追踪 ID。
     *
     * @param regeneratedFromAnswerId 原回答 ID。
     *
     * @param question 用户问题。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param now 当前时间。
     *
     * @return 创建并持久化业务对象。
     */
    AnswerSnapshot create(
            String ownerId,
            UUID conversationId,
            UUID questionId,
            UUID answerId,
            UUID traceId,
            UUID regeneratedFromAnswerId,
            String question,
            String idempotencyKey,
            Instant now);

    /**
     * 查询满足用户隔离条件的目标记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @return 查询满足用户隔离条件的目标记录。
     */
    Optional<AnswerSnapshot> find(String ownerId, UUID answerId);

    /**
     * 读取回答对应的原始问题。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @return 读取回答对应的原始问题。
     */
    Optional<String> findQuestion(String ownerId, UUID answerId);

    /**
     * 按允许的源状态执行回答状态条件更新。
     *
     * @param answerId 回答 ID。
     *
     * @param status 业务状态。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean transitionStatus(UUID answerId, AnswerSnapshot.Status status);

    /**
     * 保存已生成的部分回答文本。
     *
     * @param answerId 回答 ID。
     *
     * @param content 内容。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean savePartial(UUID answerId, String content);

    /**
     * 保存最终内容并将回答转换为完成状态。
     *
     * @param answerId 回答 ID。
     *
     * @param content 内容。
     *
     * @param completedAt 完成时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean complete(UUID answerId, String content, Instant completedAt);

    /**
     * 保存确定性追问并将回答转换为待澄清状态。
     *
     * @param answerId 回答 ID。
     *
     * @param content 内容。
     *
     * @param completedAt 完成时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean clarify(UUID answerId, String content, Instant completedAt);

    /**
     * 记录失败或未完整状态，并保留已生成内容。
     *
     * @param answerId 回答 ID。
     *
     * @param status 业务状态。
     *
     * @param partialContent 已生成的部分内容。
     *
     * @param errorCode 错误码。
     *
     * @param completedAt 完成时间。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    boolean fail(
            UUID answerId,
            AnswerSnapshot.Status status,
            String partialContent,
            String errorCode,
            Instant completedAt);

    /**
     * 新增或覆盖用户对指定回答的反馈。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param feedback 用户反馈。
     *
     * @param now 当前时间。
     */
    void upsertFeedback(String ownerId, UUID answerId, QuestionAnswerUseCase.Feedback feedback, Instant now);
}
