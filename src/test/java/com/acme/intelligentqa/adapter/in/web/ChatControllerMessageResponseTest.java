package com.acme.intelligentqa.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.acme.intelligentqa.domain.model.AnswerEvent;
import com.acme.intelligentqa.domain.model.AnswerSnapshot;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.MessageAttachment;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 验证历史消息接口只序列化可安全展示的附件元数据。
 */
class ChatControllerMessageResponseTest {

    /**
     * 验证用户消息附件字段完整且不会泄露对象存储内部信息。
     *
     * @throws Exception 当 JSON 序列化失败时抛出。
     */
    @Test
    void serializesSafeAttachmentMetadataOnUserMessage() throws Exception {
        final MessageAttachment attachment = new MessageAttachment(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "产品编号.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                2048L,
                TemporaryFile.Usage.QUERY_INPUT,
                TemporaryFile.Status.DELETE_PENDING);
        final ChatMessage message = new ChatMessage(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                null,
                null,
                ChatMessage.Role.USER,
                "查询附件中的产品",
                Instant.parse("2026-09-02T00:00:00Z"),
                Collections.singletonList(attachment));

        final JsonNode payload = new ObjectMapper().findAndRegisterModules()
                .valueToTree(ChatController.MessageResponse.from(message));

        assertEquals("产品编号.xlsx", payload.path("attachments").get(0).path("name").asText());
        assertEquals("QUERY_INPUT", payload.path("attachments").get(0).path("usage").asText());
        assertEquals("DELETE_PENDING", payload.path("attachments").get(0).path("status").asText());
        assertFalse(payload.toString().contains("objectKey"));
        assertFalse(payload.toString().contains("url"));
    }

    /**
     * 验证助手历史消息可恢复执行过程和引用产物。
     */
    @Test
    void serializesExecutionEventsAndCitationArtifacts() {
        final UUID answerId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        final ChatMessage message = new ChatMessage(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                answerId,
                AnswerSnapshot.Status.COMPLETED,
                ChatMessage.Role.ASSISTANT,
                "已完成回答",
                Instant.parse("2026-09-02T00:00:00Z"),
                Collections.<MessageAttachment>emptyList(),
                Arrays.asList(
                        new AnswerEvent(5L, "retrieval_started", "STARTED",
                                Instant.parse("2026-09-02T00:00:01Z")),
                        new AnswerEvent(6L, "citation", "产品档案",
                                Instant.parse("2026-09-02T00:00:02Z"))));

        final JsonNode payload = new ObjectMapper().findAndRegisterModules()
                .valueToTree(ChatController.MessageResponse.from(message));

        assertEquals("retrieval_started", payload.path("executionEvents").get(0).path("type").asText());
        assertEquals("CITATION", payload.path("artifacts").get(0).path("type").asText());
        assertEquals("产品档案", payload.path("artifacts").get(0).path("reference").asText());
    }
}
