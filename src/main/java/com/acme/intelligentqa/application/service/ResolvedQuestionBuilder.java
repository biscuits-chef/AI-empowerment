package com.acme.intelligentqa.application.service;

import com.acme.intelligentqa.domain.model.QueryIntent;
import java.util.Map;

/**
 * 根据已消歧实体构造供双通道查询使用的规范化问题。
 */
public final class ResolvedQuestionBuilder {

    /**
     * 禁止实例化纯函数工具类。
     */
    private ResolvedQuestionBuilder() {
    }

    /**
     * 在保留用户原问题的同时附加已确认实体，避免下游继续接收裸指代词。
     *
     * @param question 用户原问题。
     * @param intent 已完成消歧的查询意图。
     * @return 可供知识库和业务数据库查询的规范化问题。
     */
    public static String build(final String question, final QueryIntent intent) {
        if (intent.entities().isEmpty()) {
            return question;
        }
        final StringBuilder value = new StringBuilder(question).append("\n已确认查询实体：");
        for (final Map.Entry<String, String> entity : intent.entities().entrySet()) {
            value.append(entity.getKey()).append('=').append(entity.getValue()).append(';');
        }
        return value.toString();
    }
}
