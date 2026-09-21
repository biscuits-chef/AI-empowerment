package com.acme.intelligentqa.application.workflow;

import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.ClarificationRequest;
import com.acme.intelligentqa.domain.model.ConversationContext;
import com.acme.intelligentqa.domain.model.EvidenceAssessment;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.model.QueryScenario;
import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 单次问题执行期间在节点之间传递的类型化工作流上下文。
 *
 * <p>该对象只在一次执行任务内使用并保存完整节点轨迹；面向客户端的业务阶段仍由现有回答事件持久化。
 */
public final class QuestionWorkflowContext {
    /** 用户所有者 ID。 */
    private final String ownerId;
    /** 当前回答快照。 */
    private final AnswerSnapshot answer;
    /** 用户原始问题。 */
    private final String question;
    /** 最近有效对话历史。 */
    private final List<ChatMessage> history;
    /** 执行前已经持久化的会话结构化上下文。 */
    private final ConversationContext conversationContext;
    /** 本次后端查询场景。 */
    private final QueryScenario scenario;
    /** 回答文本增量消费者。 */
    private final Consumer<String> outputConsumer;
    /** 大模型生成取消控制器。 */
    private final LanguageModelPort.GenerationControl generationControl;
    /** 节点之间执行停止检查的回调。 */
    private final Runnable activeCheck;
    /** 公司 HiAgent 应用会话 ID，可为空。 */
    private final String appConversationId;
    /** 本次执行轨迹。 */
    private final List<WorkflowNodeExecution> executions = new ArrayList<>();
    /** 当前使用的执行计划版本。 */
    private String planVersion;
    /** 问题理解完成后的查询意图。 */
    private QueryIntent intent;
    /** 指代消解和实体补全后的问题。 */
    private String resolvedQuestion;
    /** 需要用户补充信息时生成的确定性追问。 */
    private ClarificationRequest clarification;
    /** 已确认实体对应的来源用户消息 ID。 */
    private Map<String, UUID> entitySourceMessageIds = Collections.emptyMap();
    /** 公司知识库检索结果。 */
    private List<KnowledgeChunk> knowledge = Collections.emptyList();
    /** GoldenDB 受控查询结果。 */
    private List<BusinessFact> businessFacts = Collections.emptyList();
    /** 多通道证据对账结果。 */
    private EvidenceAssessment evidenceAssessment;
    /** 大模型或确定性拒答的完成原因。 */
    private String finishReason;

    /**
     * 创建单次问答工作流上下文。
     *
     * @param ownerId 用户所有者 ID。
     * @param answer 当前回答快照。
     * @param question 用户原始问题。
     * @param history 最近有效对话历史。
     * @param conversationContext 已持久化会话上下文，首次问答时允许为空。
     * @param scenario 本次后端查询场景。
     * @param outputConsumer 回答文本增量消费者。
     * @param generationControl 大模型生成取消控制器。
     * @param activeCheck 节点之间的停止检查回调。
     * @param appConversationId 公司 HiAgent 应用会话 ID，可为空。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected callbacks are retained and not exposed")
    public QuestionWorkflowContext(
            final String ownerId,
            final AnswerSnapshot answer,
            final String question,
            final List<ChatMessage> history,
            final ConversationContext conversationContext,
            final QueryScenario scenario,
            final Consumer<String> outputConsumer,
            final LanguageModelPort.GenerationControl generationControl,
            final Runnable activeCheck,
            final String appConversationId) {
        this.ownerId = requireText(ownerId, "ownerId");
        this.answer = Objects.requireNonNull(answer, "answer must not be null");
        this.question = requireText(question, "question");
        this.history = immutableCopy(history);
        this.conversationContext = conversationContext;
        this.scenario = Objects.requireNonNull(scenario, "scenario must not be null");
        this.outputConsumer = Objects.requireNonNull(outputConsumer, "outputConsumer must not be null");
        this.generationControl = Objects.requireNonNull(
                generationControl, "generationControl must not be null");
        this.activeCheck = Objects.requireNonNull(activeCheck, "activeCheck must not be null");
        this.appConversationId = appConversationId == null || appConversationId.trim().isEmpty()
                ? null
                : appConversationId.trim();
    }

    /**
     * 创建单次问答工作流上下文。
     *
     * @param ownerId 用户所有者 ID。
     * @param answer 当前回答快照。
     * @param question 用户原始问题。
     * @param history 最近有效对话历史。
     * @param conversationContext 已持久化会话上下文，首次问答时允许为空。
     * @param scenario 本次后端查询场景。
     * @param outputConsumer 回答文本增量消费者。
     * @param generationControl 大模型生成取消控制器。
     * @param activeCheck 节点之间的停止检查回调。
     */
    public QuestionWorkflowContext(
            final String ownerId,
            final AnswerSnapshot answer,
            final String question,
            final List<ChatMessage> history,
            final ConversationContext conversationContext,
            final QueryScenario scenario,
            final Consumer<String> outputConsumer,
            final LanguageModelPort.GenerationControl generationControl,
            final Runnable activeCheck) {
        this(ownerId, answer, question, history, conversationContext, scenario, outputConsumer,
                generationControl, activeCheck, null);
    }

