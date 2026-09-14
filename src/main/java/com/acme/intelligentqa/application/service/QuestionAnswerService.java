package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.application.workflow.QuestionWorkflowContext;
import com.acme.intelligentqa.application.workflow.QuestionWorkflowEngine;
import com.acme.intelligentqa.application.workflow.WorkflowNodeCode;
import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.common.error.AnswerAlreadyTerminalException;
import com.acme.intelligentqa.common.error.GenerationCancelledException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.AgentType;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.ClarificationRequest;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.ConversationContext;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.model.QueryScenario;
import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.model.QuestionSubmission;
import com.acme.intelligentqa.domain.port.in.QuestionAnswerUseCase;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import com.acme.intelligentqa.domain.port.out.AnswerRepositoryPort;
import com.acme.intelligentqa.domain.port.out.CancellationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.ConversationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.ConversationContextRepositoryPort;
import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排问题持久化、意图理解、双通道取证、证据对账和流式回答生成。
 */
@Service
public class QuestionAnswerService implements QuestionAnswerUseCase {

    /**
     * 事件元数据类型标识。
     */
    private static final String EVENT_METADATA = "metadata";
    /** 工作流计划已选定事件。 */
    private static final String EVENT_WORKFLOW_PLAN_SELECTED = "workflow_plan_selected";
    /** 知识库检索完成事件。 */
    private static final String EVENT_KNOWLEDGE_RETRIEVAL_COMPLETED = "knowledge_retrieval_completed";
    /** 业务数据库查询完成事件。 */
    private static final String EVENT_BUSINESS_QUERY_COMPLETED = "business_query_completed";
    /** 证据对账开始事件。 */
    private static final String EVENT_EVIDENCE_RECONCILIATION_STARTED = "evidence_reconciliation_started";
    /** 回答生成完成事件。 */
    private static final String EVENT_GENERATION_COMPLETED = "generation_completed";
    /**
     * 请求幂等头名称。
     */
    private static final String IDEMPOTENCY_KEY = "idempotencyKey";
    /**
     * 会话仓储。
     */
    private final ConversationRepositoryPort conversationRepository;
    /** 统一提问所属会话解析器。 */
    private final QuestionSubmissionConversationResolver submissionConversationResolver;
    /**
     * 会话上下文仓储。
     */
    private final ConversationContextRepositoryPort contextRepository;
    /**
     * 回答仓储。
     */
    private final AnswerRepositoryPort answerRepository;
    /** 问题附件校验与关联协调器。 */
    private final QuestionFileCoordinator questionFileCoordinator;
    /** 回答创建和重新生成的幂等指纹校验器。 */
    private final AnswerIdempotencyValidator idempotencyValidator;
    /** 结构化会话上下文写入器。 */
    private final ConversationContextWriter contextWriter;
    /**
     * 停止任务仓储。
     */
    private final CancellationRepositoryPort cancellationRepository;
    /**
     * 场景计划驱动的轻量问答工作流执行器。
     */
    private final QuestionWorkflowEngine workflowEngine;
    /**
     * 回答事件端口。
     */
    private final AnswerEventPort eventPort;
    /**
     * 配置参数。
     */
    private final QaProperties properties;
    /**
     * 停止任务分发器。
     */
    private final CancellationDispatcher cancellationDispatcher;
    /**
     * 系统时钟。
     */
    private final Clock clock;
    /**
     * 异步任务执行器。
     */
    private final Executor executor;

