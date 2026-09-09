package com.acme.intelligentqa.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.ClarificationRequest;
import com.acme.intelligentqa.domain.model.ConversationContext;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.model.QuestionUnderstanding;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 验证 QuestionUnderstandingService 的业务行为与边界。
 */
class QuestionUnderstandingServiceTest {

    /**
     * 验证未补充实体信号前持续保留意图追问。
     */
    @Test
    void keepsIntentClarificationUntilFollowUpProvidesAnEntitySignal() {
        final QueryIntent pendingIntent = new QueryIntent(
                QueryIntent.Type.UNSUPPORTED, 0.4D, Collections.emptyMap());
        final ClarificationRequest pending = new ClarificationRequest(
                ClarificationRequest.Reason.LOW_CONFIDENCE,
                null,
                "请补充查询对象和内容",
                pendingIntent,
                Collections.emptyList());
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(), "user-1", 1L, pendingIntent.type(),
                Collections.emptyMap(), Collections.emptyMap(), UUID.randomUUID(), pending, Instant.EPOCH);
        final QueryIntent noSignal = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());
        final QueryIntent explicitProduct = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D,
                Collections.singletonMap("productReference", "P001"));

        assertEquals(pending, service().resolve(
                "还是查一下", noSignal, context, UUID.randomUUID()).clarification());

        final QuestionUnderstanding newQuestion = service().resolve(
                "查询 P001 的信息", explicitProduct, context, UUID.randomUUID());
        assertTrue(!newQuestion.requiresClarification());
        assertEquals("P001", newQuestion.resolvedIntent().entities().get("productReference"));
    }

    /**
     * 验证后续回复仍含糊时保留候选追问。
     */
    @Test
    void keepsPendingCandidatesWhenFollowUpIsStillAmbiguous() {
        final QueryIntent pendingIntent = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());
        final ClarificationRequest pending = new ClarificationRequest(
                ClarificationRequest.Reason.AMBIGUOUS_ENTITY,
                "productReference",
                "请选择产品：\n1. 悦享一号\n2. 悦享二号",
                pendingIntent,
                Arrays.asList(
                        new EntityCandidate("P001", "悦享一号"),
                        new EntityCandidate("P002", "悦享二号")));
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(), "user-1", 1L, pendingIntent.type(),
                Collections.emptyMap(), Collections.emptyMap(), UUID.randomUUID(), pending, Instant.EPOCH);
        final QueryIntent newlyRecognized = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());

        final QuestionUnderstanding understanding = service().resolve(
                "那个", newlyRecognized, context, UUID.randomUUID());

        assertTrue(understanding.requiresClarification());
        assertEquals(pending, understanding.clarification());
        assertEquals(pending, service().resolve(
                "都不是", newlyRecognized, context, UUID.randomUUID()).clarification());
    }

    /**
     * 验证用户可用授权目录中的规范名称选择带代码显示标签的候选。
     */
    @Test
    void resolvesDecoratedCandidateByCanonicalName() {
        final QueryIntent pendingIntent = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());
        final ClarificationRequest pending = new ClarificationRequest(
                ClarificationRequest.Reason.AMBIGUOUS_ENTITY,
                "productReference",
                "请选择产品",
                pendingIntent,
                Arrays.asList(
                        new EntityCandidate("P001", "悦享一号（P001）", "悦享一号"),
                        new EntityCandidate("P002", "悦享二号（P002）", "悦享二号")));
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(), "user-1", 1L, pendingIntent.type(),
                Collections.emptyMap(), Collections.emptyMap(), UUID.randomUUID(), pending, Instant.EPOCH);

        final QuestionUnderstanding understanding = service().resolve(
                "悦享二号",
                new QueryIntent(QueryIntent.Type.UNSUPPORTED, 0.1D, Collections.emptyMap()),
                context,
                UUID.randomUUID());

        assertTrue(!understanding.requiresClarification());
        assertEquals("P002", understanding.resolvedIntent().entities().get("productReference"));
    }

    /**
     * 验证明确新实体可以放弃旧候选追问。
     */
    @Test
    void explicitNewEntityAbandonsOldCandidatePrompt() {
        final QueryIntent pendingIntent = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());
        final ClarificationRequest pending = new ClarificationRequest(
                ClarificationRequest.Reason.AMBIGUOUS_ENTITY,
                "productReference",
                "请选择产品",
                pendingIntent,
                Arrays.asList(
                        new EntityCandidate("P001", "悦享一号"),
                        new EntityCandidate("P002", "悦享二号")));
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(), "user-1", 1L, pendingIntent.type(),
                Collections.emptyMap(), Collections.emptyMap(), UUID.randomUUID(), pending, Instant.EPOCH);
        final QueryIntent explicit = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D,
                Collections.singletonMap("tradeReference", "T123"));

        final QuestionUnderstanding understanding = service().resolve(
                "查询交易 T123 的交易员", explicit, context, UUID.randomUUID());

        assertTrue(!understanding.requiresClarification());
        assertEquals("T123", understanding.resolvedIntent().entities().get("tradeReference"));
    }

    /**
     * 验证交易标识缺失被识别为补参而不是指代。
     */
    @Test
    void asksForMissingTradeReferenceWithoutTreatingItAsPronoun() {
        final QueryIntent recognized = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                1.0D,
                Collections.emptyMap(),
                Collections.singleton("tradeReference"),
                Collections.emptyMap());

        final QuestionUnderstanding understanding = service().resolve(
                "查询交易员", recognized, null, UUID.randomUUID());

        assertTrue(understanding.requiresClarification());
        assertEquals(ClarificationRequest.Reason.MISSING_REQUIRED_PARAMETER,
                understanding.clarification().reason());
        assertTrue(understanding.clarification().prompt().contains("还缺少交易"));
    }

    /**
     * 验证只有实体值但没有逐实体来源的遗留上下文不能用于指代消解。
     */
    @Test
    void rejectsContextEntityWithoutSourceMessage() {
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(), "user-1", 1L, QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                Collections.singletonMap("productReference", "P001"),
                Collections.emptyMap(), UUID.randomUUID(), null, Instant.EPOCH);
        final QueryIntent recognized = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                1.0D,
                Collections.emptyMap(),
                Collections.singleton("productReference"),
                Collections.emptyMap());

        final QuestionUnderstanding understanding = service().resolve(
                "它的费率呢？", recognized, context, UUID.randomUUID());

        assertTrue(understanding.requiresClarification());
        assertEquals(ClarificationRequest.Reason.REFERENCE_NOT_FOUND,
                understanding.clarification().reason());
        assertTrue(understanding.entitySourceMessageIds().isEmpty());
    }

    /**
     * 验证从可信上下文解析的指代沿用原实体来源，而不是冒充当前指代消息。
     */
    @Test
    void retainsOriginalSourceWhenResolvingPronoun() {
        final UUID originalMessageId = UUID.randomUUID();
        final UUID pronounMessageId = UUID.randomUUID();
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(), "user-1", 1L, QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                Collections.singletonMap("productReference", "P001"),
                Collections.singletonMap("productReference", originalMessageId),
                originalMessageId, null, Instant.EPOCH);
        final QueryIntent recognized = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                1.0D,
                Collections.emptyMap(),
                Collections.singleton("productReference"),
                Collections.emptyMap());

        final QuestionUnderstanding understanding = service().resolve(
                "它的费率呢？", recognized, context, pronounMessageId);

        assertTrue(!understanding.requiresClarification());
        assertEquals("P001", understanding.resolvedIntent().entities().get("productReference"));
        assertEquals(originalMessageId,
                understanding.entitySourceMessageIds().get("productReference"));
    }

    /**
     * 验证直接补充实体后恢复开放式追问的原始意图。
     */
    @Test
    void restoresOpenPromptIntentFromDirectEntityReply() {
        final QueryIntent pendingIntent = new QueryIntent(
                QueryIntent.Type.PRODUCT_LATEST_DOCUMENT_INFO,
                1.0D,
                Collections.emptyMap(),
                Collections.singleton("productReference"),
                Collections.emptyMap());
        final ClarificationRequest pending = new ClarificationRequest(
                ClarificationRequest.Reason.REFERENCE_NOT_FOUND,
                "productReference",
                "请提供产品名称",
                pendingIntent,
                Collections.emptyList());
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(), "user-1", 1L, pendingIntent.type(),
                Collections.emptyMap(), Collections.emptyMap(), UUID.randomUUID(), pending, Instant.EPOCH);
        final QueryIntent followUpRecognition = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());

        final QuestionUnderstanding understanding = service().resolve(
                "悦享三号", followUpRecognition, context, UUID.randomUUID());

        assertTrue(!understanding.requiresClarification());
        assertEquals(QueryIntent.Type.PRODUCT_LATEST_DOCUMENT_INFO,
                understanding.resolvedIntent().type());
        assertEquals("悦享三号", understanding.resolvedIntent().entities().get("productReference"));
    }

    /**
     * 验证明确定义的无实体日期列表问题会放弃上一轮候选追问。
     */
    @Test
    void selfContainedDateListQuestionAbandonsOldCandidatePrompt() {
        final QueryIntent pendingIntent = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());
        final ClarificationRequest pending = new ClarificationRequest(
                ClarificationRequest.Reason.AMBIGUOUS_ENTITY,
                "productReference",
                "请选择产品",
                pendingIntent,
                Arrays.asList(
                        new EntityCandidate("P001", "悦享一号"),
                        new EntityCandidate("P002", "悦享二号")));
        final ConversationContext context = new ConversationContext(
                UUID.randomUUID(), "user-1", 1L, pendingIntent.type(),
                Collections.emptyMap(), Collections.emptyMap(), UUID.randomUUID(), pending, Instant.EPOCH);
        final QueryIntent dateList = new QueryIntent(
                QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY, 1.0D, Collections.emptyMap());

        final QuestionUnderstanding understanding = service().resolve(
                "今日到期产品有哪些？", dateList, context, UUID.randomUUID());

        assertTrue(!understanding.requiresClarification());
        assertEquals(QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY,
                understanding.resolvedIntent().type());
    }

    /**
     * 验证非法日历日期在问题理解阶段失败关闭并要求用户重新提供日期。
     */
    @Test
    void asksForClarificationWhenBusinessDateIsNotARealCalendarDate() {
        final UUID currentMessageId = UUID.randomUUID();
        final QueryIntent invalidDate = new QueryIntent(
                QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY,
                1.0D,
                Collections.singletonMap("businessDate", "2026-02-30"));

        final QuestionUnderstanding understanding = service().resolve(
                "2026-02-30 到期产品有哪些？", invalidDate, null, currentMessageId);

        assertTrue(understanding.requiresClarification());
        assertEquals(ClarificationRequest.Reason.MISSING_REQUIRED_PARAMETER,
                understanding.clarification().reason());
        assertEquals("businessDate", understanding.clarification().entityName());
        assertTrue(understanding.clarification().prompt().contains("真实日历日期"));
        assertTrue(understanding.entitySourceMessageIds().isEmpty());
    }

    /**
     * 处理被测应用服务。
     *
     * @return 被测应用服务。
     */
    private QuestionUnderstandingService service() {
        return new QuestionUnderstandingService(new QaProperties(
                false, 4000, 30000, 8, 20000, 20000, 20, 0.8D, 512, 2, 8, 200));
    }
}
