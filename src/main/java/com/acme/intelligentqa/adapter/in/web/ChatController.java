package com.acme.intelligentqa.adapter.in.web;

import com.acme.intelligentqa.common.error.AuthenticationRequiredException;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.Conversation;
import com.acme.intelligentqa.domain.model.ConversationPage;
import com.acme.intelligentqa.domain.model.MessageAttachment;
import com.acme.intelligentqa.domain.port.in.ChatUseCase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供会话创建、查询、改名、删除及历史消息查询接口。
 */
@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    /**
     * 会话管理用例。
     */
    private final ChatUseCase chatUseCase;

    /**
     * 创建 {@code ChatController} 实例。
     *
     * @param chatUseCase 会话管理用例。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected use case is retained and not exposed")
    public ChatController(final ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    /**
     * 创建并持久化业务对象。
     *
     * @param principal 认证用户主体。
     *
     * @param request 接口请求。
     *
     * @return 创建并持久化业务对象。
     */
    @PostMapping
    public ResponseEntity<ConversationResponse> create(
            final Principal principal,
            @Valid @RequestBody final ConversationRequest request) {
        final Conversation conversation = chatUseCase.create(owner(principal), request.getTitle());
        return ResponseEntity.created(URI.create("/api/v1/chats/" + conversation.id()))
                .body(ConversationResponse.from(conversation));
    }

    /**
     * 查询当前用户的会话列表。
     *
     * @param principal 认证用户主体。
     * @param cursor 上一页返回的稳定游标，首页为空。
     * @param limit 数量上限。
     * @return 查询当前用户的会话列表。
     */
    @GetMapping
    public ConversationPageResponse list(
            final Principal principal,
            @RequestParam(required = false) final String cursor,
            @RequestParam(defaultValue = "50") final int limit) {
        return ConversationPageResponse.from(chatUseCase.list(owner(principal), cursor, limit));
    }

    /**
     * 会话游标分页响应。
     */
    public static final class ConversationPageResponse {
        /** 当前页会话列表。 */
        private final List<ConversationResponse> items;
        /** 下一页游标。 */
        private final String nextCursor;
        /** 是否还有下一页。 */
        private final boolean hasMore;

        /**
         * 创建会话游标分页响应。
         *
         * @param items 当前页会话列表。
         * @param nextCursor 下一页游标。
         * @param hasMore 是否还有下一页。
         */
        private ConversationPageResponse(
                final List<ConversationResponse> items,
                final String nextCursor,
                final boolean hasMore) {
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
            this.nextCursor = nextCursor;
            this.hasMore = hasMore;
        }

        /**
         * 将领域分页对象转换为接口响应。
         *
         * @param page 会话领域分页对象。
         * @return 会话游标分页响应。
         */
        static ConversationPageResponse from(final ConversationPage page) {
            return new ConversationPageResponse(
                    page.items().stream().map(ConversationResponse::from).collect(Collectors.toList()),
                    page.nextCursor(), page.hasMore());
        }

        /** @return 当前页会话列表。 */
        public List<ConversationResponse> getItems() { return items; }

        /** @return 下一页游标。 */
        public String getNextCursor() { return nextCursor; }

        /** @return 有下一页时返回 true。 */
        public boolean isHasMore() { return hasMore; }
    }

    /**
     * 读取当前用户的目标业务对象。
     *
     * @param principal 认证用户主体。
     *
     * @param chatId 会话 ID。
     *
     * @return 读取当前用户的目标业务对象。
     */
    @GetMapping("/{chatId}")
    public ConversationResponse get(final Principal principal, @PathVariable final UUID chatId) {
        return ConversationResponse.from(chatUseCase.get(owner(principal), chatId));
    }

    /**
     * 处理会话消息列表。
     *
     * @param principal 认证用户主体。
     *
     * @param chatId 会话 ID。
     *
     * @param limit 数量上限。
     *
     * @return 会话消息列表。
     */
    @GetMapping("/{chatId}/messages")
    public List<MessageResponse> messages(
            final Principal principal,
            @PathVariable final UUID chatId,
            @RequestParam(defaultValue = "100") final int limit) {
        return chatUseCase.messages(owner(principal), chatId, limit).stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 修改当前用户的会话名称。
     *
     * @param principal 认证用户主体。
     *
     * @param chatId 会话 ID。
     *
     * @param request 接口请求。
     *
     * @return 修改当前用户的会话名称。
     */
    @PatchMapping("/{chatId}")
    public ConversationResponse rename(
            final Principal principal,
            @PathVariable final UUID chatId,
            @Valid @RequestBody final ConversationRequest request) {
        return ConversationResponse.from(chatUseCase.rename(owner(principal), chatId, request.getTitle()));
    }

    /**
     * 逻辑删除当前用户的会话。
     *
     * @param principal 认证用户主体。
     *
     * @param chatId 会话 ID。
     *
     * @return 逻辑删除当前用户的会话。
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> delete(final Principal principal, @PathVariable final UUID chatId) {
        chatUseCase.delete(owner(principal), chatId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 处理当前认证用户标识。
     *
     * @param principal 认证用户主体。
     *
     * @return 当前认证用户标识。
     */
    private String owner(final Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().trim().isEmpty()) {
            throw new AuthenticationRequiredException();
        }
        return principal.getName();
    }

    /**
     * 会话创建或改名请求。
     */
    public static final class ConversationRequest {
        /**
         * 会话名称。
         */
        @NotBlank
        @Size(max = 100)
        private String title;

        /**
         * 返回会话名称。
         *
         * @return 会话名称。
         */
        public String getTitle() { return title; }
        /**
         * 设置会话名称。
         *
         * @param value 输入值。
         */
        public void setTitle(final String value) { this.title = value; }
    }

    /**
     * 会话摘要响应。
     */
    public static final class ConversationResponse {
        /**
         * 唯一标识。
         */
        private final UUID id;
        /**
         * 会话名称。
         */
        private final String title;
        /**
         * 创建时间。
         */
        private final Instant createdAt;
        /**
         * 更新时间。
         */
        private final Instant updatedAt;

        /**
         * 创建 {@code ConversationResponse} 实例。
         *
         * @param id 唯一标识。
         *
         * @param title 会话名称。
         *
         * @param createdAt 创建时间。
         *
         * @param updatedAt 更新时间。
         */
        private ConversationResponse(
                final UUID id,
                final String title,
                final Instant createdAt,
                final Instant updatedAt) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        /**
         * 将源对象转换为接口响应对象。
         *
         * @param conversation 会话领域对象。
         *
         * @return 将源对象转换为接口响应对象。
         */
        static ConversationResponse from(final Conversation conversation) {
            return new ConversationResponse(
                    conversation.id(), conversation.title(), conversation.createdAt(), conversation.updatedAt());
        }

        /**
         * 返回唯一标识。
         *
         * @return 唯一标识。
         */
        public UUID getId() { return id; }
        /**
         * 返回会话名称。
         *
         * @return 会话名称。
         */
        public String getTitle() { return title; }
        /**
         * 返回创建时间。
         *
         * @return 创建时间。
         */
        public Instant getCreatedAt() { return createdAt; }
        /**
         * 返回更新时间。
         *
         * @return 更新时间。
         */
        public Instant getUpdatedAt() { return updatedAt; }
    }

    /**
     * 携带回答状态的历史消息响应。
     */
    public static final class MessageResponse {
        /**
         * 唯一标识。
         */
        private final UUID id;
        /**
         * 回答 ID。
         */
        private final UUID answerId;
        /**
         * 回答状态。
         */
        private final String answerStatus;
        /**
         * 消息角色。
         */
        private final String role;
        /**
         * 内容。
         */
        private final String content;
        /**
         * 创建时间。
         */
        private final Instant createdAt;
        /** 随本次用户问题提交的附件元数据。 */
        private final List<MessageAttachmentResponse> attachments;
        /** 可恢复的回答执行事件。 */
        private final List<ExecutionEventResponse> executionEvents;
        /** 可恢复的回答产物引用。 */
        private final List<ArtifactResponse> artifacts;

        /**
         * 创建 {@code MessageResponse} 实例。
         *
         * @param id 唯一标识。
         *
         * @param answerId 回答 ID。
         *
         * @param answerStatus 回答状态。
         *
         * @param role 消息角色。
         *
         * @param content 内容。
         *
         * @param createdAt 创建时间。
         *
         * @param attachments 随本次用户问题提交的附件元数据。
         *
         * @param executionEvents 可恢复的回答执行事件。
         *
         * @param artifacts 可恢复的回答产物引用。
         */
        private MessageResponse(
                final UUID id,
                final UUID answerId,
                final String answerStatus,
                final String role,
                final String content,
                final Instant createdAt,
                final List<MessageAttachmentResponse> attachments,
                final List<ExecutionEventResponse> executionEvents,
                final List<ArtifactResponse> artifacts) {
            this.id = id;
            this.answerId = answerId;
            this.answerStatus = answerStatus;
            this.role = role;
            this.content = content;
            this.createdAt = createdAt;
            this.attachments = Collections.unmodifiableList(new ArrayList<>(attachments));
            this.executionEvents = Collections.unmodifiableList(new ArrayList<>(executionEvents));
            this.artifacts = Collections.unmodifiableList(new ArrayList<>(artifacts));
        }

        /**
         * 将源对象转换为接口响应对象。
         *
         * @param message 提示信息。
         *
         * @return 将源对象转换为接口响应对象。
         */
        static MessageResponse from(final ChatMessage message) {
            final List<ExecutionEventResponse> execution = message.executionEvents().stream()
                    .map(ExecutionEventResponse::from)
                    .collect(Collectors.toList());
            final List<ArtifactResponse> restoredArtifacts = message.executionEvents().stream()
                    .filter(event -> "citation".equals(event.type()))
                    .map(ArtifactResponse::from)
                    .collect(Collectors.toList());
            return new MessageResponse(
                    message.id(), message.answerId(),
                    message.answerStatus() == null ? null : message.answerStatus().name(),
                    message.role().name(), message.content(), message.createdAt(),
                    message.attachments().stream()
                            .map(MessageAttachmentResponse::from)
                            .collect(Collectors.toList()),
                    execution,
                    restoredArtifacts);
        }

        /**
         * 返回唯一标识。
         *
         * @return 唯一标识。
         */
        public UUID getId() { return id; }
        /**
         * 返回回答 ID。
         *
         * @return 回答 ID。
         */
        public UUID getAnswerId() { return answerId; }
        /**
         * 返回回答持久化状态。
         *
         * @return 回答持久化状态。
         */
        public String getAnswerStatus() { return answerStatus; }
        /**
         * 返回消息角色。
         *
         * @return 消息角色。
         */
        public String getRole() { return role; }
        /**
         * 返回回答或消息内容。
         *
         * @return 回答或消息内容。
         */
        public String getContent() { return content; }
        /**
         * 返回创建时间。
         *
         * @return 创建时间。
         */
        public Instant getCreatedAt() { return createdAt; }
        /**
         * 返回随本次用户问题提交的附件元数据。
         *
         * @return 附件元数据列表。
         */
        public List<MessageAttachmentResponse> getAttachments() { return attachments; }

        /** @return 可恢复的回答执行事件。 */
        public List<ExecutionEventResponse> getExecutionEvents() { return executionEvents; }

        /** @return 可恢复的回答产物引用。 */
        public List<ArtifactResponse> getArtifacts() { return artifacts; }
    }

    /**
     * 历史回答的可展示执行事件。
     */
    public static final class ExecutionEventResponse {
        /** 事件稳定序号。 */
        private final long sequence;
        /** 事件类型。 */
        private final String type;
        /** 事件值。 */
        private final String value;
        /** 事件发生时间。 */
        private final Instant occurredAt;

        /**
         * 创建历史执行事件响应。
         *
         * @param sequence 事件稳定序号。
         * @param type 事件类型。
         * @param value 事件值。
         * @param occurredAt 事件发生时间。
         */
        private ExecutionEventResponse(
                final long sequence,
                final String type,
                final String value,
                final Instant occurredAt) {
            this.sequence = sequence;
            this.type = type;
            this.value = value;
            this.occurredAt = occurredAt;
        }

        /**
         * 将领域事件转换为历史接口响应。
         *
         * @param event 回答执行事件。
         * @return 历史执行事件响应。
         */
        static ExecutionEventResponse from(final com.acme.intelligentqa.domain.model.AnswerEvent event) {
            return new ExecutionEventResponse(
                    event.sequence(), event.type(), event.data(), event.occurredAt());
        }

        /** @return 事件稳定序号。 */
        public long getSequence() { return sequence; }

        /** @return 事件类型。 */
        public String getType() { return type; }

        /** @return 事件值。 */
        public String getValue() { return value; }

        /** @return 事件发生时间。 */
        public Instant getOccurredAt() { return occurredAt; }
    }

    /**
     * 历史回答中可恢复的产物引用。
     */
    public static final class ArtifactResponse {
        /** 产物类型。 */
        private final String type;
        /** 产物稳定标识。 */
        private final String reference;

        /**
         * 创建回答产物响应。
         *
         * @param type 产物类型。
         * @param reference 产物稳定标识。
         */
        private ArtifactResponse(final String type, final String reference) {
            this.type = type;
            this.reference = reference;
        }

        /**
         * 把引用事件转换为产物响应。
         *
         * @param event 引用事件。
         * @return 回答产物响应。
         */
        static ArtifactResponse from(final com.acme.intelligentqa.domain.model.AnswerEvent event) {
            return new ArtifactResponse("CITATION", event.data());
        }

        /** @return 产物类型。 */
        public String getType() { return type; }

        /** @return 产物稳定标识。 */
        public String getReference() { return reference; }
    }

    /**
     * 历史用户消息中可安全展示的附件元数据响应。
     */
    public static final class MessageAttachmentResponse {
        /** 文件唯一标识。 */
        private final UUID fileId;
        /** 安全展示文件名。 */
        private final String name;
        /** 服务端识别的内容类型。 */
        private final String contentType;
        /** 文件字节数。 */
        private final long sizeBytes;
        /** 文件在该次问题中的使用角色。 */
        private final String usage;
        /** 文件当前处理状态。 */
        private final String status;

        /**
         * 创建历史消息附件响应。
         *
         * @param fileId 文件唯一标识。
         * @param name 安全展示文件名。
         * @param contentType 服务端识别的内容类型。
         * @param sizeBytes 文件字节数。
         * @param usage 文件在该次问题中的使用角色。
         * @param status 文件当前处理状态。
         */
        private MessageAttachmentResponse(
                final UUID fileId,
                final String name,
                final String contentType,
                final long sizeBytes,
                final String usage,
                final String status) {
            this.fileId = fileId;
            this.name = name;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
            this.usage = usage;
            this.status = status;
        }

        /**
         * 将领域附件元数据转换为接口响应。
         *
         * @param attachment 用户消息附件元数据。
         * @return 接口响应对象。
         */
        private static MessageAttachmentResponse from(final MessageAttachment attachment) {
            return new MessageAttachmentResponse(
                    attachment.fileId(), attachment.name(), attachment.contentType(),
                    attachment.sizeBytes(), attachment.usage().name(), attachment.status().name());
        }

        /** @return 文件唯一标识。 */
        public UUID getFileId() { return fileId; }
        /** @return 安全展示文件名。 */
        public String getName() { return name; }
        /** @return 服务端识别的内容类型。 */
        public String getContentType() { return contentType; }
        /** @return 文件字节数。 */
        public long getSizeBytes() { return sizeBytes; }
        /** @return 文件在该次问题中的使用角色。 */
        public String getUsage() { return usage; }
        /** @return 文件当前处理状态。 */
        public String getStatus() { return status; }
    }
}
