package com.acme.intelligentqa.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * 表示问题提交时对一个已上传临时文件的不可变引用。
 */
public final class QuestionFileReference {

    /** 被引用的文件 ID。 */
    private final UUID fileId;
    /** 文件在本次问题中的使用角色。 */
    private final TemporaryFile.Usage usage;

    /**
     * 创建问题文件引用。
     *
     * @param fileId 被引用的文件 ID。
     * @param usage 文件使用角色。
     */
    public QuestionFileReference(final UUID fileId, final TemporaryFile.Usage usage) {
        this.fileId = Objects.requireNonNull(fileId, "fileId must not be null");
        this.usage = Objects.requireNonNull(usage, "usage must not be null");
    }

    /** @return 被引用的文件 ID。 */
    public UUID fileId() { return fileId; }
    /** @return 文件使用角色。 */
    public TemporaryFile.Usage usage() { return usage; }
}