    /**
     * 返回公司 HiAgent 应用会话 ID。
     *
     * @return 公司 HiAgent 应用会话 ID，未指定时返回 null。
     */
    public String appConversationId() { return appConversationId; }

    /**
     * 绑定执行器选定的场景计划版本。
     *
     * @param value 执行计划版本。
     */
    public void bindPlanVersion(final String value) { planVersion = requireText(value, "planVersion"); }

    /**
     * 记录节点生命周期状态。
     *
     * @param nodeCode 节点稳定编码。
     * @param status 节点执行状态。
     * @param recordedAt 状态记录时间。
     * @param reason 不含业务正文的安全原因。
     */
    public void recordExecution(
            final WorkflowNodeCode nodeCode,
            final WorkflowNodeStatus status,
            final Instant recordedAt,
            final String reason) {
        executions.add(new WorkflowNodeExecution(nodeCode, status, recordedAt, reason));
    }

    /**
     * 执行停止检查，收到停止请求时由回调抛出取消异常。
     */
    public void ensureActive() { activeCheck.run(); }

    /**
     * 向回答生命周期协调器输出一个文本增量。
     *
     * @param chunk 本次输出文本分片。
     */
    public void emit(final String chunk) { outputConsumer.accept(chunk); }

    /**
     * 保存已经完成的结构化问题理解结果。
     *
     * @param value 已消解且参数完整的查询意图。
     * @param resolved 已补全实体的规范化问题。
     * @param sources 每个已确认实体对应的来源用户消息 ID。
     */
    public void resolve(
            final QueryIntent value,
            final String resolved,
            final Map<String, UUID> sources) {
        intent = Objects.requireNonNull(value, "intent must not be null");
        resolvedQuestion = requireText(resolved, "resolvedQuestion");
        entitySourceMessageIds = immutableMap(sources);
    }

    /**
     * 保存需要用户处理的确定性追问结果。
     *
     * @param value 追问请求。
     * @param sources 当前已经确认实体的来源用户消息 ID。
     */
    public void clarify(
            final ClarificationRequest value,
            final Map<String, UUID> sources) {
        clarification = Objects.requireNonNull(value, "clarification must not be null");
        intent = value.pendingIntent();
        entitySourceMessageIds = immutableMap(sources);
    }

    /**
     * 保存公司知识库检索结果。
     *
     * @param values 知识片段列表。
     */
    public void knowledge(final List<KnowledgeChunk> values) { knowledge = immutableCopy(values); }

    /**
     * 保存受控业务数据库查询结果。
     *
     * @param values 业务事实列表。
     */
    public void businessFacts(final List<BusinessFact> values) { businessFacts = immutableCopy(values); }

    /**
     * 保存多通道证据对账结果。
     *
     * @param value 证据对账结果。
     */
    public void evidenceAssessment(final EvidenceAssessment value) {
        evidenceAssessment = Objects.requireNonNull(value, "evidenceAssessment must not be null");
    }

