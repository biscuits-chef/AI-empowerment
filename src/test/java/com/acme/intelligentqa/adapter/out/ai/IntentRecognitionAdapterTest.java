package com.acme.intelligentqa.adapter.out.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.QueryIntent;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * 验证 IntentRecognitionAdapter 的业务行为与边界。
 */
class IntentRecognitionAdapterTest {

    /**
     * 验证显式产品引用和最新文档意图提取。
     */
    @Test
    void extractsExplicitProductAndLatestDocumentIntent() {
        final QueryIntent intent = adapter(true).recognize(
                "user-1", "悦享三号的最新费率调整公告", Collections.emptyList());

        assertEquals(QueryIntent.Type.PRODUCT_LATEST_DOCUMENT_INFO, intent.type());
        assertEquals("悦享三号", intent.entities().get("productReference"));
        assertTrue(intent.unresolvedEntities().isEmpty());
    }

    /**
     * 验证指代词被标记为待确认而不是被猜测。
     */
    @Test
    void marksPronounAsUnresolvedInsteadOfGuessing() {
        final QueryIntent intent = adapter(true).recognize(
                "user-1", "它的管理费率是多少？", Collections.emptyList());

        assertTrue(intent.entities().isEmpty());
        assertEquals(Collections.singleton("productReference"), intent.unresolvedEntities());
    }

    /**
     * 验证显式歧义仅返回有界候选集合。
     */
    @Test
    void returnsBoundedCandidatesForExplicitAmbiguity() {
        final QueryIntent intent = adapter(true).recognize(
                "user-1", "产品名称：悦享一号还是悦享二号？", Collections.emptyList());

        assertEquals(2, intent.candidates().get("productReference").size());
        assertEquals("悦享二号", intent.candidates().get("productReference").get(1).reference());
    }

    /**
     * 验证投资经理问题识别为双口径投资经理查询并提取产品引用。
     */
    @Test
    void recognizesInvestmentManagerQuestion() {
        final QueryIntent intent = adapter(true).recognize(
                "user-1", "悦享三号的投资经理是谁？", Collections.emptyList());

        assertEquals(QueryIntent.Type.PRODUCT_INVESTMENT_MANAGER_QUERY, intent.type());
        assertEquals("悦享三号", intent.entities().get("productReference"));
        assertTrue(intent.unresolvedEntities().isEmpty());
    }

    /**
     * 验证产品经理问题和投资经理问题不会混淆。
     */
    @Test
    void recognizesProductManagerQuestion() {
        final QueryIntent intent = adapter(true).recognize(
                "user-1", "P001的产品经理是谁？", Collections.emptyList());

        assertEquals(QueryIntent.Type.PRODUCT_MANAGER_QUERY, intent.type());
        assertEquals("P001", intent.entities().get("productReference"));
    }

    /**
     * 验证“今日基准日”无需产品引用即可识别为产品日期列表查询。
     */
    @Test
    void recognizesTodayReferenceDateListWithoutProductReference() {
        final QueryIntent intent = adapter(true).recognize(
                "user-1", "今日基准日产品有哪些？", Collections.emptyList());

        assertEquals(QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY, intent.type());
        assertTrue(intent.entities().isEmpty());
        assertTrue(intent.unresolvedEntities().isEmpty());
    }

    /**
     * 验证显式业务日期会作为结构化参数传入后续查询规划。
     */
    @Test
    void extractsExplicitBusinessDateForReferenceDateList() {
        final QueryIntent intent = adapter(true).recognize(
                "user-1", "2026-09-08有哪些到期产品？", Collections.emptyList());

        assertEquals(QueryIntent.Type.PRODUCT_REFERENCE_DATE_LIST_QUERY, intent.type());
        assertEquals("2026-09-08", intent.entities().get("businessDate"));
    }

    /**
     * 验证未命中一期批准问法时返回不支持和零置信度，禁止默认猜测产品意图。
     */
    @Test
    void rejectsUnknownQuestionInsteadOfGuessingProductIntent() {
        final QueryIntent intent = adapter(true).recognize(
                "user-1", "今天天气怎么样？", Collections.emptyList());

        assertEquals(QueryIntent.Type.UNSUPPORTED, intent.type());
        assertEquals(0.0D, intent.confidence());
        assertTrue(intent.entities().isEmpty());
    }

    /**
     * 验证非演示模式下未配置生产识别器时拒绝执行。
     */
    @Test
    void failsClosedOutsideDemoMode() {
        assertThrows(DependencyUnavailableException.class, () -> adapter(false).recognize(
                "user-1", "悦享三号的费率", Collections.emptyList()));
    }

    /**
     * 处理被测适配器。
     *
     * @param demoMode 演示模式开关。
     *
     * @return 被测适配器。
     */
    private IntentRecognitionAdapter adapter(final boolean demoMode) {
        return new IntentRecognitionAdapter(new QaProperties(
                demoMode, 4000, 30000, 8, 20000, 20000, 20, 0.8D, 512, 2, 8, 200));
    }
}
