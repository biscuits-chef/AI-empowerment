package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.common.error.FileTooLargeException;
import com.acme.intelligentqa.common.error.IdempotencyConflictException;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.common.error.UnsupportedFileTypeException;
import com.acme.intelligentqa.config.TemporaryFileProperties;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.in.TemporaryFileUseCase;
import com.acme.intelligentqa.domain.port.out.ConversationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.ObjectStoragePort;
import com.acme.intelligentqa.domain.port.out.TemporaryFileRepositoryPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 编排会话临时文件的校验、对象写入、元数据状态和补偿删除。
 */
@Service
public class TemporaryFileService implements TemporaryFileUseCase {

    /** 支持扩展名到规范内容类型的映射。 */
    private static final Map<String, String> CONTENT_TYPES = supportedContentTypes();
    /** 会话仓储。 */
    private final ConversationRepositoryPort conversationRepository;
    /** 文件元数据仓储。 */
    private final TemporaryFileRepositoryPort fileRepository;
    /** 对象存储端口。 */
    private final ObjectStoragePort objectStorage;
    /** 文件配置。 */
    private final TemporaryFileProperties properties;
    /** 系统时钟。 */
    private final Clock clock;

    /**
     * 创建临时文件应用服务。
     *
     * @param conversationRepository 会话仓储。
     * @param fileRepository 文件元数据仓储。
     * @param objectStorage 对象存储端口。
     * @param properties 文件配置。
     * @param clock 系统时钟。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public TemporaryFileService(
            final ConversationRepositoryPort conversationRepository,
            final TemporaryFileRepositoryPort fileRepository,
            final ObjectStoragePort objectStorage,
            final TemporaryFileProperties properties,
            final Clock clock) {
        this.conversationRepository = conversationRepository;
        this.fileRepository = fileRepository;
        this.objectStorage = objectStorage;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 上传并保存一个会话临时文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param originalName 原始文件名。
     * @param contentType 浏览器声明的内容类型。
     * @param content 文件字节内容。
     * @param usage 文件默认使用角色。
     * @param idempotencyKey 上传幂等键。
     * @return 上传后的临时文件。
     */
    @Override
    public TemporaryFile upload(
            final String ownerId,
            final UUID conversationId,
            final String originalName,
            final String contentType,
            final byte[] content,
            final TemporaryFile.Usage usage,
            final String idempotencyKey) {
        final String validOwner = ApplicationSupport.requireText(ownerId, "ownerId");
        final UUID validConversationId = Objects.requireNonNull(
                conversationId, "conversationId must not be null");
        requireConversation(validOwner, validConversationId);
        final String validName = safeFileName(originalName);
        final byte[] boundedContent = validateContent(content);
        final String extension = extension(validName);
        final String normalizedType = validateType(extension, contentType, boundedContent);
        final TemporaryFile.Usage validUsage = Objects.requireNonNull(usage, "usage must not be null");
        final String validKey = ApplicationSupport.requireText(idempotencyKey, "idempotencyKey");
        final String sha256 = sha256(boundedContent);
        final TemporaryFile existing = fileRepository.findByIdempotencyKey(validOwner, validKey).orElse(null);
        if (existing != null) {
            return matchingIdempotent(existing, validConversationId, validName, validUsage, sha256);
        }
        if (fileRepository.list(validOwner, validConversationId,
                properties.maximumFilesPerConversation()).size()
                >= properties.maximumFilesPerConversation()) {
            throw new IllegalArgumentException("当前会话附件数量已达到上限");
        }
        final Instant now = Instant.now(clock);
        final UUID fileId = UUID.randomUUID();
        final String objectKey = "temporary/" + validConversationId + "/" + fileId;
        final TemporaryFile uploading = new TemporaryFile(
                fileId, validConversationId, validOwner, validName, normalizedType,
                boundedContent.length, sha256, objectKey, validUsage,
                TemporaryFile.Status.UPLOADING, now, now);
        fileRepository.create(uploading, validKey);
        try {
            objectStorage.store(objectKey, boundedContent, normalizedType);
            final TemporaryFile.Status storedStatus = properties.allowUnprocessedReady()
                    ? TemporaryFile.Status.READY : TemporaryFile.Status.STORED;
            return fileRepository.updateStatus(fileId, storedStatus, Instant.now(clock))
                    .orElseThrow(() -> new DependencyUnavailableException(
                            "FILE_STATE_UPDATE_FAILED", "uploaded file state could not be updated"));
        } catch (final DependencyUnavailableException | PersistenceOperationException exception) {
            fileRepository.updateStatus(fileId, TemporaryFile.Status.FAILED, Instant.now(clock));
            compensateObject(objectKey);
            throw exception;
        }
    }