    /**
     * 保存回答生成完成原因。
     *
     * @param value 大模型或确定性回答的完成原因。
     */
    public void finishReason(final String value) { finishReason = requireText(value, "finishReason"); }

    /**
     * 返回用户所有者 ID。
     *
     * @return 用户所有者 ID。
     */
    public String ownerId() { return ownerId; }

    /**
     * 返回当前回答快照。
     *
     * @return 当前回答快照。
     */
    public AnswerSnapshot answer() { return answer; }

    /**
     * 返回用户原始问题。
     *
     * @return 用户原始问题。
     */
    public String question() { return question; }

    /**
     * 返回最近有效对话历史。
     *
     * @return 不可变对话历史。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The list is an unmodifiable defensive copy")
    public List<ChatMessage> history() { return history; }

    /**
     * 返回执行前的会话结构化上下文。
     *
     * @return 会话上下文；首次问答时允许为空。
     */
    public ConversationContext conversationContext() { return conversationContext; }

    /**
     * 返回本次后端查询场景。
     *
     * @return 后端查询场景。
     */
    public QueryScenario scenario() { return scenario; }

    /**
     * 返回执行计划版本。
     *
     * @return 执行计划版本。
     */
    public String planVersion() { return planVersion; }

    /**
     * 返回当前查询意图。
     *
     * @return 已解析意图，或追问中保存的待完成意图。
     */
    public QueryIntent intent() { return intent; }

    /**
     * 返回规范化问题。
     *
     * @return 指代消解并补全实体后的问题。
     */
    public String resolvedQuestion() { return resolvedQuestion; }

    /**
     * 返回确定性追问请求。
     *
     * @return 需要追问时返回请求，否则为空。
     */
    public ClarificationRequest clarification() { return clarification; }

    /**
     * 返回已确认实体对应的来源用户消息 ID。
     *
     * @return 不可变的实体来源消息 ID 映射。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Map is an unmodifiable defensive copy")
    public Map<String, UUID> entitySourceMessageIds() { return entitySourceMessageIds; }

    /**
     * 返回公司知识库检索结果。
     *
     * @return 不可变知识片段列表。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The list is an unmodifiable defensive copy")
    public List<KnowledgeChunk> knowledge() { return knowledge; }

    /**
     * 返回 GoldenDB 受控查询结果。
     *
     * @return 不可变业务事实列表。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The list is an unmodifiable defensive copy")
    public List<BusinessFact> businessFacts() { return businessFacts; }

    /**
     * 返回多通道证据对账结果。
     *
     * @return 证据对账结果。
     */
    public EvidenceAssessment evidenceAssessment() { return evidenceAssessment; }

    /**
     * 返回回答生成完成原因。
     *
     * @return 完成原因。
     */
    public String finishReason() { return finishReason; }

    /**
     * 返回大模型生成取消控制器。
     *
     * @return 生成取消控制器。
     */
    public LanguageModelPort.GenerationControl generationControl() { return generationControl; }

    /**
     * 返回当前执行轨迹。
     *
     * @return 不可变执行记录快照。
     */
    public List<WorkflowNodeExecution> executions() {
        return Collections.unmodifiableList(new ArrayList<>(executions));
    }

    /**
     * 创建不允许外部修改的列表副本。
     *
     * @param values 输入列表。
     * @param <T> 列表元素类型。
     * @return 不可变列表副本。
     */
    private static <T> List<T> immutableCopy(final List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(values, "values must not be null")));
    }

    /**
     * 创建不允许外部修改的映射副本。
     *
     * @param values 输入映射。
     * @param <K> 映射键类型。
     * @param <V> 映射值类型。
     * @return 不可变映射副本。
     */
    private static <K, V> Map<K, V> immutableMap(final Map<K, V> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(values, "values must not be null")));
    }

    /**
     * 校验文本非空并返回去除首尾空格后的值。
     *
     * @param value 待校验文本。
     * @param field 字段名称。
     * @return 去除首尾空格后的文本。
     */
    private static String requireText(final String value, final String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
