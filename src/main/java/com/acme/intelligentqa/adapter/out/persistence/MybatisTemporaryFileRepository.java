package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.QuestionFileMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.QuestionFileReferencePersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.TemporaryFileMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.TemporaryFilePersistenceRecord;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.out.TemporaryFileRepositoryPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于原生 MyBatis 保存临时文件元数据和问题文件不可变关联。
 */
@Repository
public class MybatisTemporaryFileRepository implements TemporaryFileRepositoryPort {

    /** 临时文件映射器。 */
    private final TemporaryFileMapper fileMapper;
    /** 问题文件关联映射器。 */
    private final QuestionFileMapper questionFileMapper;

    /**
     * 创建临时文件仓储适配器。
     *
     * @param fileMapper 临时文件映射器。
     * @param questionFileMapper 问题文件关联映射器。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected mappers are retained and not exposed")
    public MybatisTemporaryFileRepository(
            final TemporaryFileMapper fileMapper,
            final QuestionFileMapper questionFileMapper) {
        this.fileMapper = fileMapper;
        this.questionFileMapper = questionFileMapper;
    }

    /**
     * 按用户和幂等键查询已有上传结果。
     *
     * @param ownerId 用户所有者 ID。
     * @param idempotencyKey 上传幂等键。
     * @return 已存在的临时文件。
     */
    @Override
    public Optional<TemporaryFile> findByIdempotencyKey(
            final String ownerId, final String idempotencyKey) {
        return Optional.ofNullable(execute(
                        () -> fileMapper.selectByIdempotencyKey(ownerId, idempotencyKey),
                        "failed to read idempotent file"))
                .map(MybatisTemporaryFileRepository::toDomain);
    }

    /**
     * 保存处于上传中的文件元数据。
     *
     * @param file 临时文件元数据。
     * @param idempotencyKey 上传幂等键。
     * @return 持久化后的临时文件。
     */
    @Override
    public TemporaryFile create(final TemporaryFile file, final String idempotencyKey) {
        final TemporaryFilePersistenceRecord record = toRecord(file, idempotencyKey);
        requireOne(execute(() -> fileMapper.insert(record), "failed to create temporary file"));
        return file;
    }

    /**
     * 更新文件处理状态。
     *
     * @param fileId 文件 ID。
     * @param status 新处理状态。
     * @param now 更新时间。
     * @return 更新后的临时文件。
     */
    @Override
    public Optional<TemporaryFile> updateStatus(
            final UUID fileId, final TemporaryFile.Status status, final Instant now) {
        if (execute(() -> fileMapper.updateStatus(
                fileId.toString(), status.name(), Timestamp.from(now)),
                "failed to update temporary file") != 1) {
            return Optional.empty();
        }
        return findById(fileId);
    }

    /**
     * 按用户和会话查询未删除文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param limit 数量上限。
     * @return 临时文件列表。
     */
    @Override
    public List<TemporaryFile> list(
            final String ownerId, final UUID conversationId, final int limit) {
        final List<TemporaryFilePersistenceRecord> records = execute(
                () -> fileMapper.selectActive(ownerId, conversationId.toString(), limit),
                "failed to list temporary files");
        final List<TemporaryFile> result = new ArrayList<>(records.size());
        for (final TemporaryFilePersistenceRecord record : records) {
            result.add(toDomain(record));
        }
        return result;
    }

    /**
     * 读取当前用户、当前会话内未删除的指定文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param fileId 文件 ID。
     * @return 临时文件。
     */
    @Override
    public Optional<TemporaryFile> findActive(
            final String ownerId, final UUID conversationId, final UUID fileId) {
        return Optional.ofNullable(execute(
                        () -> fileMapper.selectOwnedActive(
                                ownerId, conversationId.toString(), fileId.toString()),
                        "failed to read temporary file"))
                .map(MybatisTemporaryFileRepository::toDomain);
    }

    /**
     * 判断文件是否已被任一用户问题引用。
     *
     * @param fileId 文件 ID。
     * @return 已被问题引用时返回 true。
     */
    @Override
    public boolean hasQuestionReference(final UUID fileId) {
        return execute(
                () -> questionFileMapper.countByFileId(fileId.toString()),
                "failed to count temporary file references") > 0;
    }

    /**
     * 查询原问题已经固化的文件引用。
     *
     * @param questionId 原问题 ID。
     * @return 原问题的不可变文件引用。
     */
    @Override
    public List<QuestionFileReference> listQuestionReferences(final UUID questionId) {
        final List<QuestionFileReferencePersistenceRecord> records = execute(
                () -> questionFileMapper.selectByQuestionId(questionId.toString()),
                "failed to list question file references");
        final List<QuestionFileReference> references = new ArrayList<>(records.size());
        for (final QuestionFileReferencePersistenceRecord record : records) {
            references.add(new QuestionFileReference(
                    UUID.fromString(record.getFileId()),
                    TemporaryFile.Usage.valueOf(record.getUsageType())));
        }
        return references;
    }

