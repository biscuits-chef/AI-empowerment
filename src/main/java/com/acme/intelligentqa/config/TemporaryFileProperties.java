package com.acme.intelligentqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 临时文件大小、数量、本地开发存储和处理状态配置。
 */
@ConfigurationProperties("app.file")
@ConstructorBinding
public final class TemporaryFileProperties {

    /** 单文件最大字节数。 */
    private final long maximumBytes;
    /** 单会话活动文件数量上限。 */
    private final int maximumFilesPerConversation;
    /** 对象存储模式。 */
    private final String storageMode;
    /** 本地开发对象根目录。 */
    private final String localRoot;
    /** 是否允许仅完成基础校验的文件进入 READY。 */
    private final boolean allowUnprocessedReady;

    /**
     * 创建临时文件配置。
     *
     * @param maximumBytes 单文件最大字节数。
     * @param maximumFilesPerConversation 单会话活动文件数量上限。
     * @param storageMode 对象存储模式。
     * @param localRoot 本地开发对象根目录。
     * @param allowUnprocessedReady 是否允许基础校验后直接进入 READY。
     */
    public TemporaryFileProperties(
            final long maximumBytes,
            final int maximumFilesPerConversation,
            final String storageMode,
            final String localRoot,
            final boolean allowUnprocessedReady) {
        if (maximumBytes <= 0L) {
            throw new IllegalArgumentException("app.file.maximum-bytes must be positive");
        }
        if (maximumFilesPerConversation <= 0) {
            throw new IllegalArgumentException("app.file.maximum-files-per-conversation must be positive");
        }
        this.maximumBytes = maximumBytes;
        this.maximumFilesPerConversation = maximumFilesPerConversation;
        this.storageMode = requireText(storageMode, "storage-mode");
        this.localRoot = requireText(localRoot, "local-root");
        this.allowUnprocessedReady = allowUnprocessedReady;
    }

    /** @return 单文件最大字节数。 */
    public long maximumBytes() { return maximumBytes; }
    /** @return 单会话活动文件数量上限。 */
    public int maximumFilesPerConversation() { return maximumFilesPerConversation; }
    /** @return 对象存储模式。 */
    public String storageMode() { return storageMode; }
    /** @return 本地开发对象根目录。 */
    public String localRoot() { return localRoot; }
    /** @return 是否允许基础校验后直接进入 READY。 */
    public boolean allowUnprocessedReady() { return allowUnprocessedReady; }

    /**
     * 校验配置文本非空。
     *
     * @param value 配置值。
     * @param property 配置名称。
     * @return 去除首尾空白后的配置值。
     */
    private static String requireText(final String value, final String property) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("app.file." + property + " must not be blank");
        }
        return value.trim();
    }
}
