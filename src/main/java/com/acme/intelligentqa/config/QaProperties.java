package com.acme.intelligentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 问答流程、上下文、事件重放和容量上限配置。
 */
@ConfigurationProperties("app.qa")
@ConstructorBinding
public final class QaProperties {

    /**
     * 演示模式开关。
     */
    private final boolean demoMode;
    /**
     * 问题最大字符数。
     */
    private final int maxQuestionCharacters;
    /**
     * 回答最大字符数。
     */
    private final int maxAnswerCharacters;
    /**
     * 上下文条目上限。
     */
    private final int maxContextItems;
    /**
     * 知识片段上下文最大字符数。
     */
    private final int maxKnowledgeCharacters;
    /**
     * 业务数据上下文最大字符数。
     */
    private final int maxBusinessDataCharacters;
    /**
     * 历史消息数量上限。
     */
    private final int maxHistoryMessages;
    /**
     * 意图最低置信度。
     */
    private final double minIntentConfidence;
    /**
     * 可重放事件缓存数量上限。
     */
    private final int eventBufferSize;
    /**
     * 生成线程池核心线程数。
     */
    private final int executorCoreSize;
    /**
     * 生成线程池最大线程数。
     */
    private final int executorMaxSize;
    /**
     * 生成任务等待队列容量。
     */
    private final int executorQueueCapacity;

    /**
     * 创建 {@code QaProperties} 实例。
     *
     * @param demoMode 演示模式开关。
     *
     * @param maxQuestionCharacters 问题最大字符数。
     *
     * @param maxAnswerCharacters 回答最大字符数。
     *
     * @param maxContextItems 上下文条目上限。
     *
     * @param maxKnowledgeCharacters 知识片段上下文最大字符数。
     *
     * @param maxBusinessDataCharacters 业务数据上下文最大字符数。
     *
     * @param maxHistoryMessages 历史消息数量上限。
     *
     * @param minIntentConfidence 意图最低置信度。
     *
     * @param eventBufferSize 可重放事件缓存数量上限。
     *
     * @param executorCoreSize 生成线程池核心线程数。
     *
     * @param executorMaxSize 生成线程池最大线程数。
     *
     * @param executorQueueCapacity 生成任务等待队列容量。
     */
    public QaProperties(
            final boolean demoMode,
            final int maxQuestionCharacters,
            final int maxAnswerCharacters,
            final int maxContextItems,
            final int maxKnowledgeCharacters,
            final int maxBusinessDataCharacters,
            final int maxHistoryMessages,
            final double minIntentConfidence,
            final int eventBufferSize,
            final int executorCoreSize,
            final int executorMaxSize,
            final int executorQueueCapacity) {
        requirePositive(maxQuestionCharacters, "max-question-characters");
        requirePositive(maxAnswerCharacters, "max-answer-characters");
        requirePositive(maxContextItems, "max-context-items");
        requirePositive(maxKnowledgeCharacters, "max-knowledge-characters");
        requirePositive(maxBusinessDataCharacters, "max-business-data-characters");
        requirePositive(maxHistoryMessages, "max-history-messages");
        if (minIntentConfidence <= 0.0D || minIntentConfidence > 1.0D) {
            throw new IllegalArgumentException("app.qa.min-intent-confidence must be between 0 and 1");
        }
        requirePositive(eventBufferSize, "event-buffer-size");
        requirePositive(executorCoreSize, "executor-core-size");
        requirePositive(executorMaxSize, "executor-max-size");
        requirePositive(executorQueueCapacity, "executor-queue-capacity");
        if (executorCoreSize > executorMaxSize) {
            throw new IllegalArgumentException("executor-core-size must not exceed executor-max-size");
        }
        this.demoMode = demoMode;
        this.maxQuestionCharacters = maxQuestionCharacters;
        this.maxAnswerCharacters = maxAnswerCharacters;
        this.maxContextItems = maxContextItems;
        this.maxKnowledgeCharacters = maxKnowledgeCharacters;
        this.maxBusinessDataCharacters = maxBusinessDataCharacters;
        this.maxHistoryMessages = maxHistoryMessages;
        this.minIntentConfidence = minIntentConfidence;
        this.eventBufferSize = eventBufferSize;
        this.executorCoreSize = executorCoreSize;
        this.executorMaxSize = executorMaxSize;
        this.executorQueueCapacity = executorQueueCapacity;
    }

    /**
     * 返回演示模式开关。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    public boolean demoMode() { return demoMode; }
    /**
     * 返回问题最大字符数。
     *
     * @return 问题最大字符数。
     */
    public int maxQuestionCharacters() { return maxQuestionCharacters; }
    /**
     * 返回回答最大字符数。
     *
     * @return 回答最大字符数。
     */
    public int maxAnswerCharacters() { return maxAnswerCharacters; }
    /**
     * 返回上下文条目上限。
     *
     * @return 上下文条目上限。
     */
    public int maxContextItems() { return maxContextItems; }
    /**
     * 处理知识片段上下文最大字符数。
     *
     * @return 知识片段上下文最大字符数。
     */
    public int maxKnowledgeCharacters() { return maxKnowledgeCharacters; }
    /**
     * 处理业务数据上下文最大字符数。
     *
     * @return 业务数据上下文最大字符数。
     */
    public int maxBusinessDataCharacters() { return maxBusinessDataCharacters; }
    /**
     * 返回历史消息数量上限。
     *
     * @return 历史消息数量上限。
     */
    public int maxHistoryMessages() { return maxHistoryMessages; }
    /**
     * 返回意图最低置信度。
     *
     * @return 意图最低置信度。
     */
    public double minIntentConfidence() { return minIntentConfidence; }
    /**
     * 处理可重放事件缓存数量上限。
     *
     * @return 可重放事件缓存数量上限。
     */
    public int eventBufferSize() { return eventBufferSize; }
    /**
     * 处理生成线程池核心线程数。
     *
     * @return 生成线程池核心线程数。
     */
    public int executorCoreSize() { return executorCoreSize; }
    /**
     * 处理生成线程池最大线程数。
     *
     * @return 生成线程池最大线程数。
     */
    public int executorMaxSize() { return executorMaxSize; }
    /**
     * 处理生成任务等待队列容量。
     *
     * @return 生成任务等待队列容量。
     */
    public int executorQueueCapacity() { return executorQueueCapacity; }

    /**
     * 校验配置数值必须为正数。
     *
     * @param value 输入值。
     *
     * @param property 配置属性名称。
     */
    private static void requirePositive(final int value, final String property) {
        if (value <= 0) {
            throw new IllegalArgumentException("app.qa." + property + " must be positive");
        }
    }
}
