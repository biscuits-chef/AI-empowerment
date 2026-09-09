package com.acme.intelligentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 停止任务租约、重试和恢复扫描配置。
 */
@ConfigurationProperties("app.qa.cancellation")
@ConstructorBinding
public final class CancellationProperties {

    /**
     * 停止任务恢复扫描间隔（毫秒）。
     */
    private final long scanDelayMillis;
    /**
     * 停止任务租约时长（毫秒）。
     */
    private final long leaseMillis;
    /**
     * 失败重试间隔（毫秒）。
     */
    private final long retryDelayMillis;
    /**
     * 等待公司模型消息 ID 的时长（毫秒）。
     */
    private final long messageIdWaitMillis;
    /**
     * 停止接口最大尝试次数。
     */
    private final int maximumAttempts;
    /**
     * 每批扫描任务数。
     */
    private final int batchSize;

    /**
     * 创建 {@code CancellationProperties} 实例。
     *
     * @param scanDelayMillis 停止任务恢复扫描间隔（毫秒）。
     *
     * @param leaseMillis 停止任务租约时长（毫秒）。
     *
     * @param retryDelayMillis 失败重试间隔（毫秒）。
     *
     * @param messageIdWaitMillis 等待公司模型消息 ID 的时长（毫秒）。
     *
     * @param maximumAttempts 停止接口最大尝试次数。
     *
     * @param batchSize 每批扫描任务数。
     */
    public CancellationProperties(
            final long scanDelayMillis,
            final long leaseMillis,
            final long retryDelayMillis,
            final long messageIdWaitMillis,
            final int maximumAttempts,
            final int batchSize) {
        requirePositive(scanDelayMillis, "scan-delay-millis");
        requirePositive(leaseMillis, "lease-millis");
        requirePositive(retryDelayMillis, "retry-delay-millis");
        requirePositive(messageIdWaitMillis, "message-id-wait-millis");
        requirePositive(maximumAttempts, "maximum-attempts");
        requirePositive(batchSize, "batch-size");
        this.scanDelayMillis = scanDelayMillis;
        this.leaseMillis = leaseMillis;
        this.retryDelayMillis = retryDelayMillis;
        this.messageIdWaitMillis = messageIdWaitMillis;
        this.maximumAttempts = maximumAttempts;
        this.batchSize = batchSize;
    }

    /**
     * 处理停止任务恢复扫描间隔（毫秒）。
     *
     * @return 停止任务恢复扫描间隔（毫秒）。
     */
    public long scanDelayMillis() { return scanDelayMillis; }
    /**
     * 处理停止任务租约时长（毫秒）。
     *
     * @return 停止任务租约时长（毫秒）。
     */
    public long leaseMillis() { return leaseMillis; }
    /**
     * 处理失败重试间隔（毫秒）。
     *
     * @return 失败重试间隔（毫秒）。
     */
    public long retryDelayMillis() { return retryDelayMillis; }
    /**
     * 处理等待公司模型消息 ID 的时长（毫秒）。
     *
     * @return 等待公司模型消息 ID 的时长（毫秒）。
     */
    public long messageIdWaitMillis() { return messageIdWaitMillis; }
    /**
     * 处理停止接口最大尝试次数。
     *
     * @return 停止接口最大尝试次数。
     */
    public int maximumAttempts() { return maximumAttempts; }
    /**
     * 处理每批扫描任务数。
     *
     * @return 每批扫描任务数。
     */
    public int batchSize() { return batchSize; }

    /**
     * 校验配置数值必须为正数。
     *
     * @param value 输入值。
     *
     * @param field 字段名称。
     */
    private static void requirePositive(final long value, final String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
