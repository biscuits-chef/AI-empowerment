package com.acme.intelligentqa.common.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

/**
 * 验证 SensitiveDataConverter 与 Logback 事件管道的脱敏集成。
 */
class SensitiveDataConverterTest {

    /**
     * 验证 Logback 事件中的敏感信息经过转换器后被正确脱敏。
     */
    @Test
    void convertsAndMasksLoggingEventMessage() {
        final SensitiveDataConverter converter = new SensitiveDataConverter();
        final ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(
                "用户手机号: 13812345678, 身份证: 110101199003072345, 邮箱: admin@example.com");

        final String result = converter.convert(event);

        assertEquals("用户手机号: 138****5678, 身份证: 110101********2345, 邮箱: a***n@example.com", result);
    }
}
