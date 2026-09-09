package com.acme.intelligentqa.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.intelligentqa.common.error.FileTooLargeException;
import com.acme.intelligentqa.common.error.UnsupportedFileTypeException;
import com.acme.intelligentqa.config.TemporaryFileProperties;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.out.ConversationRepositoryPort;
import com.acme.intelligentqa.domain.port.out.ObjectStoragePort;
import com.acme.intelligentqa.domain.port.out.TemporaryFileRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 验证 TemporaryFileService 的校验、幂等、存储和状态行为。
 */
class TemporaryFileServiceTest {

    /** 固定测试时间。 */
    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");
    /** 测试用户 ID。 */
    private static final String OWNER = "user-1";
    /** 测试会话 ID。 */
    private UUID conversationId;
    /** 会话仓储。 */
    private ConversationRepositoryPort conversations;
    /** 文件元数据仓储。 */
    private TemporaryFileRepositoryPort files;
    /** 对象存储端口。 */
    private ObjectStoragePort storage;
    /** 被测应用服务。 */
    private TemporaryFileService service;

    /**
     * 初始化每个测试使用的隔离依赖。
     */
    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        conversations = mock(ConversationRepositoryPort.class);
        files = mock(TemporaryFileRepositoryPort.class);
        storage = mock(ObjectStoragePort.class);
        when(conversations.findActive(OWNER, conversationId)).thenReturn(Optional.of(
                new Conversation(conversationId, OWNER, "附件测试", NOW, NOW)));
        when(files.list(OWNER, conversationId, 5)).thenReturn(Collections.emptyList());
        final AtomicReference<TemporaryFile> created = new AtomicReference<>();
        when(files.create(any(TemporaryFile.class), anyString())).thenAnswer(invocation -> {
            final TemporaryFile file = invocation.getArgument(0);
            created.set(file);
            return file;
        });
        when(files.updateStatus(any(UUID.class), any(TemporaryFile.Status.class), any(Instant.class)))
                .thenAnswer(invocation -> {
                    final TemporaryFile file = created.get();
                    return Optional.of(new TemporaryFile(
                            file.id(), file.conversationId(), file.ownerId(), file.originalName(),
                            file.contentType(), file.sizeBytes(), file.sha256(), file.objectKey(),
                            file.usage(), invocation.getArgument(1), file.createdAt(), invocation.getArgument(2)));
                });
        service = new TemporaryFileService(
                conversations,
                files,
                storage,
                new TemporaryFileProperties(1024L, 5, "local", "/tmp/test-files", true),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * 验证文本附件通过服务端校验后写入对象存储并进入就绪状态。
     */
    @Test
    void storesValidatedFileAndMarksItReady() {
        final byte[] content = "product_code\nP001".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        final TemporaryFile uploaded = service.upload(
                OWNER, conversationId, "产品编号.txt", "text/plain", content,
                TemporaryFile.Usage.QUERY_INPUT, "upload-1");

        assertEquals(TemporaryFile.Status.READY, uploaded.status());
        assertEquals("产品编号.txt", uploaded.originalName());
        assertEquals(TemporaryFile.Usage.QUERY_INPUT, uploaded.usage());
        verify(storage).store(anyString(), any(byte[].class), eq("text/plain"));
    }

    /**
     * 验证内容超限、伪造文件头和错误用户均被拒绝。
     */
    @Test
    void rejectsOversizedUnsupportedAndCrossOwnerUploads() {
        assertThrows(FileTooLargeException.class, () -> service.upload(
                OWNER, conversationId, "大文件.txt", "text/plain", new byte[1025],
                TemporaryFile.Usage.AUTO, "too-large"));
        assertThrows(UnsupportedFileTypeException.class, () -> service.upload(
                OWNER, conversationId, "伪造.pdf", "application/pdf", "不是PDF".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8), TemporaryFile.Usage.EVIDENCE, "fake-pdf"));
        assertThrows(com.acme.intelligentqa.common.error.ResourceNotFoundException.class, () -> service.upload(
                "another-user", conversationId, "产品.txt", "text/plain", new byte[]{'a'},
                TemporaryFile.Usage.AUTO, "wrong-owner"));
    }

    /**
     * 验证相同幂等上传复用结果且不会重复写入对象存储。
     */
    @Test
    void reusesMatchingIdempotentUpload() {
        final byte[] content = "P001".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final TemporaryFile existing = new TemporaryFile(
                UUID.randomUUID(), conversationId, OWNER, "产品.txt", "text/plain", content.length,
                "df1e40051eff4bcf9b4ebc93083bfcad7f5195746b3e657de6b72bf3cb8897c3",
                "temporary/existing", TemporaryFile.Usage.AUTO, TemporaryFile.Status.READY, NOW, NOW);
        when(files.findByIdempotencyKey(OWNER, "same-key")).thenReturn(Optional.of(existing));

        final TemporaryFile result = service.upload(
                OWNER, conversationId, "产品.txt", "text/plain", content,
                TemporaryFile.Usage.AUTO, "same-key");

        assertEquals(existing.id(), result.id());
    }

