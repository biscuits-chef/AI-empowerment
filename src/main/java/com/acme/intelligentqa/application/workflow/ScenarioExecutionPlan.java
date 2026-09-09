package com.acme.intelligentqa.application.workflow;

import com.acme.intelligentqa.domain.model.QueryScenario;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 带版本的场景执行计划，集中声明一个场景需要执行的节点顺序。
 */
public final class ScenarioExecutionPlan {
    /** 查询场景。 */
    private final QueryScenario scenario;
    /** 执行计划版本。 */
    private final String version;
    /** 按顺序执行的节点编码。 */
    private final List<WorkflowNodeCode> nodeCodes;

    /**
     * 创建不可变场景执行计划。
     *
     * @param scenario 查询场景。
     * @param version 执行计划版本。
     * @param nodeCodes 按顺序执行的节点编码。
     */
    public ScenarioExecutionPlan(
            final QueryScenario scenario,
            final String version,
            final List<WorkflowNodeCode> nodeCodes) {
        this.scenario = Objects.requireNonNull(scenario, "scenario must not be null");
        this.version = requireText(version, "version");
        if (Objects.requireNonNull(nodeCodes, "nodeCodes must not be null").isEmpty()) {
            throw new IllegalArgumentException("nodeCodes must not be empty");
        }
        this.nodeCodes = Collections.unmodifiableList(new ArrayList<>(nodeCodes));
    }

    /**
     * 返回查询场景。
     *
     * @return 查询场景。
     */
    public QueryScenario scenario() { return scenario; }

    /**
     * 返回执行计划版本。
     *
     * @return 执行计划版本。
     */
    public String version() { return version; }

    /**
     * 返回按顺序排列的节点编码。
     *
     * @return 不可变节点编码列表。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The list is an unmodifiable defensive copy")
    public List<WorkflowNodeCode> nodeCodes() { return nodeCodes; }

    /**
     * 校验文本非空并返回去除首尾空格后的值。
     *
     * @param value 待校验文本。
     * @param field 字段名称。
     * @return 去除首尾空格后的文本。
     */
    private static String requireText(final String value, final String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
