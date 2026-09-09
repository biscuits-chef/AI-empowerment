package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.common.error.FileNotReadyException;
import com.acme.intelligentqa.common.error.ResourceNotFoundException;
import com.acme.intelligentqa.domain.model.QuestionFileReference;
import com.acme.intelligentqa.domain.model.TemporaryFile;
import com.acme.intelligentqa.domain.port.out.TemporaryFileRepositoryPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 集中校验问题附件并保存问题与文件的不可变引用。
 */
@Component
public class QuestionFileCoordinator {

    /** 单个问题允许引用的附件数量上限。 */
    private static final int MAXIMUM_QUESTION_FILES = 5;
    /** 临时文件元数据和问题关联仓储。 */
    private final TemporaryFileRepositoryPort fileRepository;

    /**
     * 创建问题附件协调器。
     *
     * @param fileRepository 临时文件元数据和问题关联仓储。
     */
    public QuestionFileCoordinator(final TemporaryFileRepositoryPort fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * 校验文件引用数量、去重、归属和就绪状态。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param files 待校验文件引用。
     * @return 防御复制后的有效文件引用。
     */
    public List<QuestionFileReference> validate(
            final String ownerId,
            final UUID conversationId,
            final List<QuestionFileReference> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        if (files.size() > MAXIMUM_QUESTION_FILES) {
            throw new IllegalArgumentException("单个问题最多引用 5 个附件");
        }
        final List<QuestionFileReference> result = new ArrayList<>(files.size());
        final Set<UUID> uniqueIds = new LinkedHashSet<>();
        for (final QuestionFileReference reference : files) {
            result.add(validateOne(ownerId, conversationId, reference, uniqueIds));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 保存问题与文件的不可变引用。
     *
     * @param questionId 问题 ID。
     * @param references 已校验的文件引用。
     * @param now 当前时间。
     */
    public void attach(
            final UUID questionId,
            final List<QuestionFileReference> references,
            final Instant now) {
        fileRepository.attachToQuestion(questionId, references, now);
    }

    /**
     * 把原问题已经固化的附件引用复制到重新发起的新问题。
     *
     * @param originalQuestionId 原问题 ID。
     * @param newQuestionId 新问题 ID。
     * @param now 当前时间。
     */
    public void copyForRegeneration(
            final UUID originalQuestionId,
            final UUID newQuestionId,
            final Instant now) {
        final List<QuestionFileReference> references = fileRepository.listQuestionReferences(
                Objects.requireNonNull(originalQuestionId, "originalQuestionId must not be null"));
        fileRepository.attachToQuestion(
                Objects.requireNonNull(newQuestionId, "newQuestionId must not be null"),
                references,
                Objects.requireNonNull(now, "now must not be null"));
    }

    /**
     * 判断重放请求中的附件用途是否与原问题已经固化的快照一致。
     *
     * @param questionId 已持久化的问题 ID。
     * @param references 重放请求携带的附件引用。
     * @return 文件 ID 与用途完全一致时返回 true。
     */
    public boolean matchesQuestionReferences(
            final UUID questionId,
            final List<QuestionFileReference> references) {
        final Map<UUID, TemporaryFile.Usage> expected = referenceMap(
                fileRepository.listQuestionReferences(Objects.requireNonNull(
                        questionId, "questionId must not be null")));
        final Map<UUID, TemporaryFile.Usage> actual = referenceMap(references);
        return expected != null && expected.equals(actual);
    }

    /**
     * 将附件列表转换为不受输入顺序影响的幂等请求指纹片段。
     *
     * @param references 待转换的附件引用。
     * @return 合法且无重复时返回文件用途映射，非法时返回空值。
     */
    private Map<UUID, TemporaryFile.Usage> referenceMap(
            final List<QuestionFileReference> references) {
        if (references == null) {
            return Collections.emptyMap();
        }
        final Map<UUID, TemporaryFile.Usage> result = new LinkedHashMap<>();
        for (final QuestionFileReference reference : references) {
            if (reference == null || result.put(reference.fileId(), reference.usage()) != null) {
                return null;
            }
        }
        return result;
    }

    /**
     * 校验一个文件引用的唯一性、归属和就绪状态。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param reference 文件引用。
     * @param uniqueIds 本次请求已出现的文件 ID。
     * @return 已校验的文件引用。
     */
    private QuestionFileReference validateOne(
            final String ownerId,
            final UUID conversationId,
            final QuestionFileReference reference,
            final Set<UUID> uniqueIds) {
        final QuestionFileReference validReference = Objects.requireNonNull(
                reference, "file reference must not be null");
        if (!uniqueIds.add(validReference.fileId())) {
            throw new IllegalArgumentException("同一个附件不能重复引用");
        }
        final TemporaryFile file = fileRepository.findActive(
                        ownerId, conversationId, validReference.fileId())
                .orElseThrow(() -> new ResourceNotFoundException("temporary file not found"));
        if (file.status() != TemporaryFile.Status.READY) {
            throw new FileNotReadyException();
        }
        return validReference;
    }
}
