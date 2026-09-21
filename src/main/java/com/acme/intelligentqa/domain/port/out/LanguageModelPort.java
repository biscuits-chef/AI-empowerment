package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.EvidenceAssessment;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.model.QueryIntent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 将已对账证据交给公司大模型进行语言整理并流式返回的出站端口。
 */
public interface LanguageModelPort {

    /**
     * 不触发取消的默认控制器。
     */
    GenerationControl NO_CANCELLATION = new NoCancellationControl();

    /**
     * 编排证据获取并流式生成回答。
     *
     * @param request 接口请求。
     *
     * @param chunkConsumer 回答文本增量消费回调。
     *
     * @return 编排证据获取并流式生成回答。
     */
    GenerationResult generate(GenerationRequest request, Consumer<String> chunkConsumer);

    /**
     * 编排证据获取并流式生成回答。
     *
     * @param request 接口请求。
     *
     * @param chunkConsumer 回答文本增量消费回调。
     *
     * @param control 生成取消状态控制器。
     *
     * @return 编排证据获取并流式生成回答。
     */
    default GenerationResult generate(
            final GenerationRequest request,
            final Consumer<String> chunkConsumer,
            final GenerationControl control) {
        Objects.requireNonNull(control, "control must not be null");
        return generate(request, chunk -> {
            if (control.isCancellationRequested()) {
                throw new com.acme.intelligentqa.common.error.GenerationCancelledException();
            }
            chunkConsumer.accept(chunk);
        });
    }

    /**
     * 生成过程中的停止检测和模型消息 ID 协调契约。
     */
    interface GenerationControl {
        /**
         * 判断回答是否已收到停止请求。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        boolean isCancellationRequested();

        /**
         * 持久化公司 HiAgent 应用会话 ID。
         *
         * @param appConversationId 公司 HiAgent 应用会话 ID。
         */
        default void onAppConversationId(final String appConversationId) { }

        /**
         * 持久化公司模型消息 ID 并唤醒停止任务。
         *
         * @param messageId 公司模型侧消息 ID。
         */
        void onMessageId(String messageId);
    }

    /**
     * 不启用停止检测的默认控制实现。
     */
    final class NoCancellationControl implements GenerationControl {
        /**
         * 判断回答是否已收到停止请求。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override
        public boolean isCancellationRequested() { return false; }

        /**
         * 持久化公司模型消息 ID 并唤醒停止任务。
         *
         * @param messageId 公司模型侧消息 ID。
         */
        @Override
        public void onMessageId(final String messageId) { }
    }

    /**
     * 携带历史及双通道证据的模型生成请求。
     */
    final class GenerationRequest {
        /**
         * 用户问题。
         */
        private final String question;
        /**
         * 用户所有者 ID。
         */
        private final String ownerId;
        /**
         * 会话 ID。
         */
        private final UUID conversationId;
        /**
         * 公司 HiAgent 应用会话 ID，可为空。
         */
        private final String appConversationId;
        /**
         * 查询意图。
         */
        private final QueryIntent intent;
        /**
         * 已确认实体对应的来源用户消息 ID。
         */
        private final Map<String, UUID> entitySourceMessageIds;
        /**
         * 双通道证据对账结果。
         */
        private final EvidenceAssessment evidenceAssessment;
        /**
         * 最近对话历史。
         */
        private final List<ChatMessage> history;
        /**
         * 知识库片段列表。
         */
        private final List<KnowledgeChunk> knowledge;
        /**
         * 业务事实列表。
         */
        private final List<BusinessFact> businessFacts;

        /**
         * 创建 {@code GenerationRequest} 实例。
         *
         * @param ownerId 用户所有者 ID。
         * @param conversationId 会话 ID。
         * @param appConversationId 公司 HiAgent 应用会话 ID，可为空。
         * @param question 用户问题。
         * @param intent 查询意图。
         * @param entitySourceMessageIds 已确认实体对应的来源用户消息 ID。
         * @param evidenceAssessment 双通道证据对账结果。
         * @param history 最近对话历史。
         * @param knowledge 知识库片段列表。
         * @param businessFacts 业务事实列表。
         */
        public GenerationRequest(
                final String ownerId,
                final UUID conversationId,
                final String appConversationId,
                final String question,
                final QueryIntent intent,
                final Map<String, UUID> entitySourceMessageIds,
                final EvidenceAssessment evidenceAssessment,
                final List<ChatMessage> history,
                final List<KnowledgeChunk> knowledge,
                final List<BusinessFact> businessFacts) {
            this.ownerId = requireText(ownerId, "ownerId");
            this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
            this.appConversationId = appConversationId == null || appConversationId.trim().isEmpty()
                    ? null
                    : appConversationId.trim();
            this.question = requireText(question, "question");
            this.intent = Objects.requireNonNull(intent, "intent must not be null");
            this.entitySourceMessageIds = immutableMap(entitySourceMessageIds, intent);
            this.evidenceAssessment = Objects.requireNonNull(
                    evidenceAssessment, "evidenceAssessment must not be null");
            this.history = immutableCopy(history);
            this.knowledge = immutableCopy(knowledge);
            this.businessFacts = immutableCopy(businessFacts);
        }

