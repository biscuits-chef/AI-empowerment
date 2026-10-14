package com.acme.intelligentqa.infrastructure.companymodel;

import com.acme.intelligentqa.common.log.SensitiveDataMasker;

/**
 * 公司模型日志文本处理工具：统一长文本截断与敏感数据脱敏，防止大报文刷屏或敏感内容在日志中泄露。
 */
final class CompanyModelLogText {

    /**
     * 默认最大保留字符数。
     */
    static final int DEFAULT_MAX = 200;

    /**
     * 工具类私有构造器。
     */
    private CompanyModelLogText() {
    }

    /**
     * 先对敏感信息进行脱敏，再截断过长文本，超出部分替换为 {@code ...(共 N 字符)}。
     *
     * @param text 原始文本。
     * @param max 最大保留字符数（小于等于 0 时按默认值处理）。
     * @return 脱敏并截断后的文本；入参为 null 时返回 null。
     */
    static String abbreviate(final String text, final int max) {
        if (text == null) {
            return null;
        }
        final String masked = SensitiveDataMasker.mask(text);
        final int limit = max > 0 ? max : DEFAULT_MAX;
        if (masked.length() <= limit) {
            return masked;
        }
        return masked.substring(0, limit) + "...(共 " + masked.length() + " 字符)";
    }
}
