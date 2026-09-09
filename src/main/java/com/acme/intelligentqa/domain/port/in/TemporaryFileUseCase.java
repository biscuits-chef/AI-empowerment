package com.acme.intelligentqa.domain.port.in;

import com.acme.intelligentqa.domain.model.TemporaryFile;
import java.util.List;
import java.util.UUID;

/**
 * 提供会话临时文件上传、查询和删除能力。
 */
public interface TemporaryFileUseCase {

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
    TemporaryFile upload(
            String ownerId,
            UUID conversationId,
            String originalName,
            String contentType,
            byte[] content,
            TemporaryFile.Usage usage,
            String idempotencyKey);

    /**
     * 查询当前会话未删除的临时文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @return 当前会话临时文件列表。
     */
    List<TemporaryFile> list(String ownerId, UUID conversationId);

    /**
     * 删除当前会话内尚未用于新问题的临时文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param fileId 文件 ID。
     */
    void delete(String ownerId, UUID conversationId, UUID fileId);
}