    /**
     * 将文件标记为等待物理清理。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param fileId 文件 ID。
     * @param now 当前时间。
     * @return 被标记的文件。
     */
    @Override
    public Optional<TemporaryFile> markDeletePending(
            final String ownerId,
            final UUID conversationId,
            final UUID fileId,
            final Instant now) {
        final Optional<TemporaryFile> current = findActive(ownerId, conversationId, fileId);
        if (!current.isPresent()) {
            return Optional.empty();
        }
        final Timestamp deletedAt = Timestamp.from(now);
        return execute(() -> fileMapper.markDeletePending(
                ownerId, conversationId.toString(), fileId.toString(), deletedAt),
                "failed to delete temporary file") == 1
                ? current : Optional.empty();
    }

    /**
     * 为用户问题保存不可变文件引用。
     *
     * @param questionId 问题 ID。
     * @param references 文件引用列表。
     * @param now 当前时间。
     */
    @Override
    @Transactional
    public void attachToQuestion(
            final UUID questionId,
            final List<QuestionFileReference> references,
            final Instant now) {
        for (final QuestionFileReference reference : references) {
            requireOne(execute(
                    () -> questionFileMapper.insertReference(
                            questionId.toString(), reference.fileId().toString(),
                            reference.usage().name(), Timestamp.from(now)),
                    "failed to attach temporary file"));
        }
    }

    /**
     * 按文件 ID 读取未删除记录。
     *
     * @param fileId 文件 ID。
     * @return 临时文件。
     */
    private Optional<TemporaryFile> findById(final UUID fileId) {
        return Optional.ofNullable(execute(
                        () -> fileMapper.selectById(fileId.toString()), "failed to read temporary file"))
                .map(MybatisTemporaryFileRepository::toDomain);
    }

    /**
     * 将领域文件转换为数据库记录。
     *
     * @param file 临时文件。
     * @param idempotencyKey 上传幂等键。
     * @return 数据库记录。
     */
    private static TemporaryFilePersistenceRecord toRecord(
            final TemporaryFile file, final String idempotencyKey) {
        final TemporaryFilePersistenceRecord record = new TemporaryFilePersistenceRecord();
        record.setPublicId(file.id().toString());
        record.setConversationId(file.conversationId().toString());
        record.setOwnerId(file.ownerId());
        record.setIdempotencyKey(idempotencyKey);
        record.setOriginalName(file.originalName());
        record.setContentType(file.contentType());
        record.setSizeBytes(file.sizeBytes());
        record.setSha256(file.sha256());
        record.setObjectKey(file.objectKey());
        record.setUsageType(file.usage().name());
        record.setStatus(file.status().name());
        record.setCreatedAt(Timestamp.from(file.createdAt()));
        record.setUpdatedAt(Timestamp.from(file.updatedAt()));
        return record;
    }

    /**
     * 将数据库记录转换为领域文件。
     *
     * @param record 数据库记录。
     * @return 临时文件。
     */
    private static TemporaryFile toDomain(final TemporaryFilePersistenceRecord record) {
        return new TemporaryFile(
                UUID.fromString(record.getPublicId()),
                UUID.fromString(record.getConversationId()),
                record.getOwnerId(),
                record.getOriginalName(),
                record.getContentType(),
                record.getSizeBytes(),
                record.getSha256(),
                record.getObjectKey(),
                TemporaryFile.Usage.valueOf(record.getUsageType()),
                TemporaryFile.Status.valueOf(record.getStatus()),
                record.createdAtInstant(),
                record.updatedAtInstant());
    }

    /**
     * 执行数据库操作并统一转换依赖异常。
     *
     * @param operation 数据库操作。
     * @param message 安全错误上下文。
     * @return 数据库操作结果。
     */
    private <T> T execute(final Supplier<T> operation, final String message) {
        try {
            return operation.get();
        } catch (final DataAccessException exception) {
            throw new PersistenceOperationException(message, exception);
        }
    }

    /**
     * 要求数据库写操作恰好影响一行。
     *
     * @param affectedRows 实际影响行数。
     */
    private void requireOne(final int affectedRows) {
        if (affectedRows != 1) {
            throw new PersistenceOperationException(
                    "expected one affected row", new IllegalStateException("affected rows: " + affectedRows));
        }
    }
}
