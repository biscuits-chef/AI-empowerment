package com.acme.intelligentqa.adapter.out.ai;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.acme.intelligentqa.domain.port.out.KnowledgeRetrievalPort;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 公司知识库检索适配器；真实平台接入前仅允许显式演示模式。
 */
@Component
public class KnowledgeBaseAdapter implements KnowledgeRetrievalPort {

    /**
     * 配置参数。
     */
    private final QaProperties properties;

    /**
     * 创建 {@code KnowledgeBaseAdapter} 实例。
     *
     * @param properties 配置参数。
     */
    public KnowledgeBaseAdapter(final QaProperties properties) {
        this.properties = properties;
    }

    /**
     * 检索公司知识库中的授权证据片段。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param question 用户问题。
     *
     * @param limit 数量上限。
     *
     * @return 检索公司知识库中的授权证据片段。
     */
    @Override
    public List<KnowledgeChunk> search(final String ownerId, final String question, final int limit) {
        if (!properties.demoMode()) {
            throw new DependencyUnavailableException(
                    "KNOWLEDGE_RETRIEVAL_UNCONFIGURED", "knowledge base adapter is not configured");
        }
        return Collections.singletonList(new KnowledgeChunk(
                "demo-knowledge-1", "本地演示知识", "当前运行在显式演示模式，尚未连接公司知识库。"));
    }
}
