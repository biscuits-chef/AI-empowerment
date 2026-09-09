package com.acme.intelligentqa.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 双通道证据的确定性对账结果及人工复核标记。
 */
public final class EvidenceAssessment {

    /**
     * 双通道证据对账状态。
     */
    public enum Status {
        /**
         * 双通道证据一致。
         */
        CONSISTENT,
        /**
         * 双通道证据存在冲突，需要人工复核。
         */
        CONFLICT,
        /**
         * 现有证据不足以支持回答。
         */
        INSUFFICIENT
    }

    /**
     * 业务状态。
     */
    private final Status status;
    /**
     * 双通道存在冲突的字段集合。
     */
    private final List<String> conflictFields;

    /**
     * 创建 {@code EvidenceAssessment} 实例。
     *
     * @param status 业务状态。
     *
     * @param conflictFields 双通道存在冲突的字段集合。
     */
    public EvidenceAssessment(final Status status, final List<String> conflictFields) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.conflictFields = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(conflictFields, "conflictFields must not be null")));
        if (status != Status.CONFLICT && !this.conflictFields.isEmpty()) {
            throw new IllegalArgumentException("conflict fields require CONFLICT status");
        }
    }

    /**
     * 返回业务状态。
     *
     * @return 业务状态。
     */
    public Status status() { return status; }

    /**
     * 处理双通道存在冲突的字段集合。
     *
     * @return 双通道存在冲突的字段集合。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "List is an unmodifiable defensive copy")
    public List<String> conflictFields() { return conflictFields; }

    /**
     * 处理是否需要人工复核。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    public boolean manualReviewRequired() { return status == Status.CONFLICT; }
}