    /**
     * 查询当前会话未删除的临时文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @return 当前会话临时文件列表。
     */
    @Override
    public List<TemporaryFile> list(final String ownerId, final UUID conversationId) {
        requireConversation(ownerId, conversationId);
        return fileRepository.list(ownerId, conversationId, properties.maximumFilesPerConversation());
    }

    /**
     * 删除当前会话内尚未用于新问题的临时文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param fileId 文件 ID。
     */
    @Override
    public void delete(final String ownerId, final UUID conversationId, final UUID fileId) {
        requireConversation(ownerId, conversationId);
        final UUID validFileId = Objects.requireNonNull(fileId, "fileId must not be null");
        final boolean referenced = fileRepository.hasQuestionReference(validFileId);
        final TemporaryFile file = fileRepository.markDeletePending(
                        ownerId, conversationId, validFileId,
                        Instant.now(clock))
                .orElseThrow(() -> new ResourceNotFoundException("temporary file not found"));
        if (!referenced) {
            objectStorage.delete(file.objectKey());
        }
    }

    /**
     * 校验会话归属，跨用户访问使用统一不存在语义。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     */
    private void requireConversation(final String ownerId, final UUID conversationId) {
        if (!conversationRepository.findActive(
                ApplicationSupport.requireText(ownerId, "ownerId"),
                Objects.requireNonNull(conversationId, "conversationId must not be null")).isPresent()) {
            throw new ResourceNotFoundException("conversation not found");
        }
    }

    /**
     * 校验上传内容存在且处于配置上限内。
     *
     * @param content 文件内容。
     * @return 防御复制后的文件内容。
     */
    private byte[] validateContent(final byte[] content) {
        Objects.requireNonNull(content, "content must not be null");
        if (content.length == 0) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (content.length > properties.maximumBytes()) {
            throw new FileTooLargeException(properties.maximumBytes());
        }
        return Arrays.copyOf(content, content.length);
    }

    /**
     * 校验扩展名、声明类型和文件头的一致性。
     *
     * @param extension 文件扩展名。
     * @param declaredType 浏览器声明的内容类型。
     * @param content 文件内容。
     * @return 服务端采用的规范内容类型。
     */
    private String validateType(
            final String extension,
            final String declaredType,
            final byte[] content) {
        final String normalizedType = CONTENT_TYPES.get(extension);
        if (normalizedType == null || !matchesHeader(extension, content)) {
            throw new UnsupportedFileTypeException();
        }
        if (!isCompatibleDeclaredType(extension, declaredType, normalizedType)) {
            throw new UnsupportedFileTypeException();
        }
        return normalizedType;
    }

    /**
     * 判断浏览器声明类型是否与服务端识别结果兼容。
     *
     * @param extension 文件扩展名。
     * @param declaredType 浏览器声明的内容类型。
     * @param normalizedType 服务端规范内容类型。
     * @return 声明类型可接受时返回 true。
     */
    private boolean isCompatibleDeclaredType(
            final String extension,
            final String declaredType,
            final String normalizedType) {
        final String browserType = declaredType == null
                ? "" : declaredType.trim().toLowerCase(Locale.ROOT);
        if (browserType.isEmpty() || "application/octet-stream".equals(browserType)) {
            return true;
        }
        if (normalizedType.equalsIgnoreCase(browserType)) {
            return true;
        }
        return "md".equals(extension) && "text/plain".equals(browserType);
    }

