package com.acme.intelligentqa.adapter.in.web;

import com.acme.intelligentqa.common.error.AuthenticationRequiredException;
import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.in.TemporaryFileUseCase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 提供会话临时文件上传、列表和删除接口。
 */
@RestController
@RequestMapping("/api/v1/chats/{chatId}/files")
public class TemporaryFileController {

    /** 临时文件用例。 */
    private final TemporaryFileUseCase fileUseCase;

    /**
     * 创建临时文件控制器。
     *
     * @param fileUseCase 临时文件用例。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected use case is retained and not exposed")
    public TemporaryFileController(final TemporaryFileUseCase fileUseCase) {
        this.fileUseCase = fileUseCase;
    }

    /**
     * 上传一个会话临时文件。
     *
     * @param principal 认证用户主体。
     * @param chatId 会话 ID。
     * @param idempotencyKey 上传幂等键。
     * @param file 浏览器上传文件。
     * @param usage 文件默认使用角色。
     * @return 已创建文件元数据。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TemporaryFileResponse> upload(
            final Principal principal,
            @PathVariable final UUID chatId,
            @RequestHeader("Idempotency-Key") final String idempotencyKey,
            @RequestParam("file") final MultipartFile file,
            @RequestParam(value = "usage", defaultValue = "AUTO") final TemporaryFile.Usage usage) {
        try {
            final TemporaryFile uploaded = fileUseCase.upload(
                    owner(principal), chatId, file.getOriginalFilename(), file.getContentType(),
                    file.getBytes(), usage, idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(TemporaryFileResponse.from(uploaded));
        } catch (final IOException exception) {
            throw new DependencyUnavailableException(
                    "UPLOAD_READ_FAILED", "uploaded file could not be read", exception);
        }
    }

    /**
     * 查询当前会话未删除的临时文件。
     *
     * @param principal 认证用户主体。
     * @param chatId 会话 ID。
     * @return 临时文件列表。
     */
    @GetMapping
    public List<TemporaryFileResponse> list(
            final Principal principal, @PathVariable final UUID chatId) {
        return fileUseCase.list(owner(principal), chatId).stream()
                .map(TemporaryFileResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 删除当前会话中的指定临时文件。
     *
     * @param principal 认证用户主体。
     * @param chatId 会话 ID。
     * @param fileId 文件 ID。
     * @return 无响应体的删除结果。
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(
            final Principal principal,
            @PathVariable final UUID chatId,
            @PathVariable final UUID fileId) {
        fileUseCase.delete(owner(principal), chatId, fileId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 读取可信认证主体标识。
     *
     * @param principal 认证用户主体。
     * @return 当前用户标识。
     */
    private String owner(final Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().trim().isEmpty()) {
            throw new AuthenticationRequiredException();
        }
        return principal.getName();
    }

    /**
     * 可安全返回前端的临时文件元数据。
     */
    public static final class TemporaryFileResponse {

        /** 文件唯一标识。 */
        private final UUID id;
        /** 所属会话 ID。 */
        private final UUID conversationId;
        /** 安全展示文件名。 */
        private final String name;
        /** 规范内容类型。 */
        private final String contentType;
        /** 文件字节数。 */
        private final long sizeBytes;
        /** 默认使用角色。 */
        private final String usage;
        /** 文件处理状态。 */
        private final String status;
        /** 创建时间。 */
        private final Instant createdAt;
        /** 更新时间。 */
        private final Instant updatedAt;

        /**
         * 创建临时文件接口响应。
         *
         * @param id 文件唯一标识。
         * @param conversationId 所属会话 ID。
         * @param name 安全展示文件名。
         * @param contentType 规范内容类型。
         * @param sizeBytes 文件字节数。
         * @param usage 默认使用角色。
         * @param status 文件处理状态。
         * @param createdAt 创建时间。
         * @param updatedAt 更新时间。
         */
        private TemporaryFileResponse(
                final UUID id,
                final UUID conversationId,
                final String name,
                final String contentType,
                final long sizeBytes,
                final String usage,
                final String status,
                final Instant createdAt,
                final Instant updatedAt) {
            this.id = id;
            this.conversationId = conversationId;
            this.name = name;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
            this.usage = usage;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        /**
         * 将领域文件转换为接口响应。
         *
         * @param file 临时文件。
         * @return 接口响应。
         */
        private static TemporaryFileResponse from(final TemporaryFile file) {
            return new TemporaryFileResponse(
                    file.id(), file.conversationId(), file.originalName(), file.contentType(),
                    file.sizeBytes(), file.usage().name(), file.status().name(),
                    file.createdAt(), file.updatedAt());
        }

        /** @return 文件唯一标识。 */
        public UUID getId() { return id; }
        /** @return 所属会话 ID。 */
        public UUID getConversationId() { return conversationId; }
        /** @return 安全展示文件名。 */
        public String getName() { return name; }
        /** @return 规范内容类型。 */
        public String getContentType() { return contentType; }
        /** @return 文件字节数。 */
        public long getSizeBytes() { return sizeBytes; }
        /** @return 默认使用角色。 */
        public String getUsage() { return usage; }
        /** @return 文件处理状态。 */
        public String getStatus() { return status; }
        /** @return 创建时间。 */
        public Instant getCreatedAt() { return createdAt; }
        /** @return 更新时间。 */
        public Instant getUpdatedAt() { return updatedAt; }
    }
}