    /**
     * 验证未被问题引用的附件删除时同步清理对象内容。
     */
    @Test
    void physicallyDeletesUnreferencedFile() {
        final TemporaryFile existing = storedFile();
        when(files.hasQuestionReference(existing.id())).thenReturn(false);
        when(files.markDeletePending(OWNER, conversationId, existing.id(), NOW))
                .thenReturn(Optional.of(existing));

        service.delete(OWNER, conversationId, existing.id());

        verify(storage).delete(existing.objectKey());
    }

    /**
     * 验证已随问题提交的附件只进入延迟清理状态，避免回答处理期间丢失内容。
     */
    @Test
    void retainsReferencedObjectForDelayedCleanup() {
        final TemporaryFile existing = storedFile();
        when(files.hasQuestionReference(existing.id())).thenReturn(true);
        when(files.markDeletePending(OWNER, conversationId, existing.id(), NOW))
                .thenReturn(Optional.of(existing));

        service.delete(OWNER, conversationId, existing.id());

        verify(storage, never()).delete(anyString());
    }

    /**
     * 验证一期允许的 Markdown、图片和表格文件头均可通过服务端识别。
     */
    @Test
    void acceptsSupportedDeclaredTypesAndFileHeaders() {
        assertEquals("text/markdown", service.upload(
                OWNER, conversationId, "说明.md", "text/plain", new byte[]{'h', 'i'},
                TemporaryFile.Usage.AUTO, "markdown").contentType());
        assertEquals("text/plain", service.upload(
                OWNER, conversationId, "说明.txt", null, new byte[]{'h', 'i'},
                TemporaryFile.Usage.AUTO, "no-declared-type").contentType());
        assertEquals("image/png", service.upload(
                OWNER, conversationId, "截图.png", "application/octet-stream",
                new byte[]{(byte) 0x89, 'P', 'N', 'G'}, TemporaryFile.Usage.EVIDENCE,
                "png").contentType());
        assertEquals("image/jpeg", service.upload(
                OWNER, conversationId, "照片.jpeg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, TemporaryFile.Usage.EVIDENCE,
                "jpeg").contentType());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", service.upload(
                OWNER, conversationId, "产品.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{'P', 'K'}, TemporaryFile.Usage.QUERY_INPUT, "xlsx").contentType());
    }

    /**
     * 验证空文件、二进制伪装文本、错误声明类型和缺少扩展名均被拒绝。
     */
    @Test
    void rejectsEmptySpoofedAndMismatchedFiles() {
        assertThrows(IllegalArgumentException.class, () -> service.upload(
                OWNER, conversationId, "空.txt", "text/plain", new byte[0],
                TemporaryFile.Usage.AUTO, "empty"));
        assertThrows(UnsupportedFileTypeException.class, () -> service.upload(
                OWNER, conversationId, "伪装.txt", "text/plain", new byte[]{'a', 0},
                TemporaryFile.Usage.AUTO, "binary-text"));
        assertThrows(UnsupportedFileTypeException.class, () -> service.upload(
                OWNER, conversationId, "类型.txt", "application/pdf", new byte[]{'a'},
                TemporaryFile.Usage.AUTO, "wrong-declared-type"));
        assertThrows(UnsupportedFileTypeException.class, () -> service.upload(
                OWNER, conversationId, "无扩展名", "text/plain", new byte[]{'a'},
                TemporaryFile.Usage.AUTO, "no-extension"));
    }

    /**
     * 验证对象存储失败后文件状态被标记失败并执行对象补偿删除。
     */
    @Test
    void compensatesObjectWhenStorageFails() {
        doThrow(new com.acme.intelligentqa.common.error.DependencyUnavailableException(
                "STORAGE_FAILED", "storage failed"))
                .when(storage).store(anyString(), any(byte[].class), eq("text/plain"));

        assertThrows(com.acme.intelligentqa.common.error.DependencyUnavailableException.class,
                () -> service.upload(
                        OWNER, conversationId, "失败.txt", "text/plain", new byte[]{'a'},
                        TemporaryFile.Usage.AUTO, "storage-failure"));

        verify(files).updateStatus(any(UUID.class), eq(TemporaryFile.Status.FAILED), eq(NOW));
        verify(storage).delete(anyString());
    }

    /**
     * 创建可复用的已存储测试文件。
     *
     * @return 已就绪的测试文件。
     */
    private TemporaryFile storedFile() {
        return new TemporaryFile(
                UUID.randomUUID(), conversationId, OWNER, "产品.txt", "text/plain", 4L,
                "df1e40051eff4bcf9b4ebc93083bfcad7f5195746b3e657de6b72bf3cb8897c3",
                "temporary/stored", TemporaryFile.Usage.AUTO, TemporaryFile.Status.READY, NOW, NOW);
    }
}
