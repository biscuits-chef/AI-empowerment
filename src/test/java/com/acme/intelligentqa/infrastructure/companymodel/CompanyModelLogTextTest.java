package com.acme.intelligentqa.infrastructure.companymodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * 验证 CompanyModelLogText 的长文本截断与边界行为。
 */
class CompanyModelLogTextTest {

    /**
     * 验证空文本或未超限文本原样返回。
     */
    @Test
    void returnsOriginalTextWhenWithinLimitOrNull() {
        assertNull(CompanyModelLogText.abbreviate(null, 50));
        assertEquals("hello", CompanyModelLogText.abbreviate("hello", 10));
        assertEquals("exact", CompanyModelLogText.abbreviate("exact", 5));
    }

    /**
     * 验证超长文本截断并附带总字符数提示。
     */
    @Test
    void truncatesLongTextWithCharacterCount() {
        final String input = "1234567890abcdef";
        final String result = CompanyModelLogText.abbreviate(input, 10);
        assertEquals("1234567890...(共 16 字符)", result);
    }

    /**
     * 验证非正数限制时采用默认限制截断。
     */
    @Test
    void usesDefaultLimitWhenMaxIsNonPositive() {
        final StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            longText.append('x');
        }
        final String result = CompanyModelLogText.abbreviate(longText.toString(), -1);
        assertEquals(200 + "...(共 250 字符)".length(), result.length());
    }

    /**
     * 验证敏感信息在被截断前已执行脱敏处理。
     */
    @Test
    void masksSensitiveDataBeforeAbbreviating() {
        final String input = "用户手机号13812345678提交了查询请求。";
        final String result = CompanyModelLogText.abbreviate(input, 50);
        assertEquals("用户手机号138****5678提交了查询请求。", result);
    }
}
