package com.acme.intelligentqa.infrastructure;

import com.acme.intelligentqa.config.RagProperties;
import com.acme.intelligentqa.domain.model.KnowledgeChunk;
import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.client.TRSResultSet;
import com.trs.hybase.client.params.ConnectParams;
import com.trs.hybase.client.params.SearchParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 海贝向量知识库客户端，提供基于向量的相似度查询功能。
 */
public final class VectorStoreClient {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreClient.class);

    /**
     * 海贝向量数据库配置。
     */
    private final RagProperties ragProperties;

    /**
     * 创建海贝向量知识库客户端。
     *
     * @param ragProperties 海贝向量数据库配置。
     */
    public VectorStoreClient(final RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /**
     * 执行向量相似度查询并返回知识片段列表。
     *
     * @param ownerId 用户所有者 ID（目前未使用，保留接口一致性）。
     * @param question 用户问题，用于转换为向量查询。
     * @param limit 返回结果数量上限。
     * @return 知识片段列表。
     */
    public List<KnowledgeChunk> search(final String ownerId, final String question, final int limit) {
        try (TRSConnection conn = createConnection()) {
            SearchParams params = createSearchParams();
            List<Float> vectors = convertQuestionToVectors(question);
            TRSResultSet res = executeQuery(conn, params, vectors, Math.min(limit, ragProperties.queryLimit()));

            return processResultSet(res);
        } catch (final TRSException e) {
            handleException(e);
            return new ArrayList<>();
        }
    }

    /**
     * 创建海贝数据库连接。
     *
     * @return 海贝数据库连接。
     */
    private TRSConnection createConnection() {
        return new TRSConnection(
                ragProperties.urls(),
                ragProperties.username(),
                ragProperties.password(),
                new ConnectParams()
        );
    }

    /**
     * 创建查询参数。
     *
     * @return 查询参数对象。
     */
    private SearchParams createSearchParams() {
        SearchParams params = new SearchParams();
        params.setSortMethod(ragProperties.sortMethod());
        params.setReadColumns(ragProperties.readColumns());
        params.setProperty("search.read.strong.consistency", String.valueOf(ragProperties.strongConsistency()));
        params.setProperty("search.vector.candidates", String.valueOf(ragProperties.vectorCandidates()));
        return params;
    }

    /**
     * 将问题文本转换为向量。
     *
     * @param question 用户问题。
     * @return 向量列表。
     */
    private List<Float> convertQuestionToVectors(final String question) {
        throw new UnsupportedOperationException("向量转换暂未实现，请接入向量 embedding 服务");
    }

    /**
     * 执行查询。
     *
     * @param conn 数据库连接。
     * @param params 查询参数。
     * @param vectors 待查询的向量列表。
     * @param limit 结果数量限制。
     * @return 查询结果集。
     * @throws TRSException 查询执行失败时抛出。
     */
    private TRSResultSet executeQuery(
            final TRSConnection conn,
            final SearchParams params,
            final List<Float> vectors,
            final int limit) throws TRSException {
        String qualifiedCollection = ragProperties.database() + "." + ragProperties.collection();
        String vectorExpression = vectors.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "vec:", ""));

        return conn.executeSelect(
                qualifiedCollection,
                vectorExpression,
                ragProperties.queryOffset(),
                limit,
                params
        );
    }

    /**
     * 处理查询结果集。
     *
     * @param res 查询结果集。
     * @return 知识片段列表。
     * @throws TRSException 处理结果集时抛出异常。
     */
    private List<KnowledgeChunk> processResultSet(final TRSResultSet res) throws TRSException {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int i = 0; i < res.size(); i++) {
            res.moveNext();
            TRSRecord record = res.get();
            String sourceId = extractSourceId(record);
            String title = extractTitle(record);
            String content = extractContent(record);
            chunks.add(new KnowledgeChunk(sourceId, title, content));
        }
        return chunks;
    }

    /**
     * 从记录中提取来源 ID。
     *
     * @param record 数据库记录。
     * @return 来源 ID。
     */
    private String extractSourceId(final TRSRecord record) {
        try {
            return record.getString("id");
        } catch (final TRSException e) {
            return String.valueOf(record.hashCode());
        }
    }

    /**
     * 从记录中提取标题。
     *
     * @param record 数据库记录。
     * @return 标题文本。
     */
    private String extractTitle(final TRSRecord record) {
        try {
            return record.getString("标题");
        } catch (final TRSException e) {
            return "未命名文档";
        }
    }

    /**
     * 从记录中提取内容。
     *
     * @param record 数据库记录。
     * @return 内容文本。
     */
    private String extractContent(final TRSRecord record) {
        try {
            return record.getString("正文");
        } catch (final TRSException e) {
            return "";
        }
    }

    /**
     * 处理异常并输出错误信息。
     *
     * @param e 捕获的异常。
     */
    private void handleException(final TRSException e) {
        log.error("ErrorCode:" + e.getErrorCode());
        log.error("ErrorString:" + e.getErrorString());
    }
}