    /**
     * 创建 {@code QuestionAnswerService} 实例。
     *
     * @param conversationRepository 会话仓储。
     *
     * @param contextRepository 会话上下文仓储。
     *
     * @param answerRepository 回答仓储。
     *
     * @param questionFileCoordinator 问题附件校验与关联协调器。
     *
     * @param cancellationRepository 停止任务仓储。
     *
     * @param workflowEngine 场景计划驱动的轻量问答工作流执行器。
     *
     * @param eventPort 回答事件端口。
     *
     * @param properties 配置参数。
     *
     * @param cancellationDispatcher 停止任务分发器。
     *
     * @param clock 系统时钟。
     *
     * @param executor 异步任务执行器。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public QuestionAnswerService(
            final ConversationRepositoryPort conversationRepository,
            final ConversationContextRepositoryPort contextRepository,
            final AnswerRepositoryPort answerRepository,
            final QuestionFileCoordinator questionFileCoordinator,
            final CancellationRepositoryPort cancellationRepository,
            final QuestionWorkflowEngine workflowEngine,
            final AnswerEventPort eventPort,
            final QaProperties properties,
            final CancellationDispatcher cancellationDispatcher,
            final Clock clock,
            @Qualifier("qaExecutor") final Executor executor) {
        this.conversationRepository = conversationRepository;
        this.submissionConversationResolver = new QuestionSubmissionConversationResolver(
                conversationRepository, clock);
        this.contextRepository = contextRepository;
        this.answerRepository = answerRepository;
        this.questionFileCoordinator = questionFileCoordinator;
        this.idempotencyValidator = new AnswerIdempotencyValidator(
                answerRepository, questionFileCoordinator);
        this.contextWriter = new ConversationContextWriter(contextRepository, clock);
        this.cancellationRepository = cancellationRepository;
        this.workflowEngine = workflowEngine;
        this.eventPort = eventPort;
        this.properties = properties;
        this.cancellationDispatcher = cancellationDispatcher;
        this.clock = clock;
        this.executor = executor;
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
    @Override
    @Transactional
    public AnswerSnapshot submit(
            final String ownerId,
            final UUID conversationId,
            final String question,
            final String idempotencyKey) {
        return submit(ownerId, conversationId, question, Collections.emptyList(), idempotencyKey);
    }

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
    @Override
    @Transactional
    public AnswerSnapshot submit(
            final String ownerId,
            final UUID conversationId,
            final String question,
            final List<QuestionFileReference> files,
            final String idempotencyKey) {
        return submitWithAgent(
                ownerId, conversationId, null, question, files, idempotencyKey).answer();
    }

    /**
     * 使用一个事务提交首次或后续问题，并返回会话和回答受理结果。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID；首次提问时为空。
     * @param agentType 用户选择的 Agent 类型。
     * @param question 用户问题。
     * @param files 本次问题引用的临时文件；第一阶段必须为空。
     * @param idempotencyKey 覆盖会话、问题和回答创建的幂等键。
     * @return 同一事务内持久化的会话和回答结果。
     */
    @Override
    @Transactional
    public QuestionSubmission submitQuestion(
            final String ownerId,
            final UUID conversationId,
            final AgentType agentType,
            final String question,
            final List<QuestionFileReference> files,
            final String idempotencyKey) {
        return submitWithAgent(ownerId, conversationId, agentType, question, files, idempotencyKey);
    }

