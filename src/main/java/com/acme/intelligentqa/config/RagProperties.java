package com.acme.intelligentqa.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * 海贝向量数据库连接配置参数。
 */
@ConfigurationProperties("app.rag")
@ConstructorBinding
public final class RagProperties {

    /**
     * 海贝TR服务器地址，支持多地址以分号分隔。
     */
    private final String urls;

    /**
     * 连接用户名。
     */
    private final String username;

    /**
     * 连接密码。
     */
    private final String password;

    /**
     * 数据库名称。
     */
    private final String database;

    /**
     * 集合名称。
     */
    private final String collection;

    /**
     * 搜索结果排序方式。
     */
    private final String sortMethod;

    /**
     * 查询返回字段，以分号分隔。
     */
    private final String readColumns;

    /**
     * 是否开启强一致性读取。
     */
    private final boolean strongConsistency;

    /**
     * 向量候选集大小。
     */
    private final int vectorCandidates;

    /**
     * 查询偏移量。
     */
    private final int queryOffset;

    /**
     * 查询结果数量限制。
     */
    private final int queryLimit;

    /**
     * 创建海贝向量数据库配置。
     *
     * @param urls 海贝TR服务器地址。
     * @param username 连接用户名。
     * @param password 连接密码。
     * @param database 数据库名称。
     * @param collection 集合名称。
     * @param sortMethod 搜索结果排序方式。
     * @param readColumns 查询返回字段。
     * @param strongConsistency 是否开启强一致性读取。
     * @param vectorCandidates 向量候选集大小。
     * @param queryOffset 查询偏移量。
     * @param queryLimit 查询结果数量限制。
     */
    public RagProperties(
            final String urls,
            final String username,
            final String password,
            final String database,
            final String collection,
            final String sortMethod,
            final String readColumns,
            final boolean strongConsistency,
            final int vectorCandidates,
            final int queryOffset,
            final int queryLimit) {
        this.urls = requireNonEmpty(urls, "urls");
        this.username = requireNonEmpty(username, "username");
        this.password = requireNonEmpty(password, "password");
        this.database = requireNonEmpty(database, "database");
        this.collection = requireNonEmpty(collection, "collection");
        this.sortMethod = requireNonEmpty(sortMethod, "sort-method");
        this.readColumns = requireNonEmpty(readColumns, "read-columns");
        requirePositive(vectorCandidates, "vector-candidates");
        requirePositive(queryOffset, "query-offset");
        requirePositive(queryLimit, "query-limit");
        this.strongConsistency = strongConsistency;
        this.vectorCandidates = vectorCandidates;
        this.queryOffset = queryOffset;
        this.queryLimit = queryLimit;
    }

    /**
     * 返回海贝TR服务器地址。
     *
     * @return 海贝TR服务器地址。
     */
    public String urls() {
        return urls;
    }

    /**
     * 返回连接用户名。
     *
     * @return 连接用户名。
     */
    public String username() {
        return username;
    }

    /**
     * 返回连接密码。
     *
     * @return 连接密码。
     */
    public String password() {
        return password;
    }

    /**
     * 返回数据库名称。
     *
     * @return 数据库名称。
     */
    public String database() {
        return database;
    }

    /**
     * 返回集合名称。
     *
     * @return 集合名称。
     */
    public String collection() {
        return collection;
    }

    /**
     * 返回搜索结果排序方式。
     *
     * @return 搜索结果排序方式。
     */
    public String sortMethod() {
        return sortMethod;
    }

    /**
     * 返回查询返回字段。
     *
     * @return 查询返回字段。
     */
    public String readColumns() {
        return readColumns;
    }

    /**
     * 返回是否开启强一致性读取。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    public boolean strongConsistency() {
        return strongConsistency;
    }

    /**
     * 返回向量候选集大小。
     *
     * @return 向量候选集大小。
     */
    public int vectorCandidates() {
        return vectorCandidates;
    }

    /**
     * 返回查询偏移量。
     *
     * @return 查询偏移量。
     */
    public int queryOffset() {
        return queryOffset;
    }

    /**
     * 返回查询结果数量限制。
     *
     * @return 查询结果数量限制。
     */
    public int queryLimit() {
        return queryLimit;
    }

    /**
     * 校验字符串非空。
     *
     * @param value 输入值。
     * @param propertyName 属性名称。
     * @return 规范化后的字符串。
     */
    private static String requireNonEmpty(final String value, final String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("app.rag." + propertyName + " must not be blank");
        }
        return value.trim();
    }

    /**
     * 校验数值必须为正数。
     *
     * @param value 输入值。
     * @param propertyName 属性名称。
     */
    private static void requirePositive(final int value, final String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException("app.rag." + propertyName + " must be positive");
        }
    }
}
