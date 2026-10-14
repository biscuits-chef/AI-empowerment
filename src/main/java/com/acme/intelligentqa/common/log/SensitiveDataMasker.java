package com.acme.intelligentqa.common.log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志敏感数据脱敏工具：针对身份证、银行卡号、手机号、姓名、居住地址与邮箱地址执行脱敏处理。
 */
public final class SensitiveDataMasker {

    /**
     * 18 位二代居民身份证号码正则表达式。
     */
    private static final Pattern ID_CARD_18 = Pattern.compile(
            "(?<![0-9a-zA-Z])([1-9]\\d{5})(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])"
                    + "(\\d{3}[0-9Xx])(?![0-9a-zA-Z])");

    /**
     * 15 位一代居民身份证号码正则表达式。
     */
    private static final Pattern ID_CARD_15 = Pattern.compile(
            "(?<![0-9a-zA-Z])([1-9]\\d{5})\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])(\\d{3})(?![0-9a-zA-Z])");

    /**
     * 16 至 19 位银行卡号正则表达式。
     */
    private static final Pattern BANK_CARD = Pattern.compile(
            "(?<!\\d)([1-9]\\d{5})(\\d{6,9})(\\d{4})(?!\\d)");

    /**
     * 11 位中国大陆手机号码（支持可选的 +86 前缀）正则表达式。
     */
    private static final Pattern MOBILE_PHONE = Pattern.compile(
            "(?<!\\d)(\\+?86[- ]?)?(1[3-9]\\d)(\\d{4})(\\d{4})(?!\\d)");

    /**
     * 电子邮箱地址正则表达式。
     */
    private static final Pattern EMAIL = Pattern.compile(
            "(?<![a-zA-Z0-9._%+-])([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    /**
     * 单独出现的 Bearer 令牌正则表达式。
     */
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)(Bearer\\s+)([A-Za-z0-9._\\-+/=]{20,})");

    /**
     * 键值对或 JSON 属性中的敏感凭据正则表达式。
     */
    private static final Pattern SENSITIVE_CREDENTIAL = Pattern.compile(
            "(?i)([\"']?(?:token|refresh[-_]?token|access[-_]?token|password|passwd|pwd|secret|"
                    + "authorization|credential|access[-_]?key|api[-_]?key)[\"']?\\s*[:=：]\\s*[\"']?)"
                    + "([^\"'\\s,;}&]+)([\"']?)");

    /**
     * 键值对或 JSON 属性中的姓名字段正则表达式。
     */
    private static final Pattern SENSITIVE_NAME = Pattern.compile(
            "(?i)([\"']?(?:name|userName|username|realName|real_name|customerName|customer_name|"
                    + "clientName|client_name|ownerName|owner_name|contactName|contact_name|"
                    + "holderName|holder_name|姓名|客户姓名|用户姓名|持卡人姓名|持卡人|联系人|经办人)"
                    + "[\"']?\\s*[:=：]\\s*[\"']?)([^\"',;\\r\\n}\\]]+)([\"']?)");

    /**
     * 中文自由文本与结构化数据中的居住地址模式正则表达式（省市区县开头并包含路街号楼等门牌细节）。
     */
    private static final Pattern ADDRESS_TEXT = Pattern.compile(
            "((?:北京|上海|天津|重庆)市?(?:[^省市区县\\r\\n]{1,6}[区县])?|"
                    + "(?:河北|山西|辽宁|吉林|黑龙江|江苏|浙江|安徽|福建|江西|山东|河南|湖北|湖南|广东|海南|四川|贵州|云南|"
                    + "陕西|甘肃|青海|内蒙古|广西|西藏|宁夏|新疆)(?:省|自治区)?(?:[^省市区县\\r\\n]{1,6}市)?(?:[^省市区县\\r\\n]{1,6}[区县旗])?)"
                    + "([^\"',;\\r\\n。!？\\s，、*]{2,40}?(?:路|街|道|巷|弄|号|栋|幢|室|村|镇|庄|小区|单元)[^\"',;\\r\\n。!？\\s，、*]*)");

    /**
     * 键值对或 JSON 属性中的居住地址字段正则表达式。
     */
    private static final Pattern SENSITIVE_ADDRESS = Pattern.compile(
            "(?i)([\"']?(?:address|addr|residentialAddress|residential_address|homeAddress|home_address|"
                    + "liveAddress|live_address|addressDetail|address_detail|contactAddress|contact_address|"
                    + "地址|居住地址|家庭地址|联系地址|住址|详细地址)[\"']?\\s*[:=：]\\s*[\"']?)"
                    + "([^\"',;\\r\\n}\\]]+)([\"']?)");

    /**
     * 掩码替换文本。
     */
    private static final String MASK_FOUR_STARS = "****";

    /**
     * 工具类私有构造器。
     */
    private SensitiveDataMasker() {
    }

    /**
     * 对输入文本中的全部敏感信息执行脱敏处理。
     *
     * @param input 原始文本。
     * @return 脱敏后的文本；入参为 null 或空时返回原值。
     */
    public static String mask(final String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        result = maskBearerToken(result);
        result = maskCredentials(result);
        result = maskIdCard(result);
        result = maskBankCard(result);
        result = maskMobilePhone(result);
        result = maskEmail(result);
        result = maskNameFields(result);
        result = maskAddressTextPattern(result);
        result = maskAddressFields(result);
        return result;
    }