        /**
         * 创建 {@code GenerationRequest} 实例。
         *
         * @param ownerId 用户所有者 ID。
         * @param conversationId 会话 ID。
         * @param question 用户问题。
         * @param intent 查询意图。
         * @param entitySourceMessageIds 已确认实体对应的来源用户消息 ID。
         * @param evidenceAssessment 双通道证据对账结果。
         * @param history 最近对话历史。
         * @param knowledge 知识库片段列表。
         * @param businessFacts 业务事实列表。
         */
        public GenerationRequest(
                final String ownerId,
                final UUID conversationId,
                final String question,
                final QueryIntent intent,
                final Map<String, UUID> entitySourceMessageIds,
                final EvidenceAssessment evidenceAssessment,
                final List<ChatMessage> history,
                final List<KnowledgeChunk> knowledge,
                final List<BusinessFact> businessFacts) {
            this(ownerId, conversationId, null, question, intent, entitySourceMessageIds,
                    evidenceAssessment, history, knowledge, businessFacts);
        }

        /**
         * 返回用户问题。
         *
         * @return 用户问题。
         */
        public String question() { return question; }
        /**
         * 返回用户所有者 ID。
         *
         * @return 用户所有者 ID。
         */
        public String ownerId() { return ownerId; }
        /**
         * 返回会话 ID。
         *
         * @return 会话 ID。
         */
        public UUID conversationId() { return conversationId; }
        /**
         * 返回公司 HiAgent 应用会话 ID。
         *
         * @return 公司 HiAgent 应用会话 ID，未指定时返回 null。
         */
        public String appConversationId() { return appConversationId; }
        /**
         * 返回查询意图。
         *
         * @return 查询意图。
         */
        public QueryIntent intent() { return intent; }
        /**
         * 返回已确认实体对应的来源用户消息 ID。
         *
         * @return 不可变的实体来源消息 ID 映射。
         */
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Map is an unmodifiable defensive copy")
        public Map<String, UUID> entitySourceMessageIds() { return entitySourceMessageIds; }
        /**
         * 处理双通道证据对账结果。
         *
         * @return 双通道证据对账结果。
         */
        public EvidenceAssessment evidenceAssessment() { return evidenceAssessment; }

        /**
         * 返回最近对话历史。
         *
         * @return 最近对话历史。
         */
        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP",
                justification = "The list is an unmodifiable defensive copy")
        public List<ChatMessage> history() { return history; }
        /**
         * 处理知识库片段列表。
         *
         * @return 知识库片段列表。
         */
        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP",
                justification = "The list is an unmodifiable defensive copy")
        public List<KnowledgeChunk> knowledge() { return knowledge; }

        /**
         * 返回业务事实列表。
         *
         * @return 业务事实列表。
         */
        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP",
                justification = "The list is an unmodifiable defensive copy")
        public List<BusinessFact> businessFacts() { return businessFacts; }

        /**
         * 创建不可修改的集合副本。
         *
         * @param source 证据来源。
         *
         * @return 创建不可修改的集合副本。
         */
        private static <T> List<T> immutableCopy(final List<T> source) {
            final List<T> copy = new ArrayList<>(Objects.requireNonNull(source, "source must not be null"));
            return Collections.unmodifiableList(copy);
        }

        /**
         * 校验模型请求中的每个已确认实体都具有来源消息 ID。
         *
         * @param source 实体来源消息 ID 映射。
         * @param intent 已完成消歧的查询意图。
         * @return 经过校验的不可变来源映射。
         */
        private static Map<String, UUID> immutableMap(
                final Map<String, UUID> source,
                final QueryIntent intent) {
            final Map<String, UUID> copy = new LinkedHashMap<>(
                    Objects.requireNonNull(source, "entitySourceMessageIds must not be null"));
            if (!copy.keySet().containsAll(intent.entities().keySet())
                    || !intent.entities().keySet().containsAll(copy.keySet())
                    || copy.containsValue(null)) {
                throw new IllegalArgumentException(
                        "entity source message ids must match confirmed intent entities");
            }
            return Collections.unmodifiableMap(copy);
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
            Objects.requireNonNull(value, field + " must not be null");
            if (value.trim().isEmpty()) {
                throw new IllegalArgumentException(field + " must not be blank");
            }
            return value;
        }
    }

    /**
     * 模型生成结束信息。
     */
    final class GenerationResult {
        /**
         * 公司模型返回的错误码。
         */
        private final String modelCode;
        /**
         * 模型结束生成的原因。
         */
        private final String finishReason;

        /**
         * 创建 {@code GenerationResult} 实例。
         *
         * @param modelCode 公司模型返回的错误码。
         *
         * @param finishReason 模型结束生成的原因。
         */
        public GenerationResult(final String modelCode, final String finishReason) {
            this.modelCode = Objects.requireNonNull(modelCode, "modelCode must not be null");
            this.finishReason = Objects.requireNonNull(finishReason, "finishReason must not be null");
        }

        /**
         * 处理公司模型返回的错误码。
         *
         * @return 公司模型返回的错误码。
         */
        public String modelCode() { return modelCode; }
        /**
         * 处理模型结束生成的原因。
         *
         * @return 模型结束生成的原因。
         */
        public String finishReason() { return finishReason; }
    }
}
