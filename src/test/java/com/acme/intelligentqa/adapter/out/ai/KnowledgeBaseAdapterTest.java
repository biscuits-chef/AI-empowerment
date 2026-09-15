package com.acme.intelligentqa.adapter.out.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.config.RagProperties;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import java.util.List;

import com.acme.intelligentqa.infrastructure.VectorStoreClient;
import org.junit.jupiter.api.Test;

/**
 * 验证 KnowledgeBaseAdapter 的业务行为与边界。
 */
class KnowledgeBaseAdapterTest {

    /**
     * 验证演示模式下返回模拟数据。
     */
    @Test
    void returnsDemoDataInDemoMode() {
        final QaProperties properties = createDemoQaProperties();
        final VectorStoreClient vectorStoreClient = createMockHyKnowledge();

        final KnowledgeBaseAdapter adapter = new KnowledgeBaseAdapter(properties, vectorStoreClient);
        final List<KnowledgeChunk> chunks = adapter.search("user-1", "测试问题", 5);

        assertEquals(1, chunks.size());
        assertEquals("demo-knowledge-1", chunks.get(0).sourceId());
        assertTrue(chunks.get(0).content().contains("演示模式"));
    }

    /**
     * 验证查询错误时能优雅处理。
     */
    @Test
    void handlesQueryErrorGracefully() {
        final QaProperties properties = createNonDemoQaProperties();
        final VectorStoreClient vectorStoreClient = createMockHyKnowledge();

        final KnowledgeBaseAdapter adapter = new KnowledgeBaseAdapter(properties, vectorStoreClient);
        final List<KnowledgeChunk> chunks = adapter.search("user-1", "测试问题", 5);

        assertTrue(chunks.isEmpty());
    }

    /**
     * 创建演示模式的配置。
     *
     * @return 演示模式配置。
     */
    private QaProperties createDemoQaProperties() {
        return new QaProperties(
                true, 4000, 30000, 8, 20000, 20000, 20, 0.85D, 512, 2, 8, 200);
    }

    /**
     * 创建非演示模式的配置。
     *
     * @return 非演示模式配置。
     */
    private QaProperties createNonDemoQaProperties() {
        return new QaProperties(
                false, 4000, 30000, 8, 20000, 20000, 20, 0.85D, 512, 2, 8, 200);
    }

    /**
     * 创建模拟的海贝知识库客户端。
     *
     * @return 模拟的海贝知识库客户端。
     */
    private VectorStoreClient createMockHyKnowledge() {
        final RagProperties ragProperties = new RagProperties(
                "http://localhost:8080",
                "test",
                "password",
                "demo",
                "demo",
                "RELEVANCE",
                "标题;正文;",
                false,
                100,
                0,
                100
        );
        return new VectorStoreClient(ragProperties);
    }
}