    /**
     * 脱敏 Bearer 令牌。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    private static String maskBearerToken(final String text) {
        return BEARER_TOKEN.matcher(text).replaceAll("$1***");
    }

    /**
     * 脱敏口令、密钥等敏感凭据。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    private static String maskCredentials(final String text) {
        final Matcher matcher = SENSITIVE_CREDENTIAL.matcher(text);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String value = matcher.group(2);
            if ("Bearer".equalsIgnoreCase(value)) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            } else {
                final String replacement = matcher.group(1) + "***" + matcher.group(3);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 脱敏 18 位与 15 位居民身份证号。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    public static String maskIdCard(final String text) {
        String result = ID_CARD_18.matcher(text).replaceAll("$1********$2");
        return ID_CARD_15.matcher(result).replaceAll("$1******$2");
    }

    /**
     * 脱敏 16 至 19 位银行卡号。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    public static String maskBankCard(final String text) {
        return BANK_CARD.matcher(text).replaceAll("$1******$3");
    }

    /**
     * 脱敏 11 位手机号码（保留前 3 位与后 4 位）。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    public static String maskMobilePhone(final String text) {
        final Matcher matcher = MOBILE_PHONE.matcher(text);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String prefix = matcher.group(1);
            final String head = matcher.group(2);
            final String tail = matcher.group(4);
            final String replacement = (prefix != null ? prefix : "") + head + MASK_FOUR_STARS + tail;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 脱敏电子邮箱地址（保留域名及用户名前后字符）。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    public static String maskEmail(final String text) {
        final Matcher matcher = EMAIL.matcher(text);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String local = matcher.group(1);
            final String domain = matcher.group(2);
            final String replacement = maskEmailLocal(local) + "@" + domain;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 脱敏邮箱前缀。
     *
     * @param local 邮箱前缀字符串。
     * @return 脱敏后的邮箱前缀。
     */
    private static String maskEmailLocal(final String local) {
        if (local.length() <= 2) {
            return local.charAt(0) + "***";
        }
        return local.charAt(0) + "***" + local.substring(local.length() - 1);
    }

    /**
     * 脱敏键值对与 JSON 属性中的姓名字段。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    private static String maskNameFields(final String text) {
        final Matcher matcher = SENSITIVE_NAME.matcher(text);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String prefix = matcher.group(1);
            final String rawName = matcher.group(2);
            final String suffix = matcher.group(3);
            final String replacement = prefix + maskNameValue(rawName) + suffix;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 对单个姓名文本执行脱敏。
     *
     * @param rawName 原始姓名。
     * @return 脱敏后的姓名。
     */
    public static String maskNameValue(final String rawName) {
        if (rawName == null) {
            return null;
        }
        final String trimmed = rawName.trim();
        if (trimmed.isEmpty() || trimmed.contains("*")) {
            return rawName;
        }
        if (trimmed.length() <= 1) {
            return trimmed;
        }
        if (trimmed.length() == 2) {
            return trimmed.charAt(0) + "*";
        }
        if (trimmed.length() == 3) {
            return trimmed.charAt(0) + "*" + trimmed.charAt(2);
        }
        return trimmed.charAt(0) + "**" + trimmed.charAt(trimmed.length() - 1);
    }

    /**
     * 脱敏键值对与 JSON 属性中的居住地址字段。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    private static String maskAddressFields(final String text) {
        final Matcher matcher = SENSITIVE_ADDRESS.matcher(text);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String prefix = matcher.group(1);
            final String rawAddress = matcher.group(2);
            final String suffix = matcher.group(3);
            final String replacement = prefix + maskAddressValue(rawAddress) + suffix;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 对单个居住地址文本执行脱敏（保留行政区划，遮蔽门牌与房间等细节）。
     *
     * @param rawAddress 原始居住地址。
     * @return 脱敏后的居住地址。
     */
    public static String maskAddressValue(final String rawAddress) {
        if (rawAddress == null) {
            return null;
        }
        final String trimmed = rawAddress.trim();
        if (trimmed.isEmpty() || trimmed.contains("*")) {
            return rawAddress;
        }
        final int splitIndex = findAdministrativeSplitIndex(trimmed);
        if (splitIndex > 0) {
            return trimmed.substring(0, splitIndex) + MASK_FOUR_STARS;
        }
        if (trimmed.length() <= 6) {
            final int keep = Math.min(2, trimmed.length());
            return trimmed.substring(0, keep) + MASK_FOUR_STARS;
        }
        return trimmed.substring(0, 6) + MASK_FOUR_STARS;
    }

    /**
     * 寻找地址中的行政区划（区、县、旗、市）分割点。
     *
     * @param address 地址文本。
     * @return 行政区划结尾字符索引加一；未找到时返回 -1。
     */
    private static int findAdministrativeSplitIndex(final String address) {
        int best = -1;
        final char[] primarySuffixes = new char[]{'区', '县', '旗'};
        for (final char suffix : primarySuffixes) {
            final int pos = address.lastIndexOf(suffix);
            if (pos >= 2 && pos < address.length() - 1) {
                best = Math.max(best, pos + 1);
            }
        }
        if (best > 0) {
            return best;
        }
        final int cityPos = address.lastIndexOf('市');
        if (cityPos >= 2 && cityPos < address.length() - 1) {
            return cityPos + 1;
        }
        return -1;
    }

    /**
     * 脱敏自由文本中的中文居住地址模式。
     *
     * @param text 待处理文本。
     * @return 处理后的文本。
     */
    private static String maskAddressTextPattern(final String text) {
        return ADDRESS_TEXT.matcher(text).replaceAll("$1****");
    }
}
