package com.acme.intelligentqa.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.acme.intelligentqa.application.workflow.QuestionWorkflowEngine;
import com.acme.intelligentqa.application.workflow.ScenarioPlanRegistry;
import com.acme.intelligentqa.application.workflow.node.AnswerGenerationWorkflowNode;
import com.acme.intelligentqa.application.workflow.node.BusinessDataQueryWorkflowNode;
import com.acme.intelligentqa.application.workflow.node.EvidenceReconciliationWorkflowNode;
import com.acme.intelligentqa.application.workflow.node.KnowledgeRetrievalWorkflowNode;
import com.acme.intelligentqa.application.workflow.node.QuestionUnderstandingWorkflowNode;
import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.common.error.AnswerAlreadyTerminalException;
import com.acme.intelligentqa.common.error.IdempotencyConflictException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.config.CancellationProperties;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.AgentType;
import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.ConversationContext;
import com.acme.intelligentqa.domain.model.EvidenceAssessment;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.model.QuestionSubmission;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.in.QuestionAnswerUseCase;
import com.acme.intelligentqa.domain.port.in.AnswerCancellationUseCase;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import com.acme.intelligentqa.domain.port.out.AnswerRepositoryPort;
import com.acme.intelligentqa.domain.port.out.BusinessDataQueryPort;
import com.acme.intelligentqa.domain.port.out.CancellationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.ConversationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.ConversationContextRepositoryPort;
import com.acme.intelligentqa.domain.port.out.EvidenceReconciliationPort;
import com.acme.intelligentqa.domain.port.out.IntentRecognitionPort;
import com.acme.intelligentqa.domain.port.out.KnowledgeRetrievalPort;
import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import com.acme.intelligentqa.domain.port.out.ProductEntityResolutionPort;
import com.acme.intelligentqa.domain.port.out.TemporaryFileRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 验证 QuestionAnswerService 的业务行为与边界。
 */
class QuestionAnswerServiceTest {

    /**
     * 固定测试时钟时间。
     */
    private static final Instant NOW = Instant.parse("2026-08-19T01:00:00Z");
    /**
     * 测试用户标识。
     */
    private static final String OWNER = "user-1";
    /**
     * 会话 ID。
     */
    private UUID conversationId;
    /**
     * 测试会话集合。
     */
    private FakeConversationRepository conversations;
    /**
     * 测试回答仓储中的记录集合。
     */
    private FakeAnswerRepository answers;
    /**
     * 测试结构化上下文存储。
     */
    private FakeConversationContextRepository contexts;
    /**
     * 回答事件列表。
     */
    private FakeEventPort events;
    /** 临时文件元数据和问题关联仓储。 */
    private TemporaryFileRepositoryPort temporaryFiles;
    /** 产品实体解析测试替身。 */
    private ProductEntityResolutionPort productEntityResolution;

