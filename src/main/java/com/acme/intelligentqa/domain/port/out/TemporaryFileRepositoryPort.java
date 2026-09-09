package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 保存临时文件元数据、处理状态及问题关联的持久化端口。
 */
public interface TemporaryFileRepositoryPort {

    /**
     * 按用户和幂等键查询已有上传结果。
     *
     * @param ownerId 用户所有者 ID。
     * @param idempotencyKey 上传幂等键。
     * @return 已存在的临时文件。
     */
    Optional<TemporaryFile> findByIdempotencyKey(String ownerId, String idempotencyKey);

    /**
     * 保存处于上传中的文件元数据。
     *
     * @param file 临时文件元数据。
     * @param idempotencyKey 上传幂等键。
     * @return 持久化后的临时文件。
     */
    TemporaryFile create(TemporaryFile file, String idempotencyKey);

    /**
     * 更新文件处理状态。
     *
     * @param fileId 文件 ID。
     * @param status 新处理状态。
     * @param now 更新时间。
     * @return 更新后的临时文件。
     */
    Optional<TemporaryFile> updateStatus(UUID fileId, TemporaryFile.Status status, Instant now);

    /**
     * 按用户和会话查询未删除文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param limit 数量上限。
     * @return 临时文件列表。
     */
    List<TemporaryFile> list(String ownerId, UUID conversationId, int limit);

    /**
     * 读取当前用户、当前会话内未删除的指定文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param fileId 文件 ID。
     * @return 临时文件。
     */
    Optional<TemporaryFile> findActive(String ownerId, UUID conversationId, UUID fileId);

    /**
     * 判断文件是否已被任一用户问题引用。
     *
     * @param fileId 文件 ID。
     * @return 已被问题引用时返回 true。
     */
    boolean hasQuestionReference(UUID fileId);

    /**
     * 查询原问题已经固化的文件引用。
     *
     * @param questionId 原问题 ID。
     * @return 原问题的不可变文件引用。
     */
    List<QuestionFileReference> listQuestionReferences(UUID questionId);

    /**
     * 将文件标记为等待物理清理。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param fileId 文件 ID。
     * @param now 当前时间。
     * @return 被标记的文件。
     */
    Optional<TemporaryFile> markDeletePending(
            String ownerId, UUID conversationId, UUID fileId, Instant now);

    /**
     * 为用户问题保存不可变文件引用。
     *
     * @param questionId 问题 ID。
     * @param references 文件引用列表。
     * @param now 当前时间。
     */
    void attachToQuestion(UUID questionId, List<QuestionFileReference> references, Instant now);
}