    /**
     * 判断文件头是否符合扩展名类别。
     *
     * @param extension 文件扩展名。
     * @param content 文件内容。
     * @return 匹配时返回 true。
     */
    private boolean matchesHeader(final String extension, final byte[] content) {
        if ("pdf".equals(extension)) {
            return startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII));
        }
        if ("png".equals(extension)) {
            return startsWith(content, new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        }
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return content.length >= 3 && content[0] == (byte) 0xFF
                    && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF;
        }
        if ("docx".equals(extension) || "xlsx".equals(extension)) {
            return startsWith(content, new byte[]{'P', 'K'});
        }
        return !containsNullByte(content);
    }

    /**
     * 判断内容是否包含二进制空字节。
     *
     * @param content 文件内容。
     * @return 包含空字节时返回 true。
     */
    private boolean containsNullByte(final byte[] content) {
        for (final byte value : content) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断内容是否以指定文件头开头。
     *
     * @param content 文件内容。
     * @param prefix 文件头。
     * @return 匹配时返回 true。
     */
    private boolean startsWith(final byte[] content, final byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index += 1) {
            if (content[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 对重复上传请求执行请求指纹校验。
     *
     * @param existing 已存在文件。
     * @param conversationId 会话 ID。
     * @param originalName 文件名。
     * @param usage 文件使用角色。
     * @param sha256 文件内容摘要。
     * @return 可复用的已存在文件。
     */
    private TemporaryFile matchingIdempotent(
            final TemporaryFile existing,
            final UUID conversationId,
            final String originalName,
            final TemporaryFile.Usage usage,
            final String sha256) {
        if (!existing.conversationId().equals(conversationId)
                || !existing.originalName().equals(originalName)
                || existing.usage() != usage
                || !existing.sha256().equals(sha256)) {
            throw new IdempotencyConflictException();
        }
        return existing;
    }

    /**
     * 删除部分失败流程可能留下的对象，补偿失败不覆盖原始异常。
     *
     * @param objectKey 服务端对象键。
     */
    private void compensateObject(final String objectKey) {
        try {
            objectStorage.delete(objectKey);
        } catch (final DependencyUnavailableException ignored) {
            // 元数据保留 FAILED 状态，后续孤儿对象对账任务负责再次清理。
        }
    }

    /**
     * 净化浏览器上传文件名，禁止路径和控制字符进入元数据。
     *
     * @param originalName 原始文件名。
     * @return 安全展示文件名。
     */
    private String safeFileName(final String originalName) {
        final String source = ApplicationSupport.requireText(originalName, "originalName")
                .replace('\\', '/');
        final String name = source.substring(source.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();
        if (name.isEmpty() || name.length() > 255) {
            throw new IllegalArgumentException("文件名无效或超过 255 个字符");
        }
        return name;
    }

    /**
     * 提取小写文件扩展名。
     *
     * @param fileName 文件名。
     * @return 小写扩展名。
     */
    private String extension(final String fileName) {
        final int separator = fileName.lastIndexOf('.');
        if (separator <= 0 || separator == fileName.length() - 1) {
            throw new UnsupportedFileTypeException();
        }
        return fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 计算文件内容摘要。
     *
     * @param content 文件内容。
     * @return 小写十六进制 SHA-256。
     */
    private String sha256(final byte[] content) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            final StringBuilder value = new StringBuilder(digest.length * 2);
            for (final byte item : digest) {
                value.append(String.format(Locale.ROOT, "%02x", item & 0xFF));
            }
            return value.toString();
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /**
     * 建立一期允许文件扩展名与规范内容类型映射。
     *
     * @return 不可修改的内容类型映射。
     */
    private static Map<String, String> supportedContentTypes() {
        final Map<String, String> types = new HashMap<>();
        types.put("pdf", "application/pdf");
        types.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        types.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        types.put("txt", "text/plain");
        types.put("md", "text/markdown");
        types.put("jpg", "image/jpeg");
        types.put("jpeg", "image/jpeg");
        types.put("png", "image/png");
        return Collections.unmodifiableMap(types);
    }
}