    /**
     * 校验输入并按后端查询场景受理问题。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param requestedAgentType 首次提问选择的 Agent 类型；已有会话可为空。
     * @param question 用户问题。
     * @param files 本次问题引用的临时文件。
     * @param idempotencyKey 幂等键。
     * @return 同一事务内持久化的会话与回答结果。
     */
    private QuestionSubmission submitWithAgent(
            final String ownerId,
            final UUID conversationId,
            final AgentType requestedAgentType,
            final String question,
            final List<QuestionFileReference> files,
            final String idempotencyKey) {
        if (files != null && !files.isEmpty()) {
            throw new IllegalArgumentException("第一阶段不支持文件上传");
        }
        final String validOwner = ApplicationSupport.requireText(ownerId, "ownerId");
        final String validQuestion = validateQuestion(question);
        final String validKey = ApplicationSupport.requireText(idempotencyKey, IDEMPOTENCY_KEY);
        final QuestionSubmissionConversationResolver.Resolution resolution =
                submissionConversationResolver.resolve(
                        validOwner, conversationId, validQuestion, requestedAgentType, validKey);
        final boolean createsConversation = resolution.created();
        final Instant now = resolution.now();
        final Conversation conversation = resolution.conversation();
        final QueryScenario scenario = conversation.agentType().queryScenario();
        final AnswerSnapshot existing = answerRepository.findByIdempotencyKey(validOwner, validKey).orElse(null);
        if (existing != null) {
            return new QuestionSubmission(
                    conversation,
                    idempotencyValidator.requireMatchingSubmission(
                            validOwner, existing, conversation.id(), validQuestion, files),
                    createsConversation);
        }
        final List<QuestionFileReference> validFiles = questionFileCoordinator.validate(
                validOwner, conversation.id(), files);
        final List<ChatMessage> history = createsConversation
                ? Collections.<ChatMessage>emptyList()
                : loadHistory(validOwner, conversation.id());
        final UUID questionId = UUID.randomUUID();
        final UUID requestedAnswerId = UUID.randomUUID();
        final AnswerSnapshot answer = answerRepository.create(
                validOwner,
                conversation.id(),
                questionId,
                requestedAnswerId,
                UUID.randomUUID(),
                null,
                validQuestion,
                validKey,
                now);
        // 唯一键并发竞争可能让仓储返回另一请求已创建的回答；此时必须核对请求指纹且禁止重复投递。
        if (!requestedAnswerId.equals(answer.id())) {
            return new QuestionSubmission(
                    conversation,
                    idempotencyValidator.requireMatchingSubmission(
                            validOwner, answer, conversation.id(), validQuestion, files),
                    createsConversation);
        }
        questionFileCoordinator.attach(questionId, validFiles, now);
        startAfterCommit(validOwner, answer, validQuestion, history, scenario);
        return new QuestionSubmission(conversation, answer, createsConversation);
    }

    /**
     * 识别前端选择的 Agent 类型并路由到对应问答处理链路。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param agentType 用户选择的 Agent 类型。
     * @param question 用户问题。
     * @param files 本次问题引用的临时文件。
     * @param idempotencyKey 幂等键。
     * @return 问题受理后的回答快照。
     */
    @Override
    @Transactional
    public AnswerSnapshot submit(
            final String ownerId,
            final UUID conversationId,
            final AgentType agentType,
            final String question,
            final List<QuestionFileReference> files,
            final String idempotencyKey) {
        return submitWithAgent(
                ownerId, conversationId, agentType, question, files, idempotencyKey).answer();
    }

    /**
     * 将原问题作为新的问答轮次重新发起，并保留与原回答的追溯关系。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 新问答轮次的回答快照。
     */
    @Override
    @Transactional
    public AnswerSnapshot regenerate(final String ownerId, final UUID answerId, final String idempotencyKey) {
        final AnswerSnapshot original = getAnswer(ownerId, answerId);
        final Conversation conversation = submissionConversationResolver.lockExisting(
                ownerId, original.conversationId());
        if (original.status() == AnswerSnapshot.Status.NEEDS_CLARIFICATION) {
            throw new AnswerAlreadyTerminalException(original.status().name());
        }
        final String question = answerRepository.findQuestion(ownerId, answerId)
                .orElseThrow(() -> new ResourceNotFoundException("question not found for answer: " + answerId));
        final String validKey = ApplicationSupport.requireText(idempotencyKey, IDEMPOTENCY_KEY);
        final AnswerSnapshot existing = answerRepository.findByIdempotencyKey(ownerId, validKey).orElse(null);
        if (existing != null) {
            return idempotencyValidator.requireMatchingRegeneration(existing, original);
        }
        final List<ChatMessage> history = loadHistory(ownerId, original.conversationId());
        final UUID newQuestionId = UUID.randomUUID();
        final Instant now = Instant.now(clock);
        final UUID requestedAnswerId = UUID.randomUUID();
        final AnswerSnapshot answer = answerRepository.create(
                ownerId,
                original.conversationId(),
                newQuestionId,
                requestedAnswerId,
                UUID.randomUUID(),
                original.id(),
                question,
                validKey,
                now);
        // 并发重放只允许复用同一原回答派生出的轮次，禁止跨操作或跨回答复用幂等键。
        if (!requestedAnswerId.equals(answer.id())) {
            return idempotencyValidator.requireMatchingRegeneration(answer, original);
        }
        // 第一期重新生成只复用原问题文本，不复制历史附件，避免绕过文件能力开关。
        startAfterCommit(ownerId, answer, question, history, conversation.agentType().queryScenario());
        return answer;
    }

