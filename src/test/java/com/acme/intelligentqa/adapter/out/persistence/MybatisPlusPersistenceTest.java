package com.acme.intelligentqa.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.intelligentqa.common.error.AnswerAlreadyTerminalException;
import com.acme.intelligentqa.application.service.ConversationService;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.AgentType;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.ClarificationRequest;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.ConversationContext;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.in.QuestionAnswerUseCase;
import com.acme.intelligentqa.domain.port.in.AnswerCancellationUseCase;
import com.acme.intelligentqa.domain.port.out.CancellationRepositoryPort;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * 验证 MybatisPlusPersistence 的业务行为与边界。
 */
@ActiveProfiles("test")
@MybatisPlusTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:qa-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml",
        "mybatis-plus.configuration.map-underscore-to-camel-case=false"
})
@Import({
        MybatisPlusConversationRepository.class,
        MybatisPlusConversationContextRepository.class,
        MybatisPlusAnswerRepository.class,
        MybatisPlusAnswerEventRepository.class,
        MybatisPlusCancellationRepository.class,
        MybatisPlusTemporaryFileRepository.class,
        MybatisPlusPersistenceTest.JacksonTestConfiguration.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/db/mybatis-plus-test-schema.sql")
class MybatisPlusPersistenceTest {

    /**
     * 测试用户 ID。
     */
    private static final String OWNER_ID = "user-1";
    /**
     * 固定测试时钟时间。
     */
    private static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    /**
     * 测试会话集合。
     */
    @Autowired
    private MybatisPlusConversationRepository conversations;

    /**
     * 测试回答仓储中的记录集合。
     */
    @Autowired
    private MybatisPlusAnswerRepository answers;
    /** 测试回答执行事件仓储。 */
    @Autowired
    private MybatisPlusAnswerEventRepository answerEvents;

    /**
     * 测试停止任务仓储。
     */
    @Autowired
    private MybatisPlusCancellationRepository cancellations;

    /**
     * 测试结构化上下文存储。
     */
    @Autowired
    private MybatisPlusConversationContextRepository contexts;

    /** 测试临时文件仓储。 */
    @Autowired
    private MybatisPlusTemporaryFileRepository temporaryFiles;

    /**
     * 测试 SQL 执行器。
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 验证所属用户会话的创建改名列表与逻辑删除。
     */
    @Test
    void createsRenamesListsAndSoftDeletesOwnedConversation() {
        final UUID conversationId = UUID.randomUUID();
        conversations.create(conversationId, OWNER_ID, "新会话", NOW);

        final Conversation renamed = conversations.rename(
                        OWNER_ID, conversationId, "产品问答", NOW.plusSeconds(1))
                .orElseThrow(AssertionError::new);

        assertEquals("产品问答", renamed.title());
        assertEquals(1, conversations.listByOwner(OWNER_ID, null, null, 10).size());
        assertFalse(conversations.findActive("another-user", conversationId).isPresent());
        assertTrue(conversations.softDelete(OWNER_ID, conversationId, NOW.plusSeconds(2)));
        assertFalse(conversations.findActive(OWNER_ID, conversationId).isPresent());
        assertTrue(conversations.listByOwner(OWNER_ID, null, null, 10).isEmpty());
    }

    /**
     * 验证首次提问会话按用户和幂等键只创建一次。
     */
    @Test
    void createsFirstQuestionConversationIdempotently() {
        final UUID firstId = UUID.randomUUID();
        final Conversation first = conversations.createForQuestion(
                firstId, OWNER_ID, "首次问题", AgentType.SMART_DATA,
                "first-question-key", NOW);
        final Conversation replayed = conversations.createForQuestion(
                UUID.randomUUID(), OWNER_ID, "首次问题", AgentType.SMART_DATA,
                "first-question-key", NOW.plusSeconds(1));

        assertEquals(firstId, first.id());
        assertEquals(AgentType.SMART_DATA, first.agentType());
        assertEquals(first.id(), replayed.id());
        assertEquals(first.id(), conversations.findActiveByCreationKey(
                OWNER_ID, "first-question-key").orElseThrow(AssertionError::new).id());
        assertFalse(conversations.findActiveByCreationKey(
                "another-user", "first-question-key").isPresent());
        assertEquals(1, conversations.listByOwner(OWNER_ID, null, null, 10).size());
    }

    /**
     * 验证会话行锁查询和活动回答统计使用相同 Owner 边界。
     */
    @Test
    void locksOwnedConversationAndDetectsActiveAnswer() {
        final UUID conversationId = createConversation();
        final UUID answerId = UUID.randomUUID();
        answers.create(
                OWNER_ID,
                conversationId,
                UUID.randomUUID(),
                answerId,
                UUID.randomUUID(),
                null,
                "查询产品费率",
                "active-answer",
                NOW);

        assertTrue(conversations.findActiveForUpdate(OWNER_ID, conversationId).isPresent());
        assertFalse(conversations.findActiveForUpdate("another-user", conversationId).isPresent());
        assertTrue(conversations.hasActiveAnswer(OWNER_ID, conversationId));
        answers.complete(answerId, "回答完成", NOW.plusSeconds(1));
        assertFalse(conversations.hasActiveAnswer(OWNER_ID, conversationId));
    }

    /**
     * 验证稳定游标从上一页末项之后继续读取且不会重复。
     */
    @Test
    void paginatesConversationsWithStableCursor() {
        conversations.create(UUID.randomUUID(), OWNER_ID, "最早会话", NOW);
        conversations.create(UUID.randomUUID(), OWNER_ID, "中间会话", NOW.plusSeconds(1));
        conversations.create(UUID.randomUUID(), OWNER_ID, "最新会话", NOW.plusSeconds(2));

        final List<Conversation> first = conversations.listByOwner(OWNER_ID, null, null, 2);
        final Conversation boundary = first.get(1);
        final List<Conversation> second = conversations.listByOwner(
                OWNER_ID, boundary.updatedAt(), boundary.id(), 2);

        assertEquals(2, first.size());
        assertEquals("最新会话", first.get(0).title());
        assertEquals(1, second.size());
        assertEquals("最早会话", second.get(0).title());
    }

    /**
     * 验证临时文件元数据、用户隔离、状态迁移和问题关联持久化。
     */
    @Test
    void persistsTemporaryFilesAndQuestionReferences() {
        final UUID conversationId = createConversation();
        final UUID fileId = UUID.randomUUID();
        final UUID questionId = UUID.randomUUID();
        final TemporaryFile uploading = new TemporaryFile(
                fileId, conversationId, OWNER_ID, "产品编号.txt", "text/plain", 12L,
                "sha256", "temporary/object", TemporaryFile.Usage.QUERY_INPUT,
                TemporaryFile.Status.UPLOADING, NOW, NOW);

        temporaryFiles.create(uploading, "upload-1");
        final TemporaryFile ready = temporaryFiles.updateStatus(
                        fileId, TemporaryFile.Status.READY, NOW.plusSeconds(1))
                .orElseThrow(AssertionError::new);
        final QuestionFileReference reference = new QuestionFileReference(
                fileId, TemporaryFile.Usage.QUERY_INPUT);
        temporaryFiles.attachToQuestion(
                questionId, Collections.singletonList(reference), NOW.plusSeconds(2));

        assertEquals(TemporaryFile.Status.READY, ready.status());
        assertEquals(1, temporaryFiles.list(OWNER_ID, conversationId, 5).size());
        assertFalse(temporaryFiles.findActive("another-user", conversationId, fileId).isPresent());
        assertEquals("QUERY_INPUT", jdbcTemplate.queryForObject(
                "SELECT usage_type FROM qa_question_file WHERE question_id = ? AND file_id = ?",
                String.class, questionId.toString(), fileId.toString()));
        assertTrue(temporaryFiles.markDeletePending(
                OWNER_ID, conversationId, fileId, NOW.plusSeconds(3)).isPresent());
        assertFalse(temporaryFiles.findActive(OWNER_ID, conversationId, fileId).isPresent());
    }

    /**
     * 验证问题回答消息持久化及幂等查询。
     */
    @Test
    void persistsQuestionAnswerMessagesAndIdempotentLookup() {
        final UUID conversationId = createConversation();
        final UUID questionId = UUID.randomUUID();
        final UUID answerId = UUID.randomUUID();
        final UUID traceId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();

        temporaryFiles.create(new TemporaryFile(
                fileId, conversationId, OWNER_ID, "批量产品.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 128L,
                "attachment-sha256", "temporary/history-object", TemporaryFile.Usage.AUTO,
                TemporaryFile.Status.UPLOADING, NOW, NOW), "history-upload");
        temporaryFiles.updateStatus(fileId, TemporaryFile.Status.READY, NOW.plusSeconds(1));

        final AnswerSnapshot created = answers.create(
                OWNER_ID, conversationId, questionId, answerId, traceId,
                null, "产品费率是多少？", "submit-1", NOW.plusSeconds(2));
        temporaryFiles.attachToQuestion(
                questionId,
                Collections.singletonList(new QuestionFileReference(
                        fileId, TemporaryFile.Usage.QUERY_INPUT)),
                NOW.plusSeconds(3));
        final AnswerSnapshot duplicate = answers.create(
                OWNER_ID, conversationId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, "重复提交", "submit-1", NOW.plusSeconds(4));

        assertEquals(answerId, created.id());
        assertEquals(answerId, duplicate.id());
        assertEquals("产品费率是多少？", answers.findQuestion(OWNER_ID, answerId).orElse(""));
        final List<ChatMessage> messages = conversations.listMessages(OWNER_ID, conversationId, 10);
        assertEquals(2, messages.size());
        assertEquals(ChatMessage.Role.USER, messages.get(0).role());
        assertEquals(1, messages.get(0).attachments().size());
        assertEquals("批量产品.xlsx", messages.get(0).attachments().get(0).name());
        assertEquals(TemporaryFile.Usage.QUERY_INPUT, messages.get(0).attachments().get(0).usage());
        assertEquals(TemporaryFile.Status.READY, messages.get(0).attachments().get(0).status());
        assertEquals(ChatMessage.Role.ASSISTANT, messages.get(1).role());
        assertTrue(messages.get(1).attachments().isEmpty());
        assertEquals(AnswerSnapshot.Status.PENDING, messages.get(1).answerStatus());

        final UUID regeneratedQuestionId = UUID.randomUUID();
        final UUID regeneratedAnswerId = UUID.randomUUID();
        answers.create(
                OWNER_ID, conversationId, regeneratedQuestionId, regeneratedAnswerId,
                UUID.randomUUID(), answerId, "产品费率是多少？", "regenerate-1", NOW.plusSeconds(4));
        final List<QuestionFileReference> originalReferences = temporaryFiles.listQuestionReferences(questionId);
        temporaryFiles.attachToQuestion(regeneratedQuestionId, originalReferences, NOW.plusSeconds(4));
        final List<ChatMessage> regeneratedMessages = conversations.listMessages(
                OWNER_ID, conversationId, 10);
        assertEquals(4, regeneratedMessages.size());
        assertEquals(ChatMessage.Role.USER, regeneratedMessages.get(2).role());
        assertEquals("产品费率是多少？", regeneratedMessages.get(2).content());
        assertEquals("批量产品.xlsx", regeneratedMessages.get(2).attachments().get(0).name());
        assertEquals(ChatMessage.Role.ASSISTANT, regeneratedMessages.get(3).role());

        temporaryFiles.markDeletePending(OWNER_ID, conversationId, fileId, NOW.plusSeconds(5));
        final List<ChatMessage> restored = conversations.listMessages(OWNER_ID, conversationId, 10);
        assertEquals(TemporaryFile.Status.DELETE_PENDING,
                restored.get(0).attachments().get(0).status());
        assertEquals("批量产品.xlsx", restored.get(0).attachments().get(0).name());
    }

    /**
     * 验证上下文持久化的用户隔离与乐观版本保护。
     */
    @Test
    void persistsClarificationContextWithOwnerIsolationAndOptimisticVersion() {
        final UUID conversationId = createConversation();
        final UUID entitySourceMessageId = UUID.randomUUID();
        final QueryIntent pendingIntent = new QueryIntent(
                QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO, 1.0D, Collections.emptyMap());
        final ClarificationRequest clarification = new ClarificationRequest(
                ClarificationRequest.Reason.AMBIGUOUS_ENTITY,
                "productReference",
                "请选择产品",
                pendingIntent,
                java.util.Arrays.asList(
                        new EntityCandidate("P001", "悦享一号"),
                        new EntityCandidate("P002", "悦享二号（P002）", "悦享二号")));
        final ConversationContext context = new ConversationContext(
                conversationId, OWNER_ID, 0L, pendingIntent.type(),
                Collections.singletonMap("documentType", "FEE_NOTICE"),
                Collections.singletonMap("documentType", entitySourceMessageId),
                UUID.randomUUID(), clarification, NOW.plusSeconds(1));

        assertTrue(contexts.save(context, 0L));
        assertFalse(contexts.save(context, 0L));
        assertFalse(contexts.find("another-user", conversationId).isPresent());
        final ConversationContext restored = contexts.find(OWNER_ID, conversationId)
                .orElseThrow(AssertionError::new);

        assertEquals(1L, restored.version());
        assertEquals("FEE_NOTICE", restored.entities().get("documentType"));
        assertEquals(entitySourceMessageId,
                restored.entitySourceMessageIds().get("documentType"));
        assertEquals(ClarificationRequest.Reason.AMBIGUOUS_ENTITY,
                restored.pendingClarification().reason());
        assertEquals("P002", restored.pendingClarification().candidates().get(1).reference());
        assertEquals("悦享二号", restored.pendingClarification().candidates().get(1).canonicalName());
    }

    /**
     * 验证旧版上下文缺少逐实体来源时保持未验证状态，不得用全局问题 ID 冒充来源。
     */
    @Test
    void keepsLegacyEntityWithoutSourceMessageUnverified() {
        final UUID conversationId = createConversation();
        final UUID globalSourceQuestionId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO qa_conversation_context "
                        + "(conversation_id, owner_id, version, state_json, source_question_id, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                conversationId.toString(),
                OWNER_ID,
                1L,
                "{\"schemaVersion\":1,\"lastIntent\":\"PRODUCT_TRADE_BASIC_INFO\","
                        + "\"entities\":{\"productReference\":\"P001\"},\"pending\":null}",
                globalSourceQuestionId.toString(),
                java.sql.Timestamp.from(NOW));

        final ConversationContext restored = contexts.find(OWNER_ID, conversationId)
                .orElseThrow(AssertionError::new);

        assertEquals("P001", restored.entities().get("productReference"));
        assertEquals(globalSourceQuestionId, restored.sourceQuestionId());
        assertTrue(restored.entitySourceMessageIds().isEmpty());
    }

    /**
     * 保存最终内容并将回答转换为完成状态。
     */
    @Test
    void completesAnswerAndUpdatesAssistantMessageInOneRepositoryOperation() {
        final UUID conversationId = createConversation();
        final UUID answerId = createAnswer(conversationId, "complete-1");
        final Instant completedAt = NOW.plusSeconds(5);

        answers.transitionStatus(answerId, AnswerSnapshot.Status.GENERATING);
        answers.complete(answerId, "有依据的回答", completedAt);

        final AnswerSnapshot completed = answers.find(OWNER_ID, answerId).orElseThrow(AssertionError::new);
        assertEquals(AnswerSnapshot.Status.COMPLETED, completed.status());
        assertEquals("有依据的回答", completed.content());
        assertEquals(completedAt, completed.completedAt());
        final List<ChatMessage> messages = conversations.listMessages(OWNER_ID, conversationId, 10);
        assertEquals("有依据的回答", messages.get(messages.size() - 1).content());
        assertEquals(AnswerSnapshot.Status.COMPLETED, messages.get(messages.size() - 1).answerStatus());
    }

    /**
     * 验证执行过程和引用产物可按会话批量恢复。
     */
    @Test
    void persistsAndRestoresExecutionEventsAndArtifacts() {
        final UUID conversationId = createConversation();
        final UUID answerId = createAnswer(conversationId, "event-history");
        answerEvents.append(answerId, "retrieval_started", "STARTED", NOW.plusSeconds(2));
        answerEvents.append(answerId, "citation", "产品说明书-v2", NOW.plusSeconds(3));
        answerEvents.append(answerId, "delta", "不应重复返回的正文", NOW.plusSeconds(4));
        answerEvents.append(answerId, "completed", "stop", NOW.plusSeconds(5));

        final List<ChatMessage> messages = new ConversationService(
                conversations, answerEvents, java.time.Clock.systemUTC())
                .messages(OWNER_ID, conversationId, 10);
        final ChatMessage assistant = messages.get(messages.size() - 1);

        assertEquals(3, assistant.executionEvents().size());
        assertEquals("retrieval_started", assistant.executionEvents().get(0).type());
        assertEquals("citation", assistant.executionEvents().get(1).type());
        assertEquals("completed", assistant.executionEvents().get(2).type());
    }

    /**
     * 验证最大配置长度中文回答及消息可以完整保存。
     */
    @Test
    void persistsMaximumConfiguredChineseAnswerInAnswerAndMessage() {
        final UUID conversationId = createConversation();
        final UUID answerId = createAnswer(conversationId, "large-complete");
        final StringBuilder content = new StringBuilder(30000);
        for (int index = 0; index < 30000; index++) {
            content.append('答');
        }

        answers.transitionStatus(answerId, AnswerSnapshot.Status.GENERATING);
        assertTrue(answers.savePartial(answerId, content.toString()));
        assertTrue(answers.complete(answerId, content.toString(), NOW.plusSeconds(5)));

        assertEquals(30000, answers.find(OWNER_ID, answerId).get().content().length());
        final List<ChatMessage> messages = conversations.listMessages(OWNER_ID, conversationId, 10);
        assertEquals(30000, messages.get(messages.size() - 1).content().length());
    }

    /**
     * 验证回答进入模型生成阶段后可以持久化 HiAgent 应用会话 ID。
     */
    @Test
    void persistsHiAgentAppConversationIdOnGeneratingAnswer() {
        final UUID conversationId = createConversation();
        final UUID answerId = createAnswer(conversationId, "hiagent-conversation");

        answers.transitionStatus(answerId, AnswerSnapshot.Status.GENERATING);
        assertTrue(answers.recordAppConversationId(answerId, "company-conversation-1"));

        assertEquals("company-conversation-1", jdbcTemplate.queryForObject(
                "SELECT app_conversation_id FROM qa_answer WHERE public_id = ?",
                String.class, answerId.toString()));
    }

    /**
     * 验证反馈新增更新及失败信息持久化。
     */
    @Test
    void upsertsFeedbackAndPersistsFailureDetails() {
        final UUID conversationId = createConversation();
        final UUID answerId = createAnswer(conversationId, "failure-1");

        answers.upsertFeedback(OWNER_ID, answerId, QuestionAnswerUseCase.Feedback.LIKE, NOW.plusSeconds(2));
        answers.upsertFeedback(OWNER_ID, answerId, QuestionAnswerUseCase.Feedback.DISLIKE, NOW.plusSeconds(3));
        answers.fail(
                answerId, AnswerSnapshot.Status.INCOMPLETE,
                "部分回答", "COMPANY_MODEL_UNAVAILABLE", NOW.plusSeconds(4));

        assertEquals("DISLIKE", jdbcTemplate.queryForObject(
                "SELECT feedback_type FROM qa_answer_feedback WHERE answer_id = ? AND owner_id = ?",
                String.class, answerId.toString(), OWNER_ID));
        final AnswerSnapshot failed = answers.find(OWNER_ID, answerId).orElseThrow(AssertionError::new);
        assertEquals(AnswerSnapshot.Status.INCOMPLETE, failed.status());
        assertEquals("COMPANY_MODEL_UNAVAILABLE", failed.errorCode());
        assertEquals("部分回答", failed.content());
    }

    /**
     * 验证停止任务写入租约领取和完成持久化。
     */
    @Test
    void persistsClaimsAndCompletesCancellationTask() {
        final UUID conversationId = createConversation();
        final UUID answerId = createAnswer(conversationId, "cancel-1");

        final CancellationRepositoryPort.RequestResult requested = cancellations.request(
                OWNER_ID, answerId, "cancel-request-1",
                AnswerCancellationUseCase.CancellationReason.USER_REQUESTED,
                NOW.plusSeconds(2), NOW.plusSeconds(30));
        final CancellationRepositoryPort.RequestResult duplicate = cancellations.request(
                OWNER_ID, answerId, "cancel-request-1",
                AnswerCancellationUseCase.CancellationReason.USER_REQUESTED,
                NOW.plusSeconds(3), NOW.plusSeconds(30));

        assertTrue(requested.newlyRequested());
        assertFalse(duplicate.newlyRequested());
        assertEquals(AnswerSnapshot.Status.CANCEL_REQUESTED, duplicate.answer().status());
        assertEquals(answerId, cancellations.findDispatchable(NOW.plusSeconds(2), 10).get(0));
        final CancellationRepositoryPort.Task task = cancellations.claim(
                answerId, "worker-1", NOW.plusSeconds(2), NOW.plusSeconds(12), 3)
                .orElseThrow(AssertionError::new);
        assertEquals(AnswerSnapshot.Status.PENDING.name(), task.cancelledStage());
        assertFalse(cancellations.claim(
                answerId, "worker-2", NOW.plusSeconds(11), NOW.plusSeconds(21), 3).isPresent());
        assertTrue(cancellations.claim(
                answerId, "worker-2", NOW.plusSeconds(12), NOW.plusSeconds(22), 3).isPresent());
        assertFalse(cancellations.claim(
                answerId, "worker-3", NOW.plusSeconds(22), NOW.plusSeconds(32), 2).isPresent());
        assertTrue(cancellations.markCancelled(answerId, "worker-2", NOW.plusSeconds(23)));
        assertEquals(AnswerSnapshot.Status.CANCELLED, answers.find(OWNER_ID, answerId).get().status());
    }

    /**
     * 验证最后一次停止领取后进程崩溃会在租约过期时原子收敛为失败且不再热扫描。
     */
    @Test
    void convergesExpiredExhaustedCancellationAfterWorkerCrash() {
        final UUID conversationId = createConversation();
        final UUID answerId = createAnswer(conversationId, "cancel-crash");
        cancellations.request(
                OWNER_ID, answerId, "cancel-crash-request",
                AnswerCancellationUseCase.CancellationReason.USER_REQUESTED,
                NOW.plusSeconds(2), NOW.plusSeconds(2));

        assertTrue(cancellations.claim(
                answerId, "worker-1", NOW.plusSeconds(2), NOW.plusSeconds(12), 2).isPresent());
        assertTrue(cancellations.claim(
                answerId, "worker-2", NOW.plusSeconds(12), NOW.plusSeconds(22), 2).isPresent());
        assertTrue(cancellations.markExpiredExhausted(
                answerId, NOW.plusSeconds(22), 2, "CANCELLATION_ATTEMPTS_EXHAUSTED"));

        final AnswerSnapshot failed = answers.find(OWNER_ID, answerId).orElseThrow(AssertionError::new);
        assertEquals(AnswerSnapshot.Status.CANCEL_FAILED, failed.status());
        assertEquals("CANCELLATION_ATTEMPTS_EXHAUSTED", failed.cancelErrorCode());
        assertTrue(cancellations.findDispatchable(NOW.plusSeconds(23), 10).isEmpty());
        assertTrue(!cancellations.markExpiredExhausted(
                answerId, NOW.plusSeconds(24), 2, "CANCELLATION_ATTEMPTS_EXHAUSTED"));
    }

    /**
     * 验证回答进入终态后不能再次请求停止。
     */
    @Test
    void rejectsCancellationAfterAnswerIsTerminal() {
        final UUID conversationId = createConversation();
        final UUID answerId = createAnswer(conversationId, "terminal-cancel");
        answers.complete(answerId, "完成", NOW.plusSeconds(2));

        assertThrows(AnswerAlreadyTerminalException.class, () -> cancellations.request(
                OWNER_ID, answerId, "cancel-terminal",
                AnswerCancellationUseCase.CancellationReason.USER_REQUESTED,
                NOW.plusSeconds(3), NOW.plusSeconds(30)));
    }

    /**
     * 创建公司模型会话。
     *
     * @return 创建公司模型会话。
     */
    private UUID createConversation() {
        final UUID conversationId = UUID.randomUUID();
        conversations.create(conversationId, OWNER_ID, "问答会话", NOW);
        return conversationId;
    }

    /**
     * 创建测试回答及其占位消息。
     *
     * @param conversationId 会话 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 创建测试回答及其占位消息。
     */
    private UUID createAnswer(final UUID conversationId, final String idempotencyKey) {
        final UUID answerId = UUID.randomUUID();
        answers.create(
                OWNER_ID, conversationId, UUID.randomUUID(), answerId, UUID.randomUUID(),
                null, "问题", idempotencyKey, NOW.plusSeconds(1));
        return answerId;
    }

    /**
     * 持久化集成测试所需的 JSON 装配配置。
     */
    @TestConfiguration
    static class JacksonTestConfiguration {
        /**
         * 返回JSON 对象映射器。
         *
         * @return JSON 对象映射器。
         */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
