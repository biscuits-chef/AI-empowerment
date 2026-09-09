package com.acme.intelligentqa.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 验证 DomainModel 的业务行为与边界。
 */
class DomainModelTest {

    /**
     * 固定测试时钟时间。
     */
    private static final Instant NOW = Instant.parse("2026-08-19T01:00:00Z");

    /**
     * 验证会话与消息对象的输入校验和字段读取。
     */
    @Test
    void exposesValidatedConversationAndMessageValues() {
        final UUID conversationId = UUID.randomUUID();
        final Conversation conversation = new Conversation(conversationId, "user-1", "问答", NOW, NOW);
        final ChatMessage message = new ChatMessage(
                UUID.randomUUID(), conversationId, null, ChatMessage.Role.USER, "问题", NOW);

        assertEquals(conversationId, conversation.id());
        assertEquals("user-1", conversation.ownerId());
        assertEquals("问答", conversation.title());
        assertEquals(NOW, conversation.createdAt());
        assertEquals(NOW, conversation.updatedAt());
        assertEquals(conversationId, message.conversationId());
        assertEquals(ChatMessage.Role.USER, message.role());
        assertEquals("问题", message.content());
        assertNull(message.answerId());
        assertEquals(NOW, message.createdAt());
        assertFalse(message.id().toString().isEmpty());
    }

    /**
     * 验证回答及事件对象字段正确暴露。
     */
    @Test
    void exposesAnswerAndEventValues() {
        final UUID answerId = UUID.randomUUID();
        final UUID conversationId = UUID.randomUUID();
        final UUID questionId = UUID.randomUUID();
        final UUID traceId = UUID.randomUUID();
        final AnswerSnapshot answer = new AnswerSnapshot(
                answerId, conversationId, questionId, traceId, null,
                AnswerSnapshot.Status.PENDING, null, null, NOW, null);
        final AnswerEvent event = new AnswerEvent(1, "delta", "内容", NOW);

        assertEquals(answerId, answer.id());
        assertEquals(conversationId, answer.conversationId());
        assertEquals(questionId, answer.questionId());
        assertEquals(traceId, answer.traceId());
        assertEquals(AnswerSnapshot.Status.PENDING, answer.status());
        assertEquals("", answer.content());
        assertNull(answer.errorCode());
        assertNull(answer.completedAt());
        assertNull(answer.regeneratedFromAnswerId());
        assertEquals(1, event.sequence());
        assertEquals("delta", event.type());
        assertEquals("内容", event.data());
        assertEquals(NOW, event.occurredAt());
    }

    /**
     * 验证领域模型的输入边界约束。
     */
    @Test
    void validatesDomainBoundaries() {
        assertThrows(IllegalArgumentException.class, () -> new Conversation(UUID.randomUUID(), "", "标题", NOW, NOW));
        assertThrows(IllegalArgumentException.class, () -> new Conversation(UUID.randomUUID(), "u", " ", NOW, NOW));
        assertThrows(IllegalArgumentException.class, () -> new AnswerEvent(0, "delta", "x", NOW));
        assertThrows(IllegalArgumentException.class, () -> new AnswerEvent(1, "", "x", NOW));
        assertThrows(NullPointerException.class, () -> new ChatMessage(
                UUID.randomUUID(), UUID.randomUUID(), null, null, "x", NOW));
    }

    /**
     * 验证模型生成上下文不会被调用方意外修改。
     */
    @Test
    void protectsGenerationContextFromMutation() {
        final KnowledgeChunk chunk = new KnowledgeChunk("doc-1", "制度", "内容");
        final BusinessFact fact = new BusinessFact("query-1", "事实");
        final List<KnowledgeChunk> knowledge = new ArrayList<>(Collections.singletonList(chunk));
        final QueryIntent intent = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 0.95D, Collections.emptyMap());
        final EvidenceAssessment assessment = new EvidenceAssessment(
                EvidenceAssessment.Status.CONSISTENT, Collections.emptyList());
        final LanguageModelPort.GenerationRequest request = new LanguageModelPort.GenerationRequest(
                "user-1", UUID.randomUUID(), "问题", intent, Collections.emptyMap(),
                assessment, Collections.emptyList(),
                knowledge, Collections.singletonList(fact));
        final LanguageModelPort.GenerationResult result = new LanguageModelPort.GenerationResult("model", "stop");

        knowledge.clear();
        assertEquals(1, request.knowledge().size());
        assertEquals("问题", request.question());
        assertEquals("user-1", request.ownerId());
        assertEquals(QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, request.intent().type());
        assertEquals(EvidenceAssessment.Status.CONSISTENT, request.evidenceAssessment().status());
        assertTrue(request.history().isEmpty());
        assertEquals("doc-1", request.knowledge().get(0).sourceId());
        assertEquals("制度", chunk.title());
        assertEquals("内容", chunk.content());
        assertEquals("query-1", fact.sourceCode());
        assertEquals("事实", request.businessFacts().get(0).content());
        assertEquals("model", result.modelCode());
        assertEquals("stop", result.finishReason());
        assertThrows(UnsupportedOperationException.class, () -> request.knowledge().clear());
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeChunk("", "x", "y"));
        assertThrows(IllegalArgumentException.class, () -> new BusinessFact("x", " "));
        assertThrows(IllegalArgumentException.class, () ->
                new QueryIntent(QueryIntent.Type.UNSUPPORTED, 1.1D, Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class, () ->
                new EvidenceAssessment(EvidenceAssessment.Status.CONSISTENT,
                        Collections.singletonList("费率")));
    }

    /**
     * 验证逐实体来源映射使用防御性副本且不能引用不存在的实体。
     */
    @Test
    void protectsConversationEntitySourcesFromMutationAndMismatch() {
        final UUID sourceMessageId = UUID.randomUUID();
        final Map<String, UUID> mutableSources = new LinkedHashMap<>();
        mutableSources.put("productReference", sourceMessageId);
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(),
                "user-1",
                0L,
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                Collections.singletonMap("productReference", "P001"),
                mutableSources,
                sourceMessageId,
                null,
                NOW);

        mutableSources.clear();

        assertEquals(sourceMessageId,
                context.entitySourceMessageIds().get("productReference"));
        assertThrows(UnsupportedOperationException.class,
                () -> context.entitySourceMessageIds().clear());
        assertThrows(IllegalArgumentException.class, () -> new ConversationContext(
                UUID.randomUUID(),
                "user-1",
                0L,
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                Collections.singletonMap("productReference", "P001"),
                Collections.singletonMap("tradeReference", sourceMessageId),
                sourceMessageId,
                null,
                NOW));
    }
}
