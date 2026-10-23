package com.acme.intelligentqa.infrastructure.companymodel;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公司大模型输出结构化载体，支持推荐问题、耗时与未来属性动态扩展。
 */
public class CompanyModelPayload {

    /**
     * 模型回答正文文本。
     */
    @JsonProperty("answer")
    private String answer;

    /**
     * 推荐追问问题列表。
     */
    @JsonProperty("rec_questions")
    private List<String> recQuestions = new ArrayList<>();

    /**
     * 智能体平台调用耗时（毫秒）。
     */
    @JsonProperty("time_cost")
    private Integer timeCost;

    /**
     * 动态扩展属性映射。
     */
    private final Map<String, Object> extraFields = new LinkedHashMap<>();

    /**
     * 根节点已知框架控制字段名集合，提取扩展属性时予以排除。
     */
    private static final Set<String> KNOWN_ROOT_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "event", "id", "message_id", "MessageID", "conversation_id", "ConversationID",
            "task_id", "TaskID", "Answer", "answer", "rec_questions", "time_cost", "latency",
            "input_tokens", "output_tokens", "token_in", "error_msg", "error", "message", "detail",
            "message_title", "status")));

    /**
     * 创建空的公司模型结构化载体实例。
     */
    public CompanyModelPayload() {
    }

    /**
     * 从 SSE 根数据节点反序列化或提取结构化载体。
     *
     * @param root JSON 根节点。
     * @param mapper JSON 对象映射器。
     * @return 提取填充后的结构化载体；如果节点为空则返回 null。
     */
    public static CompanyModelPayload fromRoot(final JsonNode root, final ObjectMapper mapper) {
        if (root == null) {
            return null;
        }
        final CompanyModelPayload payload = new CompanyModelPayload();
        populateRecQuestions(payload, root.get("rec_questions"));
        populateTimeCost(payload, root);
        populateExtraFields(payload, root, mapper);
        return payload;
    }

    /**
     * 从可能嵌套的回答文本反序列化结构化载体。
     *
     * @param rawAnswer 原始回答文本。
     * @param mapper JSON 映射器。
     * @return 解析成功的结构化载体；无法反序列化时返回 null。
     */
    public static CompanyModelPayload fromInner(final String rawAnswer, final ObjectMapper mapper) {
        if (rawAnswer == null) {
            return null;
        }
        final String trimmed = rawAnswer.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        try {
            return mapper.readValue(trimmed, CompanyModelPayload.class);
        } catch (final IOException ignored) {
            return null;
        }
    }

    /**
     * 从 JSON 节点中提取推荐问题列表。
     *
     * @param payload 结构化载体。
     * @param recNode 推荐问题节点。
     */
    private static void populateRecQuestions(final CompanyModelPayload payload, final JsonNode recNode) {
        if (recNode != null && recNode.isArray()) {
            final List<String> questions = new ArrayList<>();
            for (final JsonNode item : recNode) {
                if (item.isTextual()) {
                    questions.add(item.asText());
                }
            }
            payload.setRecQuestions(questions);
        }
    }

    /**
     * 从 JSON 根节点中提取执行耗时。
     *
     * @param payload 结构化载体。
     * @param root 根节点。
     */
    private static void populateTimeCost(final CompanyModelPayload payload, final JsonNode root) {
        final JsonNode costNode = root.get("time_cost");
        if (costNode != null && costNode.isNumber()) {
            payload.setTimeCost(costNode.asInt());
            return;
        }
        final JsonNode latencyNode = root.get("latency");
        if (latencyNode != null && latencyNode.isNumber()) {
            final double latency = latencyNode.asDouble();
            final int calculated = latency < 1000.0 && latency > 0.0
                    ? (int) Math.round(latency * 1000.0) : (int) Math.round(latency);
            payload.setTimeCost(calculated);
        }
    }

    /**
     * 从 JSON 根节点中提取全部未显式声明的扩展字段。
     *
     * @param payload 结构化载体。
     * @param root 根节点。
     * @param mapper JSON 映射器。
     */
    private static void populateExtraFields(
            final CompanyModelPayload payload,
            final JsonNode root,
            final ObjectMapper mapper) {
        final Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final String fieldName = entry.getKey();
            if (!KNOWN_ROOT_FIELDS.contains(fieldName)) {
                payload.putExtraField(fieldName, mapper.convertValue(entry.getValue(), Object.class));
            }
        }
    }

    /**
     * 获取模型回答正文文本。
     *
     * @return 模型回答正文文本；若不存在则为 null。
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * 设置模型回答正文文本。
     *
     * @param answer 模型回答正文文本。
     */
    public void setAnswer(final String answer) {
        this.answer = answer;
    }

    /**
     * 获取推荐追问问题列表。
     *
     * @return 不可变的推荐追问问题列表。
     */
    public List<String> getRecQuestions() {
        return recQuestions == null ? Collections.emptyList() : Collections.unmodifiableList(recQuestions);
    }

    /**
     * 设置推荐追问问题列表。
     *
     * @param recQuestions 推荐追问问题列表。
     */
    public void setRecQuestions(final List<String> recQuestions) {
        this.recQuestions = recQuestions == null ? new ArrayList<>() : new ArrayList<>(recQuestions);
    }

    /**
     * 获取智能体平台调用耗时（毫秒）。
     *
     * @return 智能体平台调用耗时；若未提供则为 null。
     */
    public Integer getTimeCost() {
        return timeCost;
    }

    /**
     * 设置智能体平台调用耗时（毫秒）。
     *
     * @param timeCost 智能体平台调用耗时。
     */
    public void setTimeCost(final Integer timeCost) {
        this.timeCost = timeCost;
    }

    /**
     * 动态设置未显式声明的扩展字段。
     *
     * @param key 扩展字段键名。
     * @param value 扩展字段值。
     */
    @JsonAnySetter
    public void putExtraField(final String key, final Object value) {
        if (key != null) {
            this.extraFields.put(key, value);
        }
    }

    /**
     * 获取全部动态扩展字段。
     *
     * @return 不可变的动态扩展字段映射。
     */
    @JsonAnyGetter
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Unmodifiable view of extra fields is returned")
    public Map<String, Object> getExtraFields() {
        return Collections.unmodifiableMap(extraFields);
    }
}
