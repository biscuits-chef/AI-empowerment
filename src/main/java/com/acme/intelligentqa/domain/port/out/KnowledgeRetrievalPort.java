package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import java.util.List;

/**
 * 按解析后的查询从公司知识库检索授权片段的出站端口。
 */
public interface KnowledgeRetrievalPort {

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
    List<KnowledgeChunk> search(String ownerId, String question, int limit);
}