    /**
     * 初始化每个测试使用的隔离环境。
     */
    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        conversations = new FakeConversationRepository(
                new Conversation(conversationId, OWNER, "问答", NOW, NOW));
        answers = new FakeAnswerRepository();
        contexts = new FakeConversationContextRepository();
        events = new FakeEventPort();
        temporaryFiles = mock(TemporaryFileRepositoryPort.class);
        productEntityResolution = (owner, reference) -> Collections.singletonList(
                new EntityCandidate(reference, reference));
    }

    /**
     * 验证首次提问原子创建会话与回答，并按同一个幂等键复用完整结果。
     */
    @Test
    void createsConversationWithFirstQuestionAndReusesIdempotentResult() {
        final QuestionAnswerService service = service(
                (owner, question, limit) -> Collections.emptyList(),
                (owner, question, intent, limit) -> Collections.emptyList(),
                (request, consumer) -> new LanguageModelPort.GenerationResult("model", "stop"),
                properties(100));

        final QuestionSubmission first = service.submitQuestion(
                OWNER, null, AgentType.SMART_DATA, "悦享三号的投资经理是谁？",
                Collections.emptyList(), "unified-first-key");
        final QuestionSubmission replayed = service.submitQuestion(
                OWNER, null, AgentType.SMART_DATA, "悦享三号的投资经理是谁？",
                Collections.emptyList(), "unified-first-key");

        assertTrue(first.conversationCreated());
        assertEquals("悦享三号的投资经理是谁？", first.conversation().title());
        assertEquals(AgentType.SMART_DATA, first.conversation().agentType());
        assertEquals(first.conversation().id(), first.answer().conversationId());
        assertEquals(first.conversation().id(), replayed.conversation().id());
        assertEquals(first.answer().id(), replayed.answer().id());
        assertEquals(2, conversations.size());
        assertEquals(1, answers.createdCount);
    }

    /**
     * 验证后续问题继续使用已有会话，不会创建第二个会话。
     */
    @Test
    void submitsFollowUpToExistingConversationWithoutCreatingConversation() {
        final QuestionAnswerService service = service(
                (owner, question, limit) -> Collections.emptyList(),
                (owner, question, intent, limit) -> Collections.emptyList(),
                (request, consumer) -> new LanguageModelPort.GenerationResult("model", "stop"),
                properties(100));

        final QuestionSubmission submission = service.submitQuestion(
                OWNER, conversationId, null, "那产品经理呢？",
                Collections.emptyList(), "unified-follow-up-key");

        assertFalse(submission.conversationCreated());
        assertEquals(conversationId, submission.conversation().id());
        assertEquals(conversationId, submission.answer().conversationId());
        assertEquals(1, conversations.size());
    }

    /**
     * 验证已有会话不能通过后续问题修改首次选定的 Agent 类型。
     */
    @Test
    void rejectsChangingAgentTypeWithinExistingConversation() {
        final QuestionAnswerService service = service(
                (owner, question, limit) -> Collections.emptyList(),
                (owner, question, intent, limit) -> Collections.emptyList(),
                (request, consumer) -> new LanguageModelPort.GenerationResult("model", "stop"),
                properties(100));

        assertThrows(IllegalArgumentException.class, () -> service.submitQuestion(
                OWNER, conversationId, AgentType.CONTRACT_REVIEW, "审核合同",
                Collections.emptyList(), "agent-change-key"));
        assertEquals(0, answers.createdCount);
    }

    /**
     * 验证第一阶段在应用服务边界拒绝附件引用。
     */
    @Test
    void rejectsFileReferencesInPhaseOne() {
        final UUID fileId = UUID.randomUUID();
        final QuestionFileReference reference = new QuestionFileReference(
                fileId, TemporaryFile.Usage.QUERY_INPUT);
        final QuestionAnswerService service = service(
                (owner, question, limit) -> Collections.emptyList(),
                (owner, question, intent, limit) -> Collections.emptyList(),
                (request, consumer) -> new LanguageModelPort.GenerationResult("model", "stop"),
                properties(100));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.submit(
                OWNER, conversationId, "查询附件中的产品", Collections.singletonList(reference), "file-key"));

        assertEquals("第一阶段不支持文件上传", exception.getMessage());
        verifyNoInteractions(temporaryFiles);
    }

    /**
     * 验证问答执行顺序和幂等结果复用。
     */
    @Test
    void generatesAnswerInRequiredOrderAndEnforcesIdempotentFingerprint() {
        final List<String> calls = new ArrayList<>();
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> {
            calls.add("knowledge");
            return Collections.singletonList(new KnowledgeChunk("doc-1", "制度", "知识"));
        };
        final BusinessDataQueryPort business = (owner, question, chunks, limit) -> {
            calls.add("business");
            return Collections.singletonList(new BusinessFact("query-1", "事实"));
        };
        final LanguageModelPort model = (request, consumer) -> {
            calls.add("model");
            consumer.accept("完整");
            consumer.accept("回答");
            return new LanguageModelPort.GenerationResult("model-1", "stop");
        };
        final QuestionAnswerService service = service(knowledge, business, model, properties(100));

        final AnswerSnapshot submitted = service.submit(OWNER, conversationId, "问题", "key-1");
        final AnswerSnapshot completed = answers.find(OWNER, submitted.id()).orElseThrow(AssertionError::new);
        final AnswerSnapshot duplicate = service.submit(OWNER, conversationId, "问题", "key-1");

        assertEquals(AnswerSnapshot.Status.COMPLETED, completed.status());
        assertEquals("完整回答", completed.content());
        assertEquals(submitted.id(), duplicate.id());
        assertThrows(IdempotencyConflictException.class, () -> service.submit(
                OWNER, conversationId, "不同问题", "key-1"));
        assertEquals(1, answers.createdCount);
        assertEquals("knowledge", calls.get(0));
        assertEquals("business", calls.get(1));
        assertEquals("model", calls.get(2));
        assertEquals("DUAL_CHANNEL_QA|1", events.value("workflow_plan_selected"));
        assertTrue(events.value("intent_recognized").startsWith("PRODUCT_TRADE_BASIC_INFO|"));
        assertEquals("1", events.value("knowledge_retrieval_completed"));
        assertEquals("1", events.value("business_query_completed"));
        assertEquals("CONSISTENT|0", events.value("evidence_assessed"));
        assertEquals("READY_TO_PERSIST", events.value("generation_completed"));
        assertTrue(events.types().contains("citation"));
        assertTrue(events.types().contains("completed"));
    }

    /**
     * 验证输出前后失败被标记为不同的正确终态。
     */
    @Test
    void marksFailuresBeforeAndAfterPartialOutput() {
        final KnowledgeRetrievalPort unavailable = (owner, question, limit) -> {
            throw new DependencyUnavailableException("KNOWLEDGE_TIMEOUT", "timeout");
        };
        final BusinessDataQueryPort emptyBusiness = (owner, question, chunks, limit) -> Collections.emptyList();
        final LanguageModelPort unusedModel = (request, consumer) -> {
            throw new AssertionError("model must not be called");
        };
        final QuestionAnswerService failedService = service(
                unavailable, emptyBusiness, unusedModel, properties(100));

        final AnswerSnapshot failed = failedService.submit(OWNER, conversationId, "问题", "key-failed");
        assertEquals(AnswerSnapshot.Status.FAILED, answers.find(OWNER, failed.id()).get().status());
        assertEquals("KNOWLEDGE_TIMEOUT", answers.find(OWNER, failed.id()).get().errorCode());

        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> Collections.emptyList();
        final LanguageModelPort interruptedModel = (request, consumer) -> {
            consumer.accept("部分");
            throw new DependencyUnavailableException("LLM_STREAM_INTERRUPTED", "interrupted");
        };
        final QuestionAnswerService incompleteService = service(
                knowledge, emptyBusiness, interruptedModel, properties(100));
        final AnswerSnapshot incomplete = incompleteService.submit(
                OWNER, conversationId, "问题", "key-incomplete");

        assertEquals(AnswerSnapshot.Status.INCOMPLETE, answers.find(OWNER, incomplete.id()).get().status());
        assertEquals("部分", answers.find(OWNER, incomplete.id()).get().content());
        assertEquals("LLM_STREAM_INTERRUPTED", answers.find(OWNER, incomplete.id()).get().errorCode());
    }

    /**
     * 验证请求边界限制和用户归属隔离。
     */
    @Test
    void enforcesLimitsAndOwnership() {
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> Collections.emptyList();
        final BusinessDataQueryPort business = (owner, question, chunks, limit) -> Collections.emptyList();
        final LanguageModelPort longModel = (request, consumer) -> {
            consumer.accept("123456");
            return new LanguageModelPort.GenerationResult("model", "stop");
        };
        final QuestionAnswerService service = service(knowledge, business, longModel, properties(5, 5));

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "问题", "limit-key");
        assertEquals(AnswerSnapshot.Status.FAILED, answers.find(OWNER, answer.id()).get().status());
        assertEquals("ANSWER_LIMIT_EXCEEDED", answers.find(OWNER, answer.id()).get().errorCode());
        assertThrows(ResourceNotFoundException.class, () -> service.submit(
                "other-user", conversationId, "问题", "key"));
        assertThrows(IllegalArgumentException.class, () -> service.submit(OWNER, conversationId, "", "key"));
        assertThrows(IllegalArgumentException.class, () -> service.submit(OWNER, conversationId, "123456", "key"));
        assertThrows(IllegalArgumentException.class, () -> service.submit(OWNER, conversationId, "问题", ""));
        assertThrows(IllegalArgumentException.class, () -> service.submit(
                OWNER, conversationId, AgentType.CONTRACT_REVIEW, "问题", Collections.emptyList(), "agent-key"));
    }

    /**
     * 验证重新生成只复用原问题文本，不会复制历史附件。
     */
    @Test
    void regeneratesRecordsFeedbackAndSubscribes() {
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> Collections.emptyList();
        final BusinessDataQueryPort business = (owner, question, chunks, limit) -> Collections.emptyList();
        final LanguageModelPort model = (request, consumer) -> {
            consumer.accept("答案");
            return new LanguageModelPort.GenerationResult("model", "stop");
        };
        final QuestionAnswerService service = service(knowledge, business, model, properties(100));
        final AnswerSnapshot first = service.submit(OWNER, conversationId, "原问题", "first");
        service.recordFeedback(OWNER, first.id(), QuestionAnswerUseCase.Feedback.LIKE);
        final AnswerSnapshot regenerated = service.regenerate(OWNER, first.id(), "second");
        final AnswerSnapshot replayed = service.regenerate(OWNER, first.id(), "second");
        final List<AnswerEvent> received = new ArrayList<>();
        final QuestionAnswerUseCase.Subscription subscription = service.subscribe(
                OWNER, regenerated.id(), 0, received::add);
        subscription.close();

        assertEquals(QuestionAnswerUseCase.Feedback.LIKE, answers.feedback.get(first.id()));
        assertEquals(first.id(), regenerated.regeneratedFromAnswerId());
        assertEquals(regenerated.id(), replayed.id());
        assertNotEquals(first.id(), regenerated.id());
        assertNotEquals(first.questionId(), regenerated.questionId());
        assertEquals("原问题", answers.findQuestion(OWNER, regenerated.id()).get());
        verify(temporaryFiles, never()).listQuestionReferences(first.questionId());
        assertTrue(received.size() > 0);
        assertEquals(regenerated.id(), service.getAnswer(OWNER, regenerated.id()).id());
        assertThrows(IllegalArgumentException.class, () -> service.subscribe(
                OWNER, regenerated.id(), -1, event -> { }));
        assertThrows(ResourceNotFoundException.class, () -> service.getAnswer(OWNER, UUID.randomUUID()));
        assertThrows(ResourceNotFoundException.class, () -> service.regenerate(
                OWNER, UUID.randomUUID(), "missing"));
        assertThrows(IdempotencyConflictException.class, () -> service.regenerate(
                OWNER, first.id(), "first"));
    }

    /**
     * 验证生成任务只在事务提交后启动。
     */
    @Test
    void startsGenerationOnlyAfterTransactionCommit() {
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> Collections.emptyList();
        final BusinessDataQueryPort business = (owner, question, chunks, limit) -> Collections.emptyList();
        final LanguageModelPort model = (request, consumer) -> {
            consumer.accept("事务后回答");
            return new LanguageModelPort.GenerationResult("model", "stop");
        };
        final QuestionAnswerService service = service(knowledge, business, model, properties(100));

        TransactionSynchronizationManager.initSynchronization();
        try {
            final AnswerSnapshot answer = service.submit(OWNER, conversationId, "问题", "after-commit");
            assertEquals(AnswerSnapshot.Status.PENDING, answers.find(OWNER, answer.id()).get().status());
            final List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationManager.clearSynchronization();
            synchronizations.forEach(TransactionSynchronization::afterCommit);
            assertEquals(AnswerSnapshot.Status.COMPLETED, answers.find(OWNER, answer.id()).get().status());
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    /**
     * 验证线程池拒绝生成任务时记录过载错误。
     */
    @Test
    void recordsOverloadWhenGenerationQueueRejectsWork() {
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> Collections.emptyList();
        final BusinessDataQueryPort business = (owner, question, chunks, limit) -> Collections.emptyList();
        final LanguageModelPort model = (request, consumer) ->
                new LanguageModelPort.GenerationResult("model", "stop");
        final Executor rejectingExecutor = task -> {
            throw new RejectedExecutionException("full");
        };
        final QuestionAnswerService service = service(
                knowledge, business, model, properties(100), rejectingExecutor);

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "问题", "overloaded");

        assertEquals(AnswerSnapshot.Status.FAILED, answers.find(OWNER, answer.id()).get().status());
        assertEquals("SYSTEM_OVERLOADED", answers.find(OWNER, answer.id()).get().errorCode());
    }

    /**
     * 验证未预期运行时异常能转换为回答终态。
     */
    @Test
    void convertsUnexpectedRuntimeFailureToTerminalState() {
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> Collections.emptyList();
        final BusinessDataQueryPort business = (owner, question, chunks, limit) -> Collections.emptyList();
        final LanguageModelPort model = (request, consumer) -> {
            consumer.accept("部分");
            throw new IllegalStateException("unexpected provider failure");
        };
        final QuestionAnswerService service = service(knowledge, business, model, properties(100));

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "问题", "unexpected");

        assertEquals(AnswerSnapshot.Status.INCOMPLETE, answers.find(OWNER, answer.id()).get().status());
        assertEquals("GENERATION_FAILED", answers.find(OWNER, answer.id()).get().errorCode());
        assertTrue(events.types().contains("error"));
    }

    /**
     * 验证意图识别和模型生成都收到有效历史。
     */
    @Test
    void includesConversationHistoryInIntentRecognitionAndGeneration() {
        final ChatMessage previous = new ChatMessage(
                UUID.randomUUID(), conversationId, null, ChatMessage.Role.USER, "上一轮产品问题", NOW);
        conversations.addHistory(previous);
        final AtomicReference<List<ChatMessage>> recognizedHistory = new AtomicReference<>();
        final AtomicReference<LanguageModelPort.GenerationRequest> generatedRequest = new AtomicReference<>();
        final IntentRecognitionPort intent = (owner, question, history) -> {
            recognizedHistory.set(history);
            return new QueryIntent(
                    QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                    1.0D,
                    Collections.singletonMap("productReference", "P001"));
        };
        final EvidenceReconciliationPort reconciliation = (recognized, knowledge, facts) -> consistent();
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) ->
                Collections.singletonList(new KnowledgeChunk("doc-1", "产品资料", "知识"));
        final BusinessDataQueryPort business = (owner, question, recognized, limit) ->
                Collections.singletonList(new BusinessFact("product-query", "事实"));
        final LanguageModelPort model = (request, consumer) -> {
            generatedRequest.set(request);
            consumer.accept("回答");
            return new LanguageModelPort.GenerationResult("model", "stop");
        };
        final QuestionAnswerService service = service(
                knowledge, business, intent, reconciliation, model, properties(100), Runnable::run);

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "查费率", "history");

        assertEquals("上一轮产品问题", recognizedHistory.get().get(0).content());
        assertEquals("上一轮产品问题", generatedRequest.get().history().get(0).content());
        assertEquals(QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, generatedRequest.get().intent().type());
        assertTrue(generatedRequest.get().question().contains("productReference=P001"));
        assertEquals(answer.questionId(),
                generatedRequest.get().entitySourceMessageIds().get("productReference"));
    }

    /**
     * 验证双通道冲突时展示两方证据并提示人工复核。
     */
    @Test
    void exposesBothChannelsAndRequiresManualReviewOnConflict() {
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) ->
                Collections.singletonList(new KnowledgeChunk("doc-1", "费率公告", "费率为 1%"));
        final BusinessDataQueryPort business = (owner, question, recognized, limit) ->
                Collections.singletonList(new BusinessFact("product-rate", "费率为 1.2%"));
        final EvidenceReconciliationPort reconciliation = (intent, chunks, facts) ->
                new EvidenceAssessment(EvidenceAssessment.Status.CONFLICT,
                        Collections.singletonList("费率"));
        final LanguageModelPort model = (request, consumer) -> {
            assertTrue(request.evidenceAssessment().manualReviewRequired());
            consumer.accept("请以人工复核结果为准。");
            return new LanguageModelPort.GenerationResult("model", "stop");
        };
        final QuestionAnswerService service = service(
                knowledge, business, defaultIntentRecognition(), reconciliation,
                model, properties(500), Runnable::run);

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "产品费率", "conflict");
        final String content = answers.find(OWNER, answer.id()).get().content();

        assertTrue(content.contains("人工复核提示"));
        assertTrue(content.contains("费率为 1%"));
        assertTrue(content.contains("费率为 1.2%"));
        assertTrue(events.types().contains("manual_review_required"));
    }

    /**
     * 验证证据不足时拒绝猜测答案。
     */
    @Test
    void refusesToGuessWhenEvidenceIsInsufficient() {
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> Collections.emptyList();
        final BusinessDataQueryPort business = (owner, question, intent, limit) -> Collections.emptyList();
        final EvidenceReconciliationPort reconciliation = (intent, chunks, facts) ->
                new EvidenceAssessment(EvidenceAssessment.Status.INSUFFICIENT, Collections.emptyList());
        final LanguageModelPort model = (request, consumer) -> {
            throw new AssertionError("model must not be called without sufficient evidence");
        };
        final QuestionAnswerService service = service(
                knowledge, business, defaultIntentRecognition(), reconciliation,
                model, properties(100), Runnable::run);

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "未知产品", "insufficient");

        assertEquals(AnswerSnapshot.Status.COMPLETED, answers.find(OWNER, answer.id()).get().status());
        assertTrue(answers.find(OWNER, answer.id()).get().content().contains("不会推测答案"));
    }

    /**
     * 验证低置信度时先追问且不调用证据源。
     */
    @Test
    void asksForClarificationOnLowConfidenceBeforeQueryingEvidence() {
        final IntentRecognitionPort uncertain = (owner, question, history) -> {
            if (question.contains("含糊")) {
                return new QueryIntent(
                        QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                        0.5D,
                        Collections.singletonMap("productReference", "P001"));
            }
            return new QueryIntent(
                    QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                    1.0D,
                    Collections.emptyMap(),
                    Collections.singleton("productReference"),
                    Collections.emptyMap());
        };
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> {
            throw new AssertionError("knowledge must not be queried for uncertain intent");
        };
        final BusinessDataQueryPort business = (owner, question, intent, limit) -> {
            throw new AssertionError("business data must not be queried for uncertain intent");
        };
        final EvidenceReconciliationPort reconciliation = (intent, chunks, facts) -> {
            throw new AssertionError("evidence must not be reconciled for uncertain intent");
        };
        final LanguageModelPort model = (request, consumer) -> {
            throw new AssertionError("model must not be called for uncertain intent");
        };
        final QuestionAnswerService service = service(
                knowledge, business, uncertain, reconciliation,
                model, properties(100), Runnable::run);

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "含糊问题", "uncertain");

        assertEquals(AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, answer.id()).get().status());
        assertTrue(answers.find(OWNER, answer.id()).get().content().contains("不能确定"));
        assertTrue(events.types().contains("clarification_required"));
        assertTrue(!events.types().contains("retrieval_started"));
        assertTrue(!contexts.value.entities().containsKey("productReference"));
        assertTrue(!contexts.value.entitySourceMessageIds().containsKey("productReference"));

        final AnswerSnapshot followUp = service.submit(
                OWNER, conversationId, "它的产品经理是谁？", "uncertain-follow-up");
        assertEquals(AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, followUp.id()).get().status());
        assertTrue(!contexts.value.entities().containsKey("productReference"));
        assertThrows(AnswerAlreadyTerminalException.class, () -> service.regenerate(
                OWNER, answer.id(), "ambiguous-regeneration"));
    }

    /**
     * 验证非法业务日期在任何证据通道或模型调用前进入确定性追问终态。
     */
    @Test
    void asksForClarificationOnInvalidBusinessDateBeforeQueryingEvidence() {
        final IntentRecognitionPort invalidDate = (owner, question, history) -> new QueryIntent(
                QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY,
                1.0D,
                Collections.singletonMap("businessDate", "2026-02-30"));
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> {
            throw new AssertionError("knowledge must not be queried for invalid business date");
        };
        final BusinessDataQueryPort business = (owner, question, intent, limit) -> {
            throw new AssertionError("business data must not be queried for invalid business date");
        };
        final EvidenceReconciliationPort reconciliation = (intent, chunks, facts) -> {
            throw new AssertionError("evidence must not be reconciled for invalid business date");
        };
        final LanguageModelPort model = (request, consumer) -> {
            throw new AssertionError("model must not be called for invalid business date");
        };
        final QuestionAnswerService service = service(
                knowledge, business, invalidDate, reconciliation,
                model, properties(200), Runnable::run);

        final AnswerSnapshot answer = service.submit(
                OWNER, conversationId, "2026-02-30 到期产品有哪些？", "invalid-business-date");

        assertEquals(AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, answer.id()).get().status());
        assertTrue(answers.find(OWNER, answer.id()).get().content().contains("真实日历日期"));
        assertTrue(events.types().contains("clarification_required"));
        assertTrue(!events.types().contains("retrieval_started"));
        assertTrue(!events.types().contains("business_query_started"));
    }

    /**
     * 验证指代消解后两通道收到明确实体。
     */
    @Test
    void resolvesPronounFromVerifiedContextAndPassesResolvedEntityToQueries() {
        final UUID sourceMessageId = UUID.randomUUID();
        contexts.value = new ConversationContext(
                conversationId, OWNER, 1L, QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                Collections.singletonMap("productReference", "P001"),
                Collections.singletonMap("productReference", sourceMessageId), sourceMessageId,
                null, NOW.minusSeconds(1));
        final AtomicReference<String> knowledgeQuestion = new AtomicReference<>();
        final AtomicReference<QueryIntent> businessIntent = new AtomicReference<>();
        final IntentRecognitionPort reference = (owner, question, history) -> new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                1.0D,
                Collections.emptyMap(),
                new HashSet<>(Collections.singletonList("productReference")),
                Collections.emptyMap());
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> {
            knowledgeQuestion.set(question);
            return Collections.singletonList(new KnowledgeChunk("doc-1", "费率", "知识"));
        };
        final BusinessDataQueryPort business = (owner, question, intent, limit) -> {
            businessIntent.set(intent);
            return Collections.singletonList(new BusinessFact("rate-query", "事实"));
        };
        final QuestionAnswerService service = service(
                knowledge, business, reference, (intent, chunks, facts) -> consistent(),
                (request, consumer) -> {
                    consumer.accept("回答");
                    return new LanguageModelPort.GenerationResult("model", "stop");
                }, properties(100), Runnable::run);

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "它的费率呢？", "reference");

        assertEquals(AnswerSnapshot.Status.COMPLETED, answers.find(OWNER, answer.id()).get().status());
        assertTrue(knowledgeQuestion.get().contains("productReference=P001"));
        assertEquals("P001", businessIntent.get().entities().get("productReference"));
        assertTrue(!events.types().contains("clarification_required"));
        assertEquals(sourceMessageId,
                contexts.value.entitySourceMessageIds().get("productReference"));
        assertEquals(answer.questionId(), contexts.value.sourceQuestionId());
    }

    /**
     * 验证无法确认指代对象时返回追问。
     */
    @Test
    void asksForClarificationWhenReferenceCannotBeProven() {
        contexts.value = new ConversationContext(
                conversationId, OWNER, 1L, QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                Collections.singletonMap("productReference", "P001"),
                Collections.emptyMap(), UUID.randomUUID(), null, NOW.minusSeconds(1));
        final IntentRecognitionPort reference = (owner, question, history) -> new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                1.0D,
                Collections.emptyMap(),
                new HashSet<>(Collections.singletonList("productReference")),
                Collections.emptyMap());
        final KnowledgeRetrievalPort knowledge = (owner, question, limit) -> {
            throw new AssertionError("knowledge must not be queried");
        };
        final BusinessDataQueryPort business = (owner, question, intent, limit) -> {
            throw new AssertionError("business data must not be queried");
        };
        final QuestionAnswerService service = service(
                knowledge, business, reference, (intent, chunks, facts) -> {
                    throw new AssertionError("evidence must not be reconciled");
                }, (request, consumer) -> {
                    throw new AssertionError("model must not be called");
                }, properties(200), Runnable::run);

        final AnswerSnapshot answer = service.submit(OWNER, conversationId, "它的费率呢？", "missing-reference");

        assertEquals(AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, answer.id()).get().status());
        assertTrue(answers.find(OWNER, answer.id()).get().content().contains("没有可唯一确认的产品"));
        assertTrue(events.types().contains("clarification_required"));
        assertTrue(!events.types().contains("business_query_started"));
    }

    /**
     * 验证选择已持久化候选后恢复原始查询意图。
     */
    @Test
    void asksUserToChooseAndResumesOriginalIntentFromPersistedCandidates() {
        final List<EntityCandidate> options = java.util.Arrays.asList(
                new EntityCandidate("P001", "悦享一号（P001）", "悦享一号"),
                new EntityCandidate("P002", "悦享二号（P002）", "悦享二号"));
        final IntentRecognitionPort recognition = (owner, question, history) -> {
            if ("悦享二号".equals(question)) {
                return new QueryIntent(QueryIntent.Type.UNSUPPORTED, 0.1D, Collections.emptyMap());
            }
            final Map<String, List<EntityCandidate>> candidates = new HashMap<>();
            candidates.put("productReference", options);
            return new QueryIntent(
                    QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D,
                    Collections.emptyMap(), Collections.emptySet(), candidates);
        };
        final AtomicReference<QueryIntent> queried = new AtomicReference<>();
        final QuestionAnswerService service = service(
                (owner, question, limit) -> Collections.singletonList(
                        new KnowledgeChunk("doc", "资料", "知识")),
                (owner, question, intent, limit) -> {
                    queried.set(intent);
                    return Collections.singletonList(new BusinessFact("query", "事实"));
                }, recognition, (intent, chunks, facts) -> consistent(),
                (request, consumer) -> {
                    consumer.accept("回答");
                    return new LanguageModelPort.GenerationResult("model", "stop");
                }, properties(500), Runnable::run);

        final AnswerSnapshot clarification = service.submit(
                OWNER, conversationId, "悦享一号还是悦享二号的费率？", "ambiguous");
        final AnswerSnapshot resolved = service.submit(OWNER, conversationId, "悦享二号", "selected");

        assertEquals(AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, clarification.id()).get().status());
        assertTrue(answers.find(OWNER, clarification.id()).get().content().contains("2. 悦享二号（P002）"));
        assertEquals(AnswerSnapshot.Status.COMPLETED, answers.find(OWNER, resolved.id()).get().status());
        assertEquals("P002", queried.get().entities().get("productReference"));
        assertTrue(contexts.value.pendingClarification() == null);
    }

    /**
     * 验证产品名称匹配出多个 GoldenDB 候选时先追问，且不调用下游证据通道。
     */
    @Test
    void asksForClarificationWhenDatabaseProductResolutionIsAmbiguous() {
        final IntentRecognitionPort recognition = (owner, question, history) -> new QueryIntent(
                QueryIntent.Type.PRODUCT_MANAGER_QUERY,
                1.0D,
                Collections.singletonMap("productReference", "悦享"));
        productEntityResolution = (owner, reference) -> Arrays.asList(
                new EntityCandidate("P001", "悦享3号（P001）"),
                new EntityCandidate("P002", "悦享6号（P002）"));
        final QuestionAnswerService service = service(
                (owner, question, limit) -> {
                    throw new AssertionError("ambiguous product must not query knowledge");
                },
                (owner, question, intent, limit) -> {
                    throw new AssertionError("ambiguous product must not query business data");
                },
                recognition,
                (intent, chunks, facts) -> {
                    throw new AssertionError("ambiguous product must not reconcile evidence");
                },
                (request, consumer) -> {
                    throw new AssertionError("ambiguous product must not call model");
                },
                properties(500),
                Runnable::run);

        final AnswerSnapshot answer = service.submit(
                OWNER, conversationId, "悦享的产品经理是谁", "database-ambiguous-product");

        assertEquals(
                AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, answer.id()).get().status());
        assertTrue(answers.find(OWNER, answer.id()).get().content().contains("1. 悦享3号（P001）"));
        assertTrue(answers.find(OWNER, answer.id()).get().content().contains("2. 悦享6号（P002）"));
        assertTrue(!events.types().contains("business_query_started"));
    }

    /**
     * 验证歧义原文不会污染可信上下文，切换主题后也不能被后续指代复活。
     */
    @Test
    void doesNotReviveAmbiguousRawEntityAfterSwitchingTopic() {
        final IntentRecognitionPort recognition = (owner, question, history) -> {
            if (question.contains("产品经理")) {
                return new QueryIntent(
                        QueryIntent.Type.PRODUCT_MANAGER_QUERY,
                        1.0D,
                        Collections.singletonMap("productReference", "悦享"));
            }
            if (question.contains("T123")) {
                return new QueryIntent(
                        QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                        1.0D,
                        Collections.singletonMap("tradeReference", "T123"));
            }
            return new QueryIntent(
                    QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO,
                    1.0D,
                    Collections.emptyMap(),
                    new HashSet<>(Collections.singletonList("productReference")),
                    Collections.emptyMap());
        };
        productEntityResolution = (owner, reference) -> Arrays.asList(
                new EntityCandidate("P001", "悦享3号（P001）"),
                new EntityCandidate("P002", "悦享6号（P002）"));
        final QuestionAnswerService service = service(
                (owner, question, limit) -> Collections.singletonList(
                        new KnowledgeChunk("doc", "资料", "知识")),
                (owner, question, intent, limit) -> Collections.singletonList(
                        new BusinessFact("query", "事实")),
                recognition,
                (intent, chunks, facts) -> consistent(),
                (request, consumer) -> {
                    consumer.accept("回答");
                    return new LanguageModelPort.GenerationResult("model", "stop");
                },
                properties(500),
                Runnable::run);

        final AnswerSnapshot ambiguous = service.submit(
                OWNER, conversationId, "悦享的产品经理是谁", "ambiguous-before-switch");

        assertEquals(AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, ambiguous.id()).get().status());
        assertTrue(!contexts.value.entities().containsKey("productReference"));
        assertTrue(!contexts.value.entitySourceMessageIds().containsKey("productReference"));

        final AnswerSnapshot switched = service.submit(
                OWNER, conversationId, "查询交易 T123 的交易员", "switch-to-trade");
        final AnswerSnapshot pronoun = service.submit(
                OWNER, conversationId, "它的费率呢？", "pronoun-after-switch");

        assertEquals(AnswerSnapshot.Status.COMPLETED,
                answers.find(OWNER, switched.id()).get().status());
        assertEquals(AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, pronoun.id()).get().status());
        assertTrue(!contexts.value.entities().containsKey("productReference"));
        assertEquals("T123", contexts.value.entities().get("tradeReference"));
    }

    /**
     * 验证补充产品后仍查询原请求的最新文档。
     */
    @Test
    void resumesOriginalLatestDocumentIntentFromOpenEntityReply() {
        final IntentRecognitionPort recognition = (owner, question, history) -> {
            if (question.contains("最新公告")) {
                return new QueryIntent(
                        QueryIntent.Type.PRODUCT_LATEST_DOCUMENT_INFO,
                        1.0D,
                        Collections.emptyMap(),
                        Collections.singleton("productReference"),
                        Collections.emptyMap());
            }
            return new QueryIntent(
                    QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());
        };
        final AtomicReference<QueryIntent> queried = new AtomicReference<>();
        final QuestionAnswerService service = service(
                (owner, question, limit) -> Collections.singletonList(
                        new KnowledgeChunk("doc", "最新公告", "知识")),
                (owner, question, intent, limit) -> {
                    queried.set(intent);
                    return Collections.singletonList(new BusinessFact("latest-document", "事实"));
                }, recognition, (intent, chunks, facts) -> consistent(),
                (request, consumer) -> {
                    consumer.accept("回答");
                    return new LanguageModelPort.GenerationResult("model", "stop");
                }, properties(500), Runnable::run);

        final AnswerSnapshot clarification = service.submit(
                OWNER, conversationId, "它的最新公告？", "latest-reference");
        final AnswerSnapshot resolved = service.submit(
                OWNER, conversationId, "悦享三号", "latest-reference-follow-up");

        assertEquals(AnswerSnapshot.Status.NEEDS_CLARIFICATION,
                answers.find(OWNER, clarification.id()).get().status());
        assertEquals(AnswerSnapshot.Status.COMPLETED,
                answers.find(OWNER, resolved.id()).get().status());
        assertEquals(QueryIntent.Type.PRODUCT_LATEST_DOCUMENT_INFO, queried.get().type());
        assertEquals("悦享三号", queried.get().entities().get("productReference"));
    }

    /**
     * 处理被测应用服务。
     *
     * @param knowledge 知识库片段列表。
     *
     * @param business 测试业务数据端口。
     *
     * @param model 测试大模型端口。
     *
     * @param properties 配置参数。
     *
     * @return 被测应用服务。
     */
    private QuestionAnswerService service(
            final KnowledgeRetrievalPort knowledge,
            final BusinessDataQueryPort business,
            final LanguageModelPort model,
            final QaProperties properties) {
        return service(knowledge, business, model, properties, Runnable::run);
    }

    /**
     * 处理被测应用服务。
     *
     * @param knowledge 知识库片段列表。
     *
     * @param business 测试业务数据端口。
     *
     * @param model 测试大模型端口。
     *
     * @param properties 配置参数。
     *
     * @param executor 异步任务执行器。
     *
     * @return 被测应用服务。
     */
    private QuestionAnswerService service(
            final KnowledgeRetrievalPort knowledge,
            final BusinessDataQueryPort business,
            final LanguageModelPort model,
            final QaProperties properties,
            final Executor executor) {
        return new QuestionAnswerService(
                conversations,
                contexts,
                answers,
                new QuestionFileCoordinator(temporaryFiles),
                cancellationRepository(),
                workflowEngine(
                        knowledge,
                        business,
                        defaultIntentRecognition(),
                        (intent, chunks, facts) -> consistent(),
                        model,
                        properties),
                events,
                properties,
                cancellationDispatcher(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                executor);
    }

    /**
     * 处理被测应用服务。
     *
     * @param knowledge 知识库片段列表。
     *
     * @param business 测试业务数据端口。
     *
     * @param intent 查询意图。
     *
     * @param reconciliation 测试证据对账端口。
     *
     * @param model 测试大模型端口。
     *
     * @param properties 配置参数。
     *
     * @param executor 异步任务执行器。
     *
     * @return 被测应用服务。
     */
    private QuestionAnswerService service(
            final KnowledgeRetrievalPort knowledge,
            final BusinessDataQueryPort business,
            final IntentRecognitionPort intent,
            final EvidenceReconciliationPort reconciliation,
            final LanguageModelPort model,
            final QaProperties properties,
            final Executor executor) {
        return new QuestionAnswerService(
                conversations,
                contexts,
                answers,
                new QuestionFileCoordinator(temporaryFiles),
                cancellationRepository(),
                workflowEngine(knowledge, business, intent, reconciliation, model, properties),
                events,
                properties,
                cancellationDispatcher(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                executor);
    }

    /**
     * 按测试替身组装仅支持双通道场景的轻量工作流执行器。
     *
     * @param knowledge 知识库检索端口。
     * @param business 业务数据查询端口。
     * @param intent 意图识别端口。
     * @param reconciliation 证据对账端口。
     * @param model 大模型生成端口。
     * @param properties 问答容量配置。
     * @return 测试使用的工作流执行器。
     */
    private QuestionWorkflowEngine workflowEngine(
            final KnowledgeRetrievalPort knowledge,
            final BusinessDataQueryPort business,
            final IntentRecognitionPort intent,
            final EvidenceReconciliationPort reconciliation,
            final LanguageModelPort model,
            final QaProperties properties) {
        return new QuestionWorkflowEngine(
                new ScenarioPlanRegistry(Arrays.asList(
                        new QuestionUnderstandingWorkflowNode(
                                intent,
                                new QuestionUnderstandingService(properties),
                                productEntityResolution,
                                properties),
                        new KnowledgeRetrievalWorkflowNode(knowledge, properties),
                        new BusinessDataQueryWorkflowNode(business, properties),
                        new EvidenceReconciliationWorkflowNode(reconciliation),
                        new AnswerGenerationWorkflowNode(model))),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * 处理测试默认意图识别器。
     *
     * @return 测试默认意图识别器。
     */
    private IntentRecognitionPort defaultIntentRecognition() {
        return (owner, question, history) -> defaultIntent();
    }

    /**
     * 处理测试默认查询意图。
     *
     * @return 测试默认查询意图。
     */
    private QueryIntent defaultIntent() {
        return new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());
    }

    /**
     * 处理双通道证据是否一致。
     *
     * @return 双通道证据是否一致。
     */
    private EvidenceAssessment consistent() {
        return new EvidenceAssessment(EvidenceAssessment.Status.CONSISTENT, Collections.emptyList());
    }

    /**
     * 处理配置参数。
     *
     * @param maxAnswerCharacters 回答最大字符数。
     *
     * @return 配置参数。
     */
    private QaProperties properties(final int maxAnswerCharacters) {
        return properties(maxAnswerCharacters, 100);
    }

    /**
     * 处理配置参数。
     *
     * @param maxAnswerCharacters 回答最大字符数。
     *
     * @param maxQuestionCharacters 问题最大字符数。
     *
     * @return 配置参数。
     */
    private QaProperties properties(final int maxAnswerCharacters, final int maxQuestionCharacters) {
        return new QaProperties(
                false, maxQuestionCharacters, maxAnswerCharacters,
                8, 100, 100, 10, 0.8D, 100, 1, 1, 10);
    }

    /**
     * 判断停止任务配置。
     *
     * @return 停止任务配置。
     */
    private CancellationProperties cancellationProperties() {
        return new CancellationProperties(1000, 10000, 1000, 10000, 3, 10);
    }

    /**
     * 返回停止任务仓储。
     *
     * @return 停止任务仓储。
     */
    private CancellationRepositoryPort cancellationRepository() {
        return new FakeCancellationRepository();
    }

    /**
     * 判断停止任务分发器。
     *
     * @return 停止任务分发器。
     */
    private CancellationDispatcher cancellationDispatcher() {
        return new CancellationDispatcher(
                new FakeCancellationRepository(),
                (owner, messageId) -> { },
                events,
                cancellationProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * 测试使用的内存会话仓储。
     */
    private static final class FakeConversationRepository implements ConversationRepositoryPort {
        /**
         * 会话领域对象集合。
         */
        private final Map<UUID, Conversation> values = new HashMap<>();
        /** 首次提问幂等键到会话 ID 的索引。 */
        private final Map<String, UUID> creationKeys = new HashMap<>();
        /**
         * 最近对话历史。
         */
        private final List<ChatMessage> history = new ArrayList<>();

        /**
         * 创建 {@code FakeConversationRepository} 实例。
         *
         * @param conversation 会话领域对象。
         */
        FakeConversationRepository(final Conversation conversation) {
            values.put(conversation.id(), conversation);
        }

        /**
         * 构建当前用户未删除会话的查询条件。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param id 唯一标识。
         *
         * @return 构建当前用户未删除会话的查询条件。
         */
        @Override
        public Optional<Conversation> findActive(final String ownerId, final UUID id) {
            final Conversation conversation = values.get(id);
            return conversation != null && conversation.ownerId().equals(ownerId)
                    ? Optional.of(conversation) : Optional.empty();
        }

        /**
         * 模拟事务内加锁读取有效会话。
         *
         * @param ownerId 用户所有者 ID。
         * @param id 会话 ID。
         * @return 存在且归属匹配时返回会话。
         */
        @Override
        public Optional<Conversation> findActiveForUpdate(final String ownerId, final UUID id) {
            return findActive(ownerId, id);
        }

        /**
         * 创建并持久化业务对象。
         *
         * @param id 唯一标识。
         *
         * @param owner 当前认证用户标识。
         *
         * @param title 会话名称。
         *
         * @param now 当前时间。
         *
         * @return 创建并持久化业务对象。
         */
        @Override public Conversation create(final UUID id, final String owner, final String title, final Instant now) {
            final Conversation created = new Conversation(id, owner, title, now, now);
            values.put(id, created);
            return created;
        }
        /**
         * 按幂等键创建或复用首次提问会话。
         *
         * @param id 新会话 ID。
         * @param owner 当前认证用户标识。
         * @param title 会话名称。
         * @param agentType 会话创建时选定且不可变更的 Agent 类型。
         * @param idempotencyKey 首次提问幂等键。
         * @param now 当前时间。
         * @return 新创建或已经存在的会话。
         */
        @Override public Conversation createForQuestion(
                final UUID id,
                final String owner,
                final String title,
                final AgentType agentType,
                final String idempotencyKey,
                final Instant now) {
            final String indexKey = owner + ":" + idempotencyKey;
            final UUID existingId = creationKeys.get(indexKey);
            if (existingId != null) {
                return values.get(existingId);
            }
            final Conversation created = new Conversation(id, owner, title, agentType, now, now);
            values.put(id, created);
            creationKeys.put(indexKey, id);
            return created;
        }
        /**
         * 按用户和首次提问幂等键查找会话。
         *
         * @param ownerId 当前认证用户标识。
         * @param idempotencyKey 首次提问幂等键。
         * @return 匹配的会话。
         */
        @Override public Optional<Conversation> findActiveByCreationKey(
                final String ownerId,
                final String idempotencyKey) {
            return Optional.ofNullable(creationKeys.get(ownerId + ":" + idempotencyKey))
                    .map(values::get);
        }
        /**
         * 按所属用户查询会话列表。
         *
         * @param owner 当前认证用户标识。
         *
         * @param beforeUpdatedAt 游标中的最后更新时间。
         *
         * @param beforeId 游标中的最后会话 ID。
         *
         * @param limit 数量上限。
         *
         * @return 按所属用户查询会话列表。
         */
        @Override public List<Conversation> listByOwner(
                final String owner,
                final Instant beforeUpdatedAt,
                final UUID beforeId,
                final int limit) {
            return Collections.emptyList();
        }
        /**
         * 判断会话是否存在活动回答。
         *
         * @param owner 当前认证用户标识。
         * @param conversationId 会话 ID。
         * @return 此测试替身始终返回 false。
         */
        @Override public boolean hasActiveAnswer(final String owner, final UUID conversationId) {
            return false;
        }
        /**
         * 读取会话的有界历史消息。
         *
         * @param owner 当前认证用户标识。
         *
         * @param id 唯一标识。
         *
         * @param limit 数量上限。
         *
         * @return 读取会话的有界历史消息。
         */
        @Override public List<ChatMessage> listMessages(final String owner, final UUID id, final int limit) {
            return new ArrayList<>(history.subList(Math.max(0, history.size() - limit), history.size()));
        }
        /**
         * 修改当前用户的会话名称。
         *
         * @param owner 当前认证用户标识。
         *
         * @param id 唯一标识。
         *
         * @param title 会话名称。
         *
         * @param now 当前时间。
         *
         * @return 修改当前用户的会话名称。
         */
        @Override public Optional<Conversation> rename(
                final String owner, final UUID id, final String title, final Instant now) {
            return Optional.empty();
        }
        /**
         * 按用户归属对会话执行逻辑删除。
         *
         * @param owner 当前认证用户标识。
         *
         * @param id 唯一标识。
         *
         * @param now 当前时间。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override public boolean softDelete(final String owner, final UUID id, final Instant now) { return false; }

        /**
         * 向测试会话追加历史消息。
         *
         * @param message 提示信息。
         */
        void addHistory(final ChatMessage message) {
            history.add(message);
        }

        /** @return 当前保存的会话数量。 */
        int size() { return values.size(); }
    }

    /**
     * 测试使用的内存回答仓储。
     */
    private static final class FakeAnswerRepository implements AnswerRepositoryPort {
        /**
         * 待处理值集合。
         */
        private final Map<UUID, AnswerSnapshot> values = new HashMap<>();
        /**
         * 测试幂等键索引。
         */
        private final Map<String, UUID> idempotency = new HashMap<>();
        /**
         * 按回答 ID 保存的测试问题。
         */
        private final Map<UUID, String> questions = new HashMap<>();
        /**
         * 用户反馈。
         */
        private final Map<UUID, QuestionAnswerUseCase.Feedback> feedback = new HashMap<>();
        /**
         * 已创建的测试回答数量。
         */
        private int createdCount;

        /**
         * 按用户和幂等键查询已受理结果。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param key 查找键。
         *
         * @return 按用户和幂等键查询已受理结果。
         */
        @Override
        public Optional<AnswerSnapshot> findByIdempotencyKey(final String ownerId, final String key) {
            return Optional.ofNullable(idempotency.get(ownerId + ':' + key)).map(values::get);
        }

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
         * @param regeneratedFrom 待重新生成的原回答。
         *
         * @param question 用户问题。
         *
         * @param key 查找键。
         *
         * @param now 当前时间。
         *
         * @return 创建并持久化业务对象。
         */
        @Override
        public AnswerSnapshot create(
                final String ownerId,
                final UUID conversationId,
                final UUID questionId,
                final UUID answerId,
                final UUID traceId,
                final UUID regeneratedFrom,
                final String question,
                final String key,
                final Instant now) {
            final AnswerSnapshot answer = new AnswerSnapshot(
                    answerId, conversationId, questionId, traceId, regeneratedFrom,
                    AnswerSnapshot.Status.PENDING, "", null, now, null);
            values.put(answerId, answer);
            idempotency.put(ownerId + ':' + key, answerId);
            questions.put(answerId, question);
            createdCount++;
            return answer;
        }

        /**
         * 查询满足用户隔离条件的目标记录。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param answerId 回答 ID。
         *
         * @return 查询满足用户隔离条件的目标记录。
         */
        @Override public Optional<AnswerSnapshot> find(final String ownerId, final UUID answerId) {
            return Optional.ofNullable(values.get(answerId));
        }
        /**
         * 读取回答对应的原始问题。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param answerId 回答 ID。
         *
         * @return 读取回答对应的原始问题。
         */
        @Override public Optional<String> findQuestion(final String ownerId, final UUID answerId) {
            return Optional.ofNullable(questions.get(answerId));
        }
        /**
         * 按允许的源状态执行回答状态条件更新。
         *
         * @param answerId 回答 ID。
         *
         * @param status 业务状态。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override public boolean transitionStatus(final UUID answerId, final AnswerSnapshot.Status status) {
            replace(answerId, status, values.get(answerId).content(), null, null);
            return true;
        }
        /**
         * 保存公司 HiAgent 创建会话接口返回的应用会话 ID。
         *
         * @param answerId 回答 ID。
         *
         * @param appConversationId 公司 HiAgent 应用会话 ID。
         *
         * @return 测试仓储始终返回 true。
         */
        @Override public boolean recordAppConversationId(
                final UUID answerId, final String appConversationId) {
            return true;
        }
        /**
         * 保存已生成的部分回答文本。
         *
         * @param answerId 回答 ID。
         *
         * @param content 内容。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override public boolean savePartial(final UUID answerId, final String content) {
            replace(answerId, values.get(answerId).status(), content, null, null);
            return true;
        }
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
        @Override public boolean complete(final UUID answerId, final String content, final Instant completedAt) {
            replace(answerId, AnswerSnapshot.Status.COMPLETED, content, null, completedAt);
            return true;
        }
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
        @Override public boolean clarify(final UUID answerId, final String content, final Instant completedAt) {
            replace(answerId, AnswerSnapshot.Status.NEEDS_CLARIFICATION, content, null, completedAt);
            return true;
        }
        /**
         * 记录失败或未完整状态，并保留已生成内容。
         *
         * @param answerId 回答 ID。
         *
         * @param status 业务状态。
         *
         * @param content 内容。
         *
         * @param errorCode 错误码。
         *
         * @param completedAt 完成时间。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override public boolean fail(
                final UUID answerId,
                final AnswerSnapshot.Status status,
                final String content,
                final String errorCode,
                final Instant completedAt) {
            replace(answerId, status, content, errorCode, completedAt);
            return true;
        }
        /**
         * 新增或覆盖用户对指定回答的反馈。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param answerId 回答 ID。
         *
         * @param value 输入值。
         *
         * @param now 当前时间。
         */
        @Override public void upsertFeedback(
                final String ownerId,
                final UUID answerId,
                final QuestionAnswerUseCase.Feedback value,
                final Instant now) {
            feedback.put(answerId, value);
        }

        /**
         * 替换测试仓储中的回答快照。
         *
         * @param answerId 回答 ID。
         *
         * @param status 业务状态。
         *
         * @param content 内容。
         *
         * @param errorCode 错误码。
         *
         * @param completedAt 完成时间。
         */
        private void replace(
                final UUID answerId,
                final AnswerSnapshot.Status status,
                final String content,
                final String errorCode,
                final Instant completedAt) {
            final AnswerSnapshot old = values.get(answerId);
            values.put(answerId, new AnswerSnapshot(
                    old.id(), old.conversationId(), old.questionId(), old.traceId(), old.regeneratedFromAnswerId(),
                    status, content, errorCode, old.createdAt(), completedAt));
        }
    }

    /**
     * 测试使用的结构化上下文仓储。
     */
    private static final class FakeConversationContextRepository implements ConversationContextRepositoryPort {
        /**
         * 输入值。
         */
        private ConversationContext value;

        /**
         * 查询满足用户隔离条件的目标记录。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param conversationId 会话 ID。
         *
         * @return 查询满足用户隔离条件的目标记录。
         */
        @Override
        public Optional<ConversationContext> find(final String ownerId, final UUID conversationId) {
            if (value == null || !value.ownerId().equals(ownerId)
                    || !value.conversationId().equals(conversationId)) {
                return Optional.empty();
            }
            return Optional.of(value);
        }

        /**
         * 持久化当前结构化会话状态。
         *
         * @param context 结构化会话上下文。
         *
         * @param expectedVersion 期望的乐观锁版本号。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override
        public boolean save(final ConversationContext context, final long expectedVersion) {
            final long actualVersion = value == null ? 0L : value.version();
            if (actualVersion != expectedVersion) {
                return false;
            }
            value = new ConversationContext(
                    context.conversationId(), context.ownerId(), expectedVersion + 1L,
                    context.lastIntent(), context.entities(), context.entitySourceMessageIds(),
                    context.sourceQuestionId(),
                    context.pendingClarification(), context.updatedAt());
            return true;
        }
    }

    /**
     * 测试使用的停止任务仓储。
     */
    private static final class FakeCancellationRepository implements CancellationRepositoryPort {
        /**
         * 携带同源凭据并在有界超时内调用 JSON 接口。
         *
         * @param ownerId 用户所有者 ID。
         *
         * @param answerId 回答 ID。
         *
         * @param idempotencyKey 幂等键。
         *
         * @param reason 原因。
         *
         * @param requestedAt 请求受理时间。
         *
         * @param firstDispatchAt 停止任务首次计划执行时间。
         *
         * @return 接口请求。
         */
        @Override
        public RequestResult request(
                final String ownerId,
                final UUID answerId,
                final String idempotencyKey,
                final AnswerCancellationUseCase.CancellationReason reason,
                final Instant requestedAt,
                final Instant firstDispatchAt) {
            throw new UnsupportedOperationException();
        }

        /**
         * 判断回答是否已收到停止请求。
         *
         * @param answerId 回答 ID。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override public boolean isCancellationRequested(final UUID answerId) { return false; }
        /**
         * 保存公司模型侧消息 ID。
         *
         * @param answerId 回答 ID。
         *
         * @param messageId 公司模型侧消息 ID。
         *
         * @param dispatchAt 本次任务分发时间。
         */
        @Override public void recordMessageId(
                final UUID answerId, final String messageId, final Instant dispatchAt) { }
        /**
         * 查询本批可领取的停止任务。
         *
         * @param now 当前时间。
         *
         * @param limit 数量上限。
         *
         * @return 查询本批可领取的停止任务。
         */
        @Override public List<UUID> findDispatchable(final Instant now, final int limit) {
            return Collections.emptyList();
        }
        /**
         * 将耗尽重试的过期任务收敛为停止失败。
         *
         * @param answerId 回答 ID。
         * @param now 当前时间。
         * @param maximumAttempts 最大尝试次数。
         * @param errorCode 稳定错误码。
         * @return 测试替身固定返回 false。
         */
        @Override public boolean markExpiredExhausted(
                final UUID answerId,
                final Instant now,
                final int maximumAttempts,
                final String errorCode) {
            return false;
        }
        /**
         * 按租约条件领取待执行的停止任务。
         *
         * @param answerId 回答 ID。
         *
         * @param workerId 当前停止任务执行器标识。
         *
         * @param now 当前时间。
         *
         * @param leaseUntil 租约到期时间。
         *
         * @param maximumAttempts 停止接口最大尝试次数。
         *
         * @return 按租约条件领取待执行的停止任务。
         */
        @Override public Optional<Task> claim(
                final UUID answerId,
                final String workerId,
                final Instant now,
                final Instant leaseUntil,
                final int maximumAttempts) {
            return Optional.empty();
        }
        /**
         * 在并发条件允许时将回答标记为已停止。
         *
         * @param answerId 回答 ID。
         *
         * @param workerId 当前停止任务执行器标识。
         *
         * @param cancelledAt 停止完成时间。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override public boolean markCancelled(
                final UUID answerId, final String workerId, final Instant cancelledAt) { return false; }
        /**
         * 为暂时失败的停止任务安排下一次执行。
         *
         * @param answerId 回答 ID。
         *
         * @param workerId 当前停止任务执行器标识。
         *
         * @param errorCode 错误码。
         *
         * @param nextAttemptAt 下一次停止任务计划执行时间。
         */
        @Override public void reschedule(
                final UUID answerId,
                final String workerId,
                final String errorCode,
                final Instant nextAttemptAt) { }
        /**
         * 将生成异常转换为失败或未完整终态。
         *
         * @param answerId 回答 ID。
         *
         * @param workerId 当前停止任务执行器标识。
         *
         * @param errorCode 错误码。
         *
         * @param failedAt 失败记录时间。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override public boolean markFailed(
                final UUID answerId,
                final String workerId,
                final String errorCode,
                final Instant failedAt) { return false; }
    }

    /**
     * 测试使用的回答事件收集端口。
     */
    private static final class FakeEventPort implements AnswerEventPort {
        /**
         * 待处理值集合。
         */
        private final Map<UUID, List<AnswerEvent>> values = new HashMap<>();

        /**
         * 按回答序号发布一个流式事件。
         *
         * @param answerId 回答 ID。
         *
         * @param type 类型。
         *
         * @param data 一个 SSE 事件的多行数据内容。
         */
        @Override
        public void publish(final UUID answerId, final String type, final String data) {
            final List<AnswerEvent> answerEvents = values.computeIfAbsent(answerId, ignored -> new ArrayList<>());
            answerEvents.add(new AnswerEvent(answerEvents.size() + 1L, type, data, NOW));
        }

        /**
         * 从指定序号订阅回答事件。
         *
         * @param answerId 回答 ID。
         *
         * @param afterSequence 最后确认的事件序号。
         *
         * @param consumer 事件订阅回调。
         *
         * @return 从指定序号订阅回答事件。
         */
        @Override
        public Subscription subscribe(
                final UUID answerId,
                final long afterSequence,
                final Consumer<AnswerEvent> consumer) {
            for (final AnswerEvent event : values.getOrDefault(answerId, Collections.emptyList())) {
                if (event.sequence() > afterSequence) {
                    consumer.accept(event);
                }
            }
            return () -> { };
        }

        /**
         * 处理已收集的事件类型列表。
         *
         * @return 已收集的事件类型列表。
         */
        List<String> types() {
            final List<String> result = new ArrayList<>();
            for (final List<AnswerEvent> answerEvents : values.values()) {
                for (final AnswerEvent event : answerEvents) {
                    result.add(event.type());
                }
            }
            return result;
        }

        /**
         * 返回指定类型首次出现的事件值。
         *
         * @param type 事件类型。
         * @return 首个匹配事件的值。
         */
        String value(final String type) {
            for (final List<AnswerEvent> answerEvents : values.values()) {
                for (final AnswerEvent event : answerEvents) {
                    if (event.type().equals(type)) {
                        return event.data();
                    }
                }
            }
            throw new AssertionError("event not found: " + type);
        }
    }
}