    /**
     * 读取回答的持久化快照。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @return 回答快照。
     */
    @Override
    @Transactional(readOnly = true)
    public AnswerSnapshot getAnswer(final String ownerId, final UUID answerId) {
        return answerRepository.find(
                        ApplicationSupport.requireText(ownerId, "ownerId"),
                        Objects.requireNonNull(answerId))
                .orElseThrow(() -> new ResourceNotFoundException("answer not found: " + answerId));
    }

    /**
     * 保存用户对回答的评价。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param feedback 用户反馈。
     */
    @Override
    @Transactional
    public void recordFeedback(final String ownerId, final UUID answerId, final Feedback feedback) {
        getAnswer(ownerId, answerId);
        answerRepository.upsertFeedback(ownerId, answerId, Objects.requireNonNull(feedback), Instant.now(clock));
    }

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
    @Override
    @Transactional(readOnly = true)
    public Subscription subscribe(
            final String ownerId,
            final UUID answerId,
            final long afterSequence,
            final Consumer<AnswerEvent> consumer) {
        getAnswer(ownerId, answerId);
        if (afterSequence < 0) {
            throw new IllegalArgumentException("afterSequence must not be negative");
        }
        final AnswerEventPort.Subscription subscription = eventPort.subscribe(answerId, afterSequence, consumer);
        return subscription::close;
    }

