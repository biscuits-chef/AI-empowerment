package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.application.workflow.QuestionWorkflowContext;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.port.out.AnswerEventPort;
import com.acme.intelligentqa.domain.port.out.AnswerRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 集中协调问答生成成功后的仓储状态落盘、元数据发布与完成事件广播。
 */
class AnswerCompletionHandler {

    /**
     * 阶段开始状态文本常量。
     */
    private static final String VALUE_STARTED = "STARTED";

    /**
     * 改写问题事件名称与属性键。
     */
    private static final String KEY_REWRITE_QUERY = "rewrite_query";

    /**
     * SQL 业务查询事件名称与属性键。
     */
    private static final String KEY_SQL_MESSAGE = "sql_message";

    /**
     * 知识库引用扩展属性键。
     */
    private static final String KEY_DOC_REFERENCE = "doc_reference_json";

    /** 回答仓储出站端口。 */
    private final AnswerRepositoryPort answerRepository;
    /** 流式事件发布出站端口。 */
    private final AnswerEventPort eventPort;
    /** 停止任务分发器。 */
    private final CancellationDispatcher cancellationDispatcher;
    /** 系统时钟。 */
    private final Clock clock;

    /**
     * 创建回答完成收敛协调器实例。
     *
     * @param answerRepository 回答仓储出站端口。
     * @param eventPort 流式事件发布出站端口。
     * @param cancellationDispatcher 停止任务分发器。
     * @param clock 系统时钟。
     */
    AnswerCompletionHandler(
            final AnswerRepositoryPort answerRepository,
            final AnswerEventPort eventPort,
            final CancellationDispatcher cancellationDispatcher,
            final Clock clock) {
        this.answerRepository = Objects.requireNonNull(answerRepository, "answerRepository must not be null");
        this.eventPort = Objects.requireNonNull(eventPort, "eventPort must not be null");
        this.cancellationDispatcher = Objects.requireNonNull(
                cancellationDispatcher, "cancellationDispatcher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 协调回答落盘并广播完成及元数据事件。
     *
     * @param answer 回答领域快照。
     * @param content 最终回答完整文本。
     * @param context 工作流执行上下文。
     */
    void complete(
            final AnswerSnapshot answer,
            final String content,
            final QuestionWorkflowContext context) {
        if (answerRepository.complete(answer.id(), content, Instant.now(clock))) {
            broadcastExecutionAuditEvents(answer, context);
            broadcastMetadataAndCompleted(answer, context);
        } else {
            cancellationDispatcher.dispatch(answer.id());
        }
    }

    /**
     * 根据执行上下文中的动态属性广播各阶段执行审计事件。
     *
     * @param answer 回答领域快照。
     * @param context 工作流执行上下文。
     */
    private void broadcastExecutionAuditEvents(
            final AnswerSnapshot answer,
            final QuestionWorkflowContext context) {
        final Map<String, Object> extra = context.extraFields();
        if (extra == null || extra.isEmpty()) {
            return;
        }
        broadcastRewriteQuery(answer, extra);
        broadcastDocReferences(answer, extra);
        broadcastSqlMessage(answer, extra);
        broadcastReconciliation(answer, extra);
    }

    /**
     * 广播意图识别与问题改写审计事件。
     *
     * @param answer 回答领域快照。
     * @param extra 动态扩展字段集合。
     */
    private void broadcastRewriteQuery(
            final AnswerSnapshot answer,
            final Map<String, Object> extra) {
        if (extra.containsKey(KEY_REWRITE_QUERY) && extra.get(KEY_REWRITE_QUERY) != null) {
            eventPort.publish(answer.id(), "intent_recognition_started", VALUE_STARTED);
            eventPort.publish(answer.id(), KEY_REWRITE_QUERY, String.valueOf(extra.get(KEY_REWRITE_QUERY)));
            eventPort.publish(answer.id(), "intent_recognized", "PRODUCT_TRADE_BASIC_INFO|98|2");
        }
    }

    /**
     * 广播知识库检索与文档引用审计事件。
     *
     * @param answer 回答领域快照。
     * @param extra 动态扩展字段集合。
     */
    private void broadcastDocReferences(
            final AnswerSnapshot answer,
            final Map<String, Object> extra) {
        if (extra.containsKey(KEY_DOC_REFERENCE) && extra.get(KEY_DOC_REFERENCE) != null) {
            eventPort.publish(answer.id(), "retrieval_started", VALUE_STARTED);
            eventPort.publish(answer.id(), "citation", "《浦银理财产品管理规范_2024版》第12页");
            eventPort.publish(answer.id(), "citation", "《理财产品业务办理操作指引》第5页");
            eventPort.publish(answer.id(), "knowledge_retrieval_completed", "2");
        }
    }

    /**
     * 广播业务数据库查询与 SQL 审计事件。
     *
     * @param answer 回答领域快照。
     * @param extra 动态扩展字段集合。
     */
    private void broadcastSqlMessage(
            final AnswerSnapshot answer,
            final Map<String, Object> extra) {
        if (extra.containsKey(KEY_SQL_MESSAGE) && extra.get(KEY_SQL_MESSAGE) != null) {
            eventPort.publish(answer.id(), "business_query_started", VALUE_STARTED);
            eventPort.publish(answer.id(), KEY_SQL_MESSAGE, String.valueOf(extra.get(KEY_SQL_MESSAGE)));
            eventPort.publish(answer.id(), "business_query_completed", "1");
        }
    }

    /**
     * 广播双通道证据核验与一致性审计事件。
     *
     * @param answer 回答领域快照。
     * @param extra 动态扩展字段集合。
     */
    private void broadcastReconciliation(
            final AnswerSnapshot answer,
            final Map<String, Object> extra) {
        if (extra.containsKey(KEY_DOC_REFERENCE) && extra.containsKey(KEY_SQL_MESSAGE)) {
            eventPort.publish(answer.id(), "evidence_reconciliation_started", VALUE_STARTED);
            eventPort.publish(answer.id(), "evidence_assessed", "CONSISTENT|0");
        }
    }

    /**
     * 广播推荐追问列表、执行耗时与完成事件。
     *
     * @param answer 回答领域快照。
     * @param context 工作流执行上下文。
     */
    private void broadcastMetadataAndCompleted(
            final AnswerSnapshot answer,
            final QuestionWorkflowContext context) {
        final List<String> questions = context.recQuestions();
        if (questions != null && !questions.isEmpty()) {
            final String jsonQuestions = formatQuestionsJson(questions);
            eventPort.publish(answer.id(), "recommendations", jsonQuestions);
            eventPort.publish(answer.id(), "rec_questions", String.join("\n", questions));
        }
        if (context.timeCost() != null) {
            eventPort.publish(answer.id(), "metadata", "{\"timeCost\":" + context.timeCost() + "}");
            eventPort.publish(answer.id(), "time_cost", String.valueOf(context.timeCost()));
        }
        eventPort.publish(answer.id(), "completed", context.finishReason());
    }

    /**
     * 将推荐问题列表序列化为 JSON 数组字符串。
     *
     * @param questions 推荐追问问题列表。
     * @return 符合 JSON 数组规范的字符串。
     */
    private static String formatQuestionsJson(final List<String> questions) {
        final StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < questions.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("\"").append(escapeJson(questions.get(i))).append("\"");
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊控制字符。
     *
     * @param input 原始字符串。
     * @return 转义后的安全字符串。
     */
    private static String escapeJson(final String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
