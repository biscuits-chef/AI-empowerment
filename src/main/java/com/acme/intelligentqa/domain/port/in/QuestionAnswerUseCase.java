package com.acme.intelligentqa.domain.port.in;

import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.AgentType;
import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.model.QuestionSubmission;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 问题提交、回答读取、重生成和反馈的入站用例。
 */
public interface QuestionAnswerUseCase {

    /**
     * 使用一个接口提交首次或后续问题。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID；首次提问时为空。
     * @param agentType 用户选择的 Agent 类型。
     * @param question 用户问题。
     * @param files 本次问题引用的临时文件；第一阶段必须为空。
     * @param idempotencyKey 覆盖会话、问题和回答创建的幂等键。
     * @return 同一事务内持久化的会话与回答结果。
     */
    QuestionSubmission submitQuestion(
            String ownerId,
            UUID conversationId,
            AgentType agentType,
            String question,
            List<QuestionFileReference> files,
            String idempotencyKey);

    /**
     * 用户对回答的评价类型。
     */
    enum Feedback {
        /**
         * 用户喜欢该回答。
         */
        LIKE,
        /**
         * 用户不喜欢该回答。
         */
        DISLIKE
    }

    /**
     * 校验输入后提交当前问题。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @param question 用户问题。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 校验输入后提交当前问题。
     */
    AnswerSnapshot submit(String ownerId, UUID conversationId, String question, String idempotencyKey);

    /**
     * 校验附件引用后提交当前问题。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param question 用户问题。
     * @param files 本次问题引用的临时文件。
     * @param idempotencyKey 幂等键。
     * @return 问题受理后的回答快照。
     */
    default AnswerSnapshot submit(
            final String ownerId,
            final UUID conversationId,
            final String question,
            final List<QuestionFileReference> files,
            final String idempotencyKey) {
        if (files != null && !files.isEmpty()) {
            throw new UnsupportedOperationException("file references are not supported by this use case");
        }
        return submit(ownerId, conversationId, question, idempotencyKey);
    }

    /**
     * 按前端选择的 Agent 类型提交当前问题。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param agentType 用户选择的 Agent 类型。
     * @param question 用户问题。
     * @param files 本次问题引用的临时文件。
     * @param idempotencyKey 幂等键。
     * @return 问题受理后的回答快照。
     */
    default AnswerSnapshot submit(
            final String ownerId,
            final UUID conversationId,
            final AgentType agentType,
            final String question,
            final List<QuestionFileReference> files,
            final String idempotencyKey) {
        if (agentType != AgentType.SMART_DATA) {
            throw new IllegalArgumentException("agent type is not available: " + agentType);
        }
        return submit(ownerId, conversationId, question, files, idempotencyKey);
    }

    /**
     * 将原问题作为新的完整问答轮次重新发起并保留历史问答。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 新问答轮次的回答快照。
     */
    AnswerSnapshot regenerate(String ownerId, UUID answerId, String idempotencyKey);

    /**
     * 读取回答的持久化快照。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @return 回答快照。
     */
    AnswerSnapshot getAnswer(String ownerId, UUID answerId);

    /**
     * 保存用户对回答的评价。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param feedback 用户反馈。
     */
    void recordFeedback(String ownerId, UUID answerId, Feedback feedback);

    /**
     * 从指定序号订阅回答事件。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param afterSequence 最后确认的事件序号。
     *
     * @param consumer 事件订阅回调。
     *
     * @return 从指定序号订阅回答事件。
     */
    Subscription subscribe(String ownerId, UUID answerId, long afterSequence, Consumer<AnswerEvent> consumer);

    /**
     * 可关闭的回答事件订阅句柄。
     */
    interface Subscription {
        /**
         * 移除当前事件订阅并释放资源。
         */
        void close();
    }
}