    /**
     * 编排证据获取并流式生成回答。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answer 回答快照。
     *
     * @param question 用户问题。
     *
     * @param history 最近对话历史。
     *
     * @param scenario 后端查询场景。
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void generate(
            final String ownerId,
            final AnswerSnapshot answer,
            final String question,
            final List<ChatMessage> history,
            final QueryScenario scenario) {
        final StringBuilder content = new StringBuilder();
        try {
            final ConversationContext conversationContext = contextRepository.find(
                    ownerId, answer.conversationId()).orElse(null);
            final QuestionWorkflowContext workflowContext = new QuestionWorkflowContext(
                    ownerId,
                    answer,
                    question,
                    history,
                    conversationContext,
                    scenario,
                    chunk -> appendChunk(answer.id(), content, chunk),
                    new GenerationControl(answer.id()),
                    () -> ensureNotCancelled(answer.id()));
            workflowEngine.execute(workflowContext, this::beforeNode, this::afterNode);
            if (workflowContext.clarification() != null) {
                return;
            }
            ensureNotCancelled(answer.id());
            if (answerRepository.complete(answer.id(), content.toString(), Instant.now(clock))) {
                eventPort.publish(answer.id(), "completed", workflowContext.finishReason());
            } else {
                cancellationDispatcher.dispatch(answer.id());
            }
        } catch (final GenerationCancelledException exception) {
            cancellationDispatcher.dispatch(answer.id());
        } catch (final DependencyUnavailableException exception) {
            fail(answer.id(), content, exception);
        } catch (final IllegalArgumentException exception) {
            fail(answer.id(), content, exception);
        } catch (final RuntimeException exception) {
            fail(answer.id(), content, exception);
        }
    }

    /**
     * 在节点执行前保持现有回答状态机和 SSE 阶段事件语义。
     *
     * @param nodeCode 即将执行的节点编码。
     * @param context 当前问答工作流上下文。
     */
    private void beforeNode(
            final WorkflowNodeCode nodeCode,
            final QuestionWorkflowContext context) {
        switch (nodeCode) {
            case QUESTION_UNDERSTANDING:
                eventPort.publish(
                        context.answer().id(),
                        EVENT_WORKFLOW_PLAN_SELECTED,
                        context.scenario().name() + "|" + context.planVersion());
                eventPort.publish(context.answer().id(), "intent_recognition_started", "STARTED");
                break;
            case KNOWLEDGE_RETRIEVAL:
                update(context.answer().id(), AnswerSnapshot.Status.RETRIEVING, "retrieval_started");
                break;
            case BUSINESS_DATA_QUERY:
                update(context.answer().id(), AnswerSnapshot.Status.QUERYING, "business_query_started");
                break;
            case EVIDENCE_RECONCILIATION:
                eventPort.publish(context.answer().id(), EVENT_EVIDENCE_RECONCILIATION_STARTED, "STARTED");
                break;
            case ANSWER_GENERATION:
                update(context.answer().id(), AnswerSnapshot.Status.GENERATING, "generation_started");
                if (context.evidenceAssessment().manualReviewRequired()) {
                    eventPort.publish(context.answer().id(), "manual_review_required", "EVIDENCE_CONFLICT");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 在节点成功后保存结构化上下文并发布已有的结果型事件。
     *
     * @param nodeCode 已成功执行的节点编码。
     * @param context 当前问答工作流上下文。
     */
    private void afterNode(
            final WorkflowNodeCode nodeCode,
            final QuestionWorkflowContext context) {
        switch (nodeCode) {
            case QUESTION_UNDERSTANDING:
                afterUnderstanding(context);
                break;
            case KNOWLEDGE_RETRIEVAL:
                publishCitations(context.answer().id(), context.knowledge());
                eventPort.publish(
                        context.answer().id(),
                        EVENT_KNOWLEDGE_RETRIEVAL_COMPLETED,
                        Integer.toString(context.knowledge().size()));
                break;
            case BUSINESS_DATA_QUERY:
                eventPort.publish(
                        context.answer().id(),
                        EVENT_BUSINESS_QUERY_COMPLETED,
                        Integer.toString(context.businessFacts().size()));
                break;
            case EVIDENCE_RECONCILIATION:
                eventPort.publish(
                        context.answer().id(),
                        "evidence_assessed",
                        context.evidenceAssessment().status().name()
                                + "|" + context.evidenceAssessment().conflictFields().size());
                break;
            case ANSWER_GENERATION:
                eventPort.publish(context.answer().id(), EVENT_GENERATION_COMPLETED, "READY_TO_PERSIST");
                break;
            default:
                break;
        }
    }


    /**
     * 保存问题理解结果；追问分支在此收敛为合法终态。
     *
     * @param context 当前问答工作流上下文。
     */
    private void afterUnderstanding(final QuestionWorkflowContext context) {
        final ClarificationRequest clarification = context.clarification();
        contextWriter.save(
                context.ownerId(),
                context.answer(),
                context.conversationContext(),
                context.intent(),
                clarification,
                context.entitySourceMessageIds());
        if (clarification != null) {
            completeClarification(context.answer().id(), clarification);
            return;
        }
        eventPort.publish(
                context.answer().id(),
                "intent_recognized",
                context.intent().type().name()
                        + "|" + Math.round(context.intent().confidence() * 100.0D)
                        + "|" + context.intent().entities().size());
    }

    /**
     * 注册事务提交后的生成任务投递。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answer 回答快照。
     *
     * @param question 用户问题。
     *
     * @param history 最近对话历史。
     *
     * @param scenario 后端查询场景。
     */
    private void startAfterCommit(
            final String ownerId,
            final AnswerSnapshot answer,
            final String question,
            final List<ChatMessage> history,
            final QueryScenario scenario) {
        final Runnable start = () -> launchGeneration(ownerId, answer, question, history, scenario);
        ApplicationSupport.runAfterCommit(start);
    }

    /**
     * 向有界线程池投递生成任务。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param answer 回答快照。
     *
     * @param question 用户问题。
     *
     * @param history 最近对话历史。
     *
     * @param scenario 后端查询场景。
     */
    private void launchGeneration(
            final String ownerId,
            final AnswerSnapshot answer,
            final String question,
            final List<ChatMessage> history,
            final QueryScenario scenario) {
        eventPort.publish(answer.id(), EVENT_METADATA, answer.traceId().toString());
        try {
            executor.execute(() -> generate(ownerId, answer, question, history, scenario));
        } catch (final RejectedExecutionException exception) {
            final boolean failed = answerRepository.fail(
                    answer.id(),
                    AnswerSnapshot.Status.FAILED,
                    "",
                    "SYSTEM_OVERLOADED",
                    Instant.now(clock));
            if (failed) {
                eventPort.publish(answer.id(), "error", "SYSTEM_OVERLOADED");
            } else {
                cancellationDispatcher.dispatch(answer.id());
            }
        }
    }

    /**
     * 记录失败或未完整状态，并保留已生成内容。
     *
     * @param answerId 回答 ID。
     *
     * @param content 内容。
     *
     * @param exception 待转换的原始异常。
     */
    private void fail(final UUID answerId, final StringBuilder content, final RuntimeException exception) {
        final String errorCode = errorCode(exception);
        final AnswerSnapshot.Status status = content.length() == 0
                ? AnswerSnapshot.Status.FAILED : AnswerSnapshot.Status.INCOMPLETE;
        if (answerRepository.fail(answerId, status, content.toString(), errorCode, Instant.now(clock))) {
            eventPort.publish(answerId, "error", errorCode);
        } else {
            cancellationDispatcher.dispatch(answerId);
        }
    }

    /**
     * 追加回答文本增量并同步持久化及流事件。
     *
     * @param answerId 回答 ID。
     *
     * @param content 内容。
     *
     * @param chunk 本次接收的文本分片。
     */
    private void appendChunk(final UUID answerId, final StringBuilder content, final String chunk) {
        final String value = Objects.requireNonNull(chunk, "model chunk must not be null");
        if (content.length() + value.length() > properties.maxAnswerCharacters()) {
            throw new IllegalArgumentException("generated answer exceeds configured maximum");
        }
        content.append(value);
        if (!answerRepository.savePartial(answerId, content.toString())) {
            throw new GenerationCancelledException();
        }
        eventPort.publish(answerId, "delta", value);
    }

    /**
     * 向客户端发布已检索知识片段的来源标识。
     *
     * @param answerId 回答 ID。
     *
     * @param knowledge 知识库片段列表。
     */
    private void publishCitations(final UUID answerId, final List<KnowledgeChunk> knowledge) {
        for (final KnowledgeChunk chunk : knowledge) {
            eventPort.publish(answerId, "citation", chunk.sourceId());
        }
    }

    /**
     * 保存确定性追问并进入待澄清终态；若停止请求先获胜，则交给停止流程收敛。
     *
     * @param answerId 回答 ID。
     *
     * @param clarification 追问信息。
     */
    private void completeClarification(
            final UUID answerId,
            final ClarificationRequest clarification) {
        final String prompt = clarification.prompt();
        if (prompt.length() > properties.maxAnswerCharacters()) {
            throw new IllegalArgumentException("clarification prompt exceeds configured maximum");
        }
        if (answerRepository.clarify(answerId, prompt, Instant.now(clock))) {
            eventPort.publish(answerId, "clarification_required", clarification.reason().name());
            eventPort.publish(answerId, "delta", prompt);
            eventPort.publish(answerId, "completed", "clarification_required");
        } else {
            cancellationDispatcher.dispatch(answerId);
        }
    }

    /**
     * 加载并按时间正序整理最近有效对话。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @return 加载并按时间正序整理最近有效对话。
     */
    private List<ChatMessage> loadHistory(final String ownerId, final UUID conversationId) {
        final List<ChatMessage> messages = conversationRepository.listMessages(
                ownerId, conversationId, properties.maxHistoryMessages());
        final List<ChatMessage> usable = new ArrayList<>();
        for (final ChatMessage message : Objects.requireNonNull(messages, "history must not be null")) {
            if (message != null && !message.content().trim().isEmpty()) {
                usable.add(message);
            }
        }
        return Collections.unmodifiableList(usable);
    }

    /**
     * 按用户及期望版本更新持久化对象。
     *
     * @param answerId 回答 ID。
     *
     * @param status 业务状态。
     *
     * @param event 回答事件。
     */
    private void update(final UUID answerId, final AnswerSnapshot.Status status, final String event) {
        if (!answerRepository.transitionStatus(answerId, status)) {
            throw new GenerationCancelledException();
        }
        eventPort.publish(answerId, event, status.name());
    }

    /**
     * 校验生成任务尚未收到停止请求。
     *
     * @param answerId 回答 ID。
     */
    private void ensureNotCancelled(final UUID answerId) {
        if (cancellationRepository.isCancellationRequested(answerId)) {
            throw new GenerationCancelledException();
        }
    }

    /**
     * 校验问题非空且未超过字符上限。
     *
     * @param question 用户问题。
     *
     * @return 校验问题非空且未超过字符上限。
     */
    private String validateQuestion(final String question) {
        final String value = ApplicationSupport.requireText(question, "question");
        if (value.length() > properties.maxQuestionCharacters()) {
            throw new IllegalArgumentException("question exceeds configured maximum");
        }
        return value;
    }

    /**
     * 处理错误码。
     *
     * @param exception 待转换的原始异常。
     *
     * @return 错误码。
     */
    private String errorCode(final RuntimeException exception) {
        if (exception instanceof DependencyUnavailableException) {
            return ((DependencyUnavailableException) exception).errorCode();
        }
        if (exception instanceof IllegalArgumentException) {
            return "ANSWER_LIMIT_EXCEEDED";
        }
        return "GENERATION_FAILED";
    }

    /**
     * 生成过程中的停止检测和模型消息 ID 协调契约。
     */
    private final class GenerationControl implements LanguageModelPort.GenerationControl {
        /**
         * 回答 ID。
         */
        private final UUID answerId;

        /**
         * 创建 {@code GenerationControl} 实例。
         *
         * @param answerId 回答 ID。
         */
        GenerationControl(final UUID answerId) {
            this.answerId = answerId;
        }

        /**
         * 判断回答是否已收到停止请求。
         *
         * @return 条件成立时返回 true，否则返回 false。
         */
        @Override
        public boolean isCancellationRequested() {
            return cancellationRepository.isCancellationRequested(answerId);
        }

        /**
         * 持久化公司 HiAgent 应用会话 ID。
         *
         * @param appConversationId 公司 HiAgent 应用会话 ID。
         */
        @Override
        public void onAppConversationId(final String appConversationId) {
            if (!answerRepository.recordAppConversationId(answerId, appConversationId)) {
                throw new IllegalStateException("HiAgent app conversation id was not persisted");
            }
        }

        /**
         * 持久化公司模型消息 ID 并唤醒停止任务。
         *
         * @param messageId 公司模型侧消息 ID。
         */
        @Override
        public void onMessageId(final String messageId) {
            cancellationRepository.recordMessageId(answerId, messageId, Instant.now(clock));
            cancellationDispatcher.dispatch(answerId);
        }
    }
}
