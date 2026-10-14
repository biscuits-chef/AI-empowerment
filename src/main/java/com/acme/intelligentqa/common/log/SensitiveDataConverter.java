package com.acme.intelligentqa.common.log;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback 框架级日志脱敏转换器：自动拦截通过 SLF4J 输出的所有日志消息并执行敏感数据脱敏。
 */
public class SensitiveDataConverter extends MessageConverter {

    /**
     * 创建 {@code SensitiveDataConverter} 实例。
     */
    public SensitiveDataConverter() {
        super();
    }

    /**
     * 转换并脱敏日志事件中的消息内容。
     *
     * @param event 日志事件对象。
     * @return 脱敏后的日志消息文本。
     */
    @Override
    public String convert(final ILoggingEvent event) {
        final String message = super.convert(event);
        return SensitiveDataMasker.mask(message);
    }
}
