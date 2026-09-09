package com.acme.intelligentqa.adapter.out.ai;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.EntityCandidate;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.port.out.IntentRecognitionPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 意图识别适配器；当前本地规则只用于演示，生产未配置时拒绝猜测。
 */
@Component
public class IntentRecognitionAdapter implements IntentRecognitionPort {

    /**
     * 产品引用字段名。
     */
    private static final String PRODUCT_REFERENCE = "productReference";
    /**
     * 交易引用字段名。
     */
    private static final String TRADE_REFERENCE = "tradeReference";
    /**
     * 显式产品引用提取规则。
     */
    private static final Pattern EXPLICIT_PRODUCT = Pattern.compile(
            "产品(?:名称|代码|编号)?[：:\\s]+([^，。？?]{2,80})");
    /**
     * 自然语言产品名称提取规则。
     */
    private static final Pattern NATURAL_PRODUCT = Pattern.compile(
            "([^，。？?]{2,40}?)(?:的)?(?:产品经理|投资经理|管理费率|费率|成立日期|起止日期|说明书|公告|备案通知书|分层|调整计划)");
    /**
     * 交易引用提取规则。
     */
    private static final Pattern TRADE = Pattern.compile(
            "(?:交易流水号|交易编号|交易标识)[：:\\s]*([A-Za-z0-9_-]{2,64})");
    /**
     * 显式 ISO 业务日期提取规则。
     */
    private static final Pattern BUSINESS_DATE = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})");
    /**
     * 配置参数。
     */
    private final QaProperties properties;

    /**
     * 创建 {@code IntentRecognitionAdapter} 实例。
     *
     * @param properties 配置参数。
     */
    public IntentRecognitionAdapter(final QaProperties properties) {
        this.properties = properties;
    }

    /**
     * 结合问题和历史识别受控意图。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param question 用户问题。
     *
     * @param history 最近对话历史。
     *
     * @return 结合问题和历史识别受控意图。
     */
    @Override
    public QueryIntent recognize(
            final String ownerId,
            final String question,
            final List<ChatMessage> history) {
        if (!properties.demoMode()) {
            throw new DependencyUnavailableException(
                    "INTENT_RECOGNITION_UNCONFIGURED", "intent recognition adapter is not configured");
        }
        final QueryIntent.Type type = recognizeType(question);
        return recognizeEntities(type, question);
    }

    /**
     * 根据一期批准问法识别稳定业务意图。
     *
     * @param question 用户问题。
     * @return 一期支持的查询意图。
     */
    private QueryIntent.Type recognizeType(final String question) {
        if (isReferenceDateQuestion(question)) {
            return QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY;
        }
        if (question.contains("产品经理")) {
            return QueryIntent.Type.PRODUCT_MANAGER_QUERY;
        }
        if (question.contains("投资经理")) {
            return QueryIntent.Type.PRODUCT_INVESTMENT_MANAGER_QUERY;
        }
        if (containsLatestDocumentKeyword(question)) {
            return QueryIntent.Type.PRODUCT_LATEST_DOCUMENT_INFO;
        }
        if (isProductQuestion(question) || isTradeQuestion(question)) {
            return QueryIntent.Type.PRODUCT_TRADE_BASIC_INFO;
        }
        return QueryIntent.Type.UNSUPPORTED;
    }

    /**
     * 从演示问题中提取产品或交易实体。
     *
     * @param type 类型。
     *
     * @param question 用户问题。
     *
     * @return 从演示问题中提取产品或交易实体。
     */
    private QueryIntent recognizeEntities(final QueryIntent.Type type, final String question) {
        final Map<String, String> entities = new LinkedHashMap<>();
        final Set<String> unresolved = new HashSet<>();
        final Map<String, List<EntityCandidate>> candidates = new LinkedHashMap<>();
        if (type == QueryIntent.Type.UNSUPPORTED) {
            // 演示规则没有命中已批准问法时必须降低置信度并进入确定性追问，不能默认猜成产品查询。
            return new QueryIntent(type, 0.0D, entities, unresolved, candidates);
        }
        if (type == QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY) {
            final String businessDate = firstMatch(BUSINESS_DATE, question);
            if (businessDate != null) {
                entities.put("businessDate", businessDate);
            }
            return new QueryIntent(type, 1.0D, entities, unresolved, candidates);
        }
        final String tradeReference = firstMatch(TRADE, question);
        if (tradeReference != null) {
            entities.put(TRADE_REFERENCE, tradeReference);
        } else if (isTradeQuestion(question)) {
            unresolved.add(TRADE_REFERENCE);
        } else {
            recognizeProduct(question, entities, unresolved, candidates);
        }
        return new QueryIntent(type, 1.0D, entities, unresolved, candidates);
    }

    /**
     * 判断问题是否查询定开基准日或产品到期日列表。
     *
     * @param question 用户问题。
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean isReferenceDateQuestion(final String question) {
        final boolean dateSignal = question.contains("今日") || question.contains("今天")
                || BUSINESS_DATE.matcher(question).find();
        final boolean referenceSignal = question.contains("基准日") || question.contains("基准日期")
                || question.contains("到期产品") || question.contains("产品到期")
                || question.contains("到期") && question.contains("产品");
        return dateSignal && referenceSignal;
    }

    /**
     * 识别显式产品引用并生成有界候选。
     *
     * @param question 用户问题。
     *
     * @param entities 结构化实体。
     *
     * @param unresolved 构造包含待补充实体的识别结果。
     *
     * @param candidates 实体候选列表。
     */
    private void recognizeProduct(
            final String question,
            final Map<String, String> entities,
            final Set<String> unresolved,
            final Map<String, List<EntityCandidate>> candidates) {
        final List<String> products = productReferences(question);
        if (products.size() == 1) {
            entities.put(PRODUCT_REFERENCE, products.get(0));
        } else if (products.size() > 1) {
            final List<EntityCandidate> values = new ArrayList<>();
            for (final String product : products) {
                values.add(new EntityCandidate(product, product));
            }
            candidates.put(PRODUCT_REFERENCE, values);
        } else if (isProductQuestion(question)) {
            unresolved.add(PRODUCT_REFERENCE);
        }
    }

    /**
     * 判断问题是否包含最新文档查询关键词。
     *
     * @param question 用户问题。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean containsLatestDocumentKeyword(final String question) {
        return question.contains("说明书")
                || question.contains("公告")
                || question.contains("备案")
                || question.contains("调整计划");
    }

    /**
     * 判断问题是否属于交易查询。
     *
     * @param question 用户问题。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean isTradeQuestion(final String question) {
        return question.contains("交易员") || question.contains("这笔交易")
                || question.contains("该交易") || question.contains("上一笔交易");
    }

    /**
     * 判断问题是否属于产品查询。
     *
     * @param question 用户问题。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean isProductQuestion(final String question) {
        return question.contains("产品") || question.contains("费率") || question.contains("管理费")
                || question.contains("投资经理") || question.contains("成立日期")
                || containsLatestDocumentKeyword(question) || question.contains("它");
    }

    /**
     * 处理识别到的产品引用集合。
     *
     * @param question 用户问题。
     *
     * @return 识别到的产品引用集合。
     */
    private List<String> productReferences(final String question) {
        String captured = firstMatch(EXPLICIT_PRODUCT, question);
        if (captured == null) {
            captured = firstMatch(NATURAL_PRODUCT, question);
        }
        if (captured == null) {
            return Collections.emptyList();
        }
        captured = captured.replaceFirst("^(请问|请查询|查询|帮我查询|帮我看看|我想查询)", "")
                .replaceFirst("(?:的)?最新$", "").replaceFirst("的$", "").trim();
        if (isReferenceToken(captured)) {
            return Collections.emptyList();
        }
        final String[] parts = captured.split("(?:还是|或者|或|、)");
        final List<String> values = new ArrayList<>();
        for (final String part : parts) {
            final String value = part.trim();
            if (!value.isEmpty() && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * 判断文本是否为不能直接作为实体的指代词。
     *
     * @param value 输入值。
     *
     * @return 条件成立时返回 true，否则返回 false。
     */
    private boolean isReferenceToken(final String value) {
        return "它".equals(value) || "这个产品".equals(value)
                || "该产品".equals(value) || "此产品".equals(value)
                || "某产品".equals(value);
    }

    /**
     * 提取正则表达式的首个有效匹配。
     *
     * @param pattern 待匹配的正则表达式。
     *
     * @param question 用户问题。
     *
     * @return 提取正则表达式的首个有效匹配。
     */
    private String firstMatch(final Pattern pattern, final String question) {
        final Matcher matcher = pattern.matcher(question);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}
