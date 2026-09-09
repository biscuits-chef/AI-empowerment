package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationContextMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationContextPersistenceRecord;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import com.acme.intelligentqa.domain.model.ClarificationRequest;
import com.acme.intelligentqa.domain.model.ConversationContext;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.port.out.ConversationContextRepositoryPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/**
 * 使用乐观版本控制保存结构化会话上下文，防止并发追问相互覆盖。
 */
@Repository
public class MybatisPlusConversationContextRepository implements ConversationContextRepositoryPort {

    /**
     * 上下文 JSON 的反序列化类型。
     */
    private static final TypeReference<Map<String, Object>> STATE_TYPE =
            new TypeReference<Map<String, Object>>() { };
    /**
     * 上下文 JSON 中的实体字段名。
     */
    private static final String FIELD_ENTITIES = "entities";
    /**
     * 上下文 JSON 中逐实体来源消息 ID 的字段名。
     */
    private static final String FIELD_ENTITY_SOURCE_MESSAGE_IDS = "entitySourceMessageIds";
    /**
     * 数据库映射器。
     */
    private final ConversationContextMapper mapper;
    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建 {@code MybatisPlusConversationContextRepository} 实例。
     *
     * @param mapper 数据库映射器。
     *
     * @param objectMapper JSON 对象映射器。
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected collaborators are retained and not exposed")
    public MybatisPlusConversationContextRepository(
            final ConversationContextMapper mapper,
            final ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询满足用户隔离条件的目标记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param conversationId 会话 ID。
     *
     * @return 查询满足用户隔离条件的目标记录。
     */
    @Override
    public Optional<ConversationContext> find(final String ownerId, final UUID conversationId) {
        final LambdaQueryWrapper<ConversationContextPersistenceRecord> query =
                new LambdaQueryWrapper<ConversationContextPersistenceRecord>()
                        .eq(ConversationContextPersistenceRecord::getConversationId, conversationId.toString())
                        .eq(ConversationContextPersistenceRecord::getOwnerId, ownerId);
        try {
            return Optional.ofNullable(mapper.selectOne(query)).map(this::toDomain);
        } catch (final DataAccessException exception) {
            throw new PersistenceOperationException("failed to read conversation context", exception);
        }
    }

    /**
     * 持久化当前结构化会话状态。
     *
     * @param context 结构化会话上下文。
     *
     * @param expectedVersion 期望的乐观锁版本号。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    @Override
    public boolean save(final ConversationContext context, final long expectedVersion) {
        if (expectedVersion == 0L) {
            try {
                return mapper.insert(toRecord(context, 1L)) == 1;
            } catch (final DuplicateKeyException exception) {
                return false;
            } catch (final DataAccessException exception) {
                throw new PersistenceOperationException("failed to create conversation context", exception);
            }
        }
        final ConversationContextPersistenceRecord record = toRecord(context, expectedVersion + 1L);
        final LambdaUpdateWrapper<ConversationContextPersistenceRecord> update =
                new LambdaUpdateWrapper<ConversationContextPersistenceRecord>()
                        .eq(ConversationContextPersistenceRecord::getConversationId,
                                context.conversationId().toString())
                        .eq(ConversationContextPersistenceRecord::getOwnerId, context.ownerId())
                        .eq(ConversationContextPersistenceRecord::getVersion, expectedVersion)
                        .set(ConversationContextPersistenceRecord::getVersion, expectedVersion + 1L)
                        .set(ConversationContextPersistenceRecord::getStateJson, record.getStateJson())
                        .set(ConversationContextPersistenceRecord::getSourceQuestionId, record.getSourceQuestionId())
                        .set(ConversationContextPersistenceRecord::getUpdatedAt, record.getUpdatedAt());
        try {
            return mapper.update(null, update) == 1;
        } catch (final DataAccessException exception) {
            throw new PersistenceOperationException("failed to update conversation context", exception);
        }
    }

    /**
     * 将领域对象转换为数据库记录。
     *
     * @param context 结构化会话上下文。
     *
     * @param storedVersion 数据库已保存的乐观版本号。
     *
     * @return 将领域对象转换为数据库记录。
     */
    private ConversationContextPersistenceRecord toRecord(
            final ConversationContext context,
            final long storedVersion) {
        final ConversationContextPersistenceRecord record = new ConversationContextPersistenceRecord();
        record.setConversationId(context.conversationId().toString());
        record.setOwnerId(context.ownerId());
        record.setVersion(storedVersion);
        record.setStateJson(serialize(context));
        record.setSourceQuestionId(context.sourceQuestionId() == null
                ? null : context.sourceQuestionId().toString());
        record.setUpdatedAt(Timestamp.from(context.updatedAt()));
        return record;
    }

    /**
     * 将结构化上下文序列化为持久化 JSON。
     *
     * @param context 结构化会话上下文。
     *
     * @return 将结构化上下文序列化为持久化 JSON。
     */
    private String serialize(final ConversationContext context) {
        final Map<String, Object> state = new LinkedHashMap<>();
        state.put("schemaVersion", 1);
        state.put("lastIntent", context.lastIntent() == null ? null : context.lastIntent().name());
        state.put(FIELD_ENTITIES, context.entities());
        state.put(FIELD_ENTITY_SOURCE_MESSAGE_IDS, stringSourceMap(context.entitySourceMessageIds()));
        state.put("pending", clarificationMap(context.pendingClarification()));
        try {
            return objectMapper.writeValueAsString(state);
        } catch (final JsonProcessingException exception) {
            throw new PersistenceOperationException("failed to serialize conversation context", exception);
        }
    }

    /**
     * 处理追问状态的序列化字段映射。
     *
     * @param clarification 追问信息。
     *
     * @return 追问状态的序列化字段映射。
     */
    private Map<String, Object> clarificationMap(final ClarificationRequest clarification) {
        if (clarification == null) {
            return null;
        }
        final Map<String, Object> value = new LinkedHashMap<>();
        value.put("reason", clarification.reason().name());
        value.put("entityName", clarification.entityName());
        value.put("prompt", clarification.prompt());
        value.put("intent", intentMap(clarification.pendingIntent()));
        final List<Map<String, String>> candidates = new ArrayList<>();
        for (final EntityCandidate candidate : clarification.candidates()) {
            final Map<String, String> item = new LinkedHashMap<>();
            item.put("reference", candidate.reference());
            item.put("label", candidate.label());
            item.put("canonicalName", candidate.canonicalName());
            candidates.add(item);
        }
        value.put("candidates", candidates);
        return value;
    }

    /**
     * 处理意图的序列化字段映射。
     *
     * @param intent 查询意图。
     *
     * @return 意图的序列化字段映射。
     */
    private Map<String, Object> intentMap(final QueryIntent intent) {
        final Map<String, Object> value = new LinkedHashMap<>();
        value.put("type", intent.type().name());
        value.put("confidence", intent.confidence());
        value.put(FIELD_ENTITIES, intent.entities());
        value.put("unresolvedEntities", intent.unresolvedEntities());
        return value;
    }

    /**
     * 将数据库记录转换为领域对象。
     *
     * @param record 持久化记录。
     *
     * @return 将数据库记录转换为领域对象。
     */
    private ConversationContext toDomain(final ConversationContextPersistenceRecord record) {
        try {
            final Map<String, Object> state = objectMapper.readValue(record.getStateJson(), STATE_TYPE);
            final Number schemaVersion = (Number) state.get("schemaVersion");
            if (schemaVersion == null || schemaVersion.intValue() != 1) {
                throw new IllegalArgumentException("unsupported conversation context schema version");
            }
            final QueryIntent.Type lastIntent = state.get("lastIntent") == null ? null
                    : QueryIntent.Type.valueOf(String.valueOf(state.get("lastIntent")));
            final Map<String, String> entities = stringMap(state.get(FIELD_ENTITIES));
            final Map<String, UUID> entitySources = uuidMap(state.get(FIELD_ENTITY_SOURCE_MESSAGE_IDS));
            final ClarificationRequest pending = parseClarification(state.get("pending"));
            return new ConversationContext(
                    UUID.fromString(record.getConversationId()),
                    record.getOwnerId(),
                    record.getVersion(),
                    lastIntent,
                    entities,
                    entitySources,
                    record.getSourceQuestionId() == null ? null : UUID.fromString(record.getSourceQuestionId()),
                    pending,
                    record.getUpdatedAt().toInstant());
        } catch (final JsonProcessingException | IllegalArgumentException | ClassCastException exception) {
            throw new PersistenceOperationException("conversation context is invalid", exception);
        }
    }

    /**
     * 从持久化 JSON 恢复待追问状态。
     *
     * @param raw 尚未校验的原始输入。
     *
     * @return 从持久化 JSON 恢复待追问状态。
     */
    @SuppressWarnings("unchecked")
    private ClarificationRequest parseClarification(final Object raw) {
        if (raw == null) {
            return null;
        }
        final Map<String, Object> value = (Map<String, Object>) raw;
        final Map<String, Object> intentValue = (Map<String, Object>) value.get("intent");
        final QueryIntent intent = new QueryIntent(
                QueryIntent.Type.valueOf(String.valueOf(intentValue.get("type"))),
                ((Number) intentValue.get("confidence")).doubleValue(),
                stringMap(intentValue.get(FIELD_ENTITIES)),
                new java.util.HashSet<>((List<String>) intentValue.get("unresolvedEntities")),
                Collections.emptyMap());
        final List<EntityCandidate> candidates = new ArrayList<>();
        for (final Map<String, String> item : (List<Map<String, String>>) value.get("candidates")) {
            final String canonicalName = item.get("canonicalName") == null
                    ? item.get("label") : item.get("canonicalName");
            candidates.add(new EntityCandidate(item.get("reference"), item.get("label"), canonicalName));
        }
        return new ClarificationRequest(
                ClarificationRequest.Reason.valueOf(String.valueOf(value.get("reason"))),
                value.get("entityName") == null ? null : String.valueOf(value.get("entityName")),
                String.valueOf(value.get("prompt")),
                intent,
                candidates);
    }

    /**
     * 将持久化 JSON 字段转换为字符串映射。
     *
     * @param raw 尚未校验的原始输入。
     *
     * @return 将持久化 JSON 字段转换为字符串映射。
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> stringMap(final Object raw) {
        if (raw == null) {
            return Collections.emptyMap();
        }
        final Map<String, String> result = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : ((Map<String, Object>) raw).entrySet()) {
            result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * 将实体来源消息 ID 转换为 JSON 使用的字符串映射。
     *
     * @param sources 实体来源消息 ID 映射。
     * @return 可序列化的字符串映射。
     */
    private Map<String, String> stringSourceMap(final Map<String, UUID> sources) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (final Map.Entry<String, UUID> entry : sources.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    /**
     * 从持久化 JSON 恢复逐实体来源消息 ID。
     *
     * @param raw 尚未校验的原始来源映射；遗留记录允许为空。
     * @return 可解析且可核验的实体来源消息 ID 映射。
     */
    @SuppressWarnings("unchecked")
    private Map<String, UUID> uuidMap(final Object raw) {
        if (raw == null) {
            // 旧版上下文只有全局来源问题，不能据此虚构每个实体的真实来源。
            return Collections.emptyMap();
        }
        final Map<String, UUID> result = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : ((Map<String, Object>) raw).entrySet()) {
            try {
                result.put(entry.getKey(), UUID.fromString(String.valueOf(entry.getValue())));
            } catch (final IllegalArgumentException exception) {
                // 单个来源损坏时丢弃该来源，使后续指代走确定性追问而不是错误复用实体。
                result.remove(entry.getKey());
            }
        }
        return result;
    }
}
