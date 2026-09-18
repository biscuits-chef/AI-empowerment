package com.acme.intelligentqa.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerEventPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ChatMessagePersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationContextPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ProductInfoPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.TemporaryFilePersistenceRecord;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.StreamUtils;

/**
 * 验证最终结构和前向迁移遵守统一 {@code id BIGINT AUTO_INCREMENT} 主键政策。
 */
class DatabasePrimaryKeyPolicyIT {

    /** H2 主键政策测试连接地址。 */
    private static final String DATABASE_URL =
            "jdbc:h2:mem:primary-key-policy;MODE=MySQL;DATABASE_TO_LOWER=TRUE";

    /**
     * 验证全部数据表只有一个名为 id 的数值自增主键，原业务键保持唯一。
     *
     * @throws SQLException 数据库元数据读取失败时抛出。
     */
    @Test
    void finalSchemaUsesOnlyAutoIncrementIdPrimaryKeys() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "")) {
            executeSchema(connection, "db/mybatis-test-schema.sql");
            executeSchema(connection, "db/business-query-test-schema.sql");
            final DatabaseMetaData metadata = connection.getMetaData();
            for (final String table : productionTables()) {
                assertPrimaryKey(metadata, table);
            }
            assertPrimaryKey(metadata, "test_business_trade");
            for (final Map.Entry<String, List<List<String>>> entry : businessKeys().entrySet()) {
                final List<List<String>> uniqueIndexes = uniqueIndexes(metadata, entry.getKey());
                for (final List<String> expectedColumns : entry.getValue()) {
                    assertTrue(uniqueIndexes.contains(expectedColumns),
                            entry.getKey() + " 缺少业务唯一键 " + expectedColumns);
                }
            }
        }
    }

    /**
     * 验证逐表迁移覆盖全部旧主键且最终脚本没有 pk_id。
     *
     * @throws IOException 迁移资源读取失败时抛出。
     */
    @Test
    void forwardMigrationsConvertEveryTableWithoutPkId() throws IOException {
        final String migrations = migrations(migrationNames());
        assertEquals(10, occurrences(migrations, "ALTER TABLE "));
        assertFalse(migrations.contains("pk_id"));
        assertEquals(9, occurrences(migrations, "ADD PRIMARY KEY (id)"));
        assertTrue(migrations.contains("CHANGE COLUMN event_id id BIGINT NOT NULL AUTO_INCREMENT"));
        for (final String table : productionTables()) {
            assertTrue(migrations.contains("ALTER TABLE " + table), table);
        }
    }

    /**
     * 验证 MyBatis-Plus 记录只把数据库生成的 id 声明为物理主键。
     *
     * @throws NoSuchFieldException 持久化记录缺少约定字段时抛出。
     */
    @Test
    void persistenceRecordsMapDatabaseGeneratedId() throws NoSuchFieldException {
        assertTableId(ConversationPersistenceRecord.class, "id");
        assertTableId(ChatMessagePersistenceRecord.class, "id");
        assertTableId(AnswerPersistenceRecord.class, "id");
        assertTableId(ConversationContextPersistenceRecord.class, "id");
        assertTableId(TemporaryFilePersistenceRecord.class, "id");
        assertTableId(AnswerEventPersistenceRecord.class, "id");
        assertTableId(ProductInfoPersistenceRecord.class, "id");
    }

    /**
     * 验证持久化字段名由数据库列名去除下划线并转为小驼峰形式。
     */
    @Test
    void persistenceRecordFieldsUseDatabaseColumnCamelCaseNames() {
        for (final Class<?> recordType : mappedRecordTypes()) {
            for (final Field field : recordType.getDeclaredFields()) {
                final TableId tableId = field.getAnnotation(TableId.class);
                if (tableId != null) {
                    assertEquals(toCamelCase(tableId.value()), field.getName(), recordType.getSimpleName());
                }
                final TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null && tableField.exist() && !tableField.value().isEmpty()) {
                    assertEquals(toCamelCase(tableField.value()), field.getName(), recordType.getSimpleName());
                }
            }
        }
    }

    /**
     * 验证 HiAgent 回答元数据迁移使用接口同语义字段且保留旧消息 ID 数据。
     *
     * @throws IOException 迁移资源读取失败时抛出。
     */
    @Test
    void hiAgentAnswerMetadataMigrationUsesDocumentedFieldNames() throws IOException {
        final String migration = migration("V19__align_hiagent_answer_metadata.sql");
        final String answerMapper = resource("mapper/AnswerMapper.xml");
        final String cancellationMapper = resource("mapper/AnswerCancellationMapper.xml");

        assertTrue(migration.contains(
                "CHANGE COLUMN provider_message_id message_id VARCHAR(128) NULL"));
        for (final String column : Arrays.asList(
                "app_conversation_id", "query_id", "task_id", "total_tokens", "latency",
                "tracing_json_str", "intention_json_str", "retriever_resource")) {
            assertTrue(migration.contains("ADD COLUMN " + column), column);
        }
        assertTrue(answerMapper.contains("property=\"messageId\" column=\"message_id\""));
        assertTrue(cancellationMapper.contains("property=\"messageId\" column=\"message_id\""));
        assertFalse(answerMapper.contains("property=\"providerMessageId\""));
        assertFalse(cancellationMapper.contains("property=\"providerMessageId\""));
    }

    /**
     * 验证会话 Agent 类型迁移提供旧数据兼容默认值，且映射字段命名一致。
     *
     * @throws IOException 迁移或 Mapper 资源读取失败时抛出。
     */
    @Test
    void conversationAgentTypeMigrationAndMappingArePresent() throws IOException {
        final String migration = migration("V20__persist_conversation_agent_type.sql");
        final String mapper = resource("mapper/ConversationMapper.xml");

        assertTrue(migration.contains(
                "ADD COLUMN agent_type VARCHAR(32) NOT NULL DEFAULT 'SMART_DATA'"));
        assertTrue(mapper.contains("property=\"agentType\" column=\"agent_type\""));
        assertTrue(mapper.contains("id, public_id, owner_id, creation_idempotency_key, agent_type"));
    }

    /**
     * 执行最终测试结构脚本。
     *
     * @param connection 数据库连接。
     * @param resourcePath 类路径资源。
     */
    private void executeSchema(final Connection connection, final String resourcePath) {
        ScriptUtils.executeSqlScript(connection, new EncodedResource(
                new ClassPathResource(resourcePath), StandardCharsets.UTF_8));
    }

    /**
     * 断言指定表只有一个名为 id 的非空 BIGINT 自增主键。
     *
     * @param metadata 数据库元数据。
     * @param table 表名。
     * @throws SQLException 数据库元数据读取失败时抛出。
     */
    private void assertPrimaryKey(final DatabaseMetaData metadata, final String table)
            throws SQLException {
        final List<String> actualKeys = new ArrayList<String>();
        try (ResultSet keys = metadata.getPrimaryKeys(null, null, table)) {
            while (keys.next()) {
                actualKeys.add(normalize(keys.getString("COLUMN_NAME")));
            }
        }
        assertEquals(Collections.singletonList("id"), actualKeys, table);
        try (ResultSet columns = metadata.getColumns(null, null, table, "id")) {
            assertTrue(columns.next(), table + " 缺少 id 列");
            assertEquals(Types.BIGINT, columns.getInt("DATA_TYPE"), table);
            assertEquals(DatabaseMetaData.columnNoNulls, columns.getInt("NULLABLE"), table);
            assertEquals("YES", columns.getString("IS_AUTOINCREMENT"), table);
        }
    }

    /**
     * 返回指定表全部唯一索引的有序列集合。
     *
     * @param metadata 数据库元数据。
     * @param table 表名。
     * @return 唯一索引列集合。
     * @throws SQLException 数据库元数据读取失败时抛出。
     */
    private List<List<String>> uniqueIndexes(final DatabaseMetaData metadata, final String table)
            throws SQLException {
        final Map<String, List<String>> indexes = new LinkedHashMap<String, List<String>>();
        try (ResultSet rows = metadata.getIndexInfo(null, null, table, true, false)) {
            while (rows.next()) {
                final String indexName = rows.getString("INDEX_NAME");
                final String columnName = rows.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    indexes.computeIfAbsent(indexName, ignored -> new ArrayList<String>())
                            .add(normalize(columnName));
                }
            }
        }
        return new ArrayList<List<String>>(indexes.values());
    }

    /** @return 全部生产数据表。 */
    private List<String> productionTables() {
        return Arrays.asList(
                "qa_conversation", "qa_message", "qa_answer", "qa_answer_feedback",
                "qa_answer_cancel_task", "qa_conversation_context", "qa_file",
                "qa_question_file", "qa_answer_event", "dws_product_info_d");
    }

    /**
     * 断言持久化字段映射为数据库自增 id。
     *
     * @param recordType 持久化记录类型。
     * @param fieldName Java 字段名。
     * @throws NoSuchFieldException 持久化记录缺少约定字段时抛出。
     */
    private void assertTableId(final Class<?> recordType, final String fieldName)
            throws NoSuchFieldException {
        final Field field = recordType.getDeclaredField(fieldName);
        final TableId annotation = field.getAnnotation(TableId.class);
        assertTrue(annotation != null, recordType.getSimpleName() + " 缺少 @TableId");
        assertEquals("id", annotation.value(), recordType.getSimpleName());
        assertEquals(IdType.AUTO, annotation.type(), recordType.getSimpleName());
    }

    /**
     * 返回受数据库字段命名约束的 MyBatis-Plus 持久化记录类型。
     *
     * @return 持久化记录类型列表。
     */
    private List<Class<?>> mappedRecordTypes() {
        return Arrays.<Class<?>>asList(
                ConversationPersistenceRecord.class,
                ChatMessagePersistenceRecord.class,
                AnswerPersistenceRecord.class,
                ConversationContextPersistenceRecord.class,
                TemporaryFilePersistenceRecord.class,
                AnswerEventPersistenceRecord.class,
                ProductInfoPersistenceRecord.class);
    }

    /** @return 主键切换后必须继续成立的业务唯一键。 */
    private Map<String, List<List<String>>> businessKeys() {
        final Map<String, List<List<String>>> result =
                new LinkedHashMap<String, List<List<String>>>();
        result.put("qa_conversation", keys(
                key("public_id"), key("owner_id", "creation_idempotency_key")));
        result.put("qa_message", keys(key("public_id")));
        result.put("qa_answer", keys(key("public_id"), key("owner_id", "idempotency_key")));
        result.put("qa_answer_feedback", keys(key("answer_id", "owner_id")));
        result.put("qa_answer_cancel_task", keys(key("answer_id")));
        result.put("qa_conversation_context", keys(key("conversation_id")));
        result.put("qa_file", keys(key("public_id"), key("owner_id", "idempotency_key")));
        result.put("qa_question_file", keys(key("question_id", "file_id")));
        result.put("dws_product_info_d", keys(key("prdc_cd", "acct_dt")));
        return result;
    }

    /** @param columns 唯一键列。 @return 唯一键列集合。 */
    private List<String> key(final String... columns) { return Arrays.asList(columns); }

    /** @param values 多个唯一键。 @return 唯一键集合。 */
    @SafeVarargs
    private final List<List<String>> keys(final List<String>... values) {
        return Arrays.asList(values);
    }

    /** @return V8 至 V17 逐表迁移文件名。 */
    private String[] migrationNames() {
        return new String[]{
            "V8__convert_conversation_primary_key.sql",
            "V9__convert_message_primary_key.sql",
            "V10__convert_answer_primary_key.sql",
            "V11__convert_answer_feedback_primary_key.sql",
            "V12__convert_answer_cancel_task_primary_key.sql",
            "V13__convert_conversation_context_primary_key.sql",
            "V14__convert_file_primary_key.sql",
            "V15__convert_question_file_primary_key.sql",
            "V16__rename_answer_event_primary_key.sql",
            "V17__convert_product_snapshot_primary_key.sql"
        };
    }

    /**
     * 按版本顺序读取并合并迁移文本。
     *
     * @param fileNames 迁移文件名。
     * @return 合并后的迁移文本。
     * @throws IOException 迁移资源读取失败时抛出。
     */
    private String migrations(final String... fileNames) throws IOException {
        final StringBuilder result = new StringBuilder();
        for (final String fileName : fileNames) {
            final String migration = migration(fileName);
            assertEquals(1, occurrences(migration, "ALTER TABLE "), fileName);
            result.append(migration).append('\n');
        }
        return result.toString();
    }

    /**
     * 读取指定 Flyway 迁移文件。
     *
     * @param fileName 迁移文件名。
     * @return 迁移脚本文本。
     * @throws IOException 迁移资源读取失败时抛出。
     */
    private String migration(final String fileName) throws IOException {
        return resource("db/migration/" + fileName);
    }

    /**
     * 读取指定类路径资源文本。
     *
     * @param resourcePath 类路径资源位置。
     * @return 资源文本。
     * @throws IOException 资源读取失败时抛出。
     */
    private String resource(final String resourcePath) throws IOException {
        final ClassPathResource resource = new ClassPathResource(resourcePath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * 统计固定文本出现次数。
     *
     * @param source 待检查文本。
     * @param fragment 固定片段。
     * @return 片段出现次数。
     */
    private int occurrences(final String source, final String fragment) {
        int result = 0;
        int offset = 0;
        while ((offset = source.indexOf(fragment, offset)) >= 0) {
            result++;
            offset += fragment.length();
        }
        return result;
    }

    /**
     * 将数据库下划线字段名转换为 Java 小驼峰字段名。
     *
     * @param columnName 数据库字段名。
     * @return Java 小驼峰字段名。
     */
    private String toCamelCase(final String columnName) {
        final StringBuilder result = new StringBuilder(columnName.length());
        boolean upperNext = false;
        for (int index = 0; index < columnName.length(); index++) {
            final char current = columnName.charAt(index);
            if (current == '_') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(current));
                upperNext = false;
            } else {
                result.append(Character.toLowerCase(current));
            }
        }
        return result.toString();
    }

    /** @param value 元数据名称。 @return 小写元数据名称。 */
    private String normalize(final String value) { return value.toLowerCase(Locale.ROOT); }
}
