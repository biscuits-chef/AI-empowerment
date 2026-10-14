package com.acme.intelligentqa.common.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 验证 SensitiveDataMasker 的各类敏感信息脱敏功能与边界处理。
 */
class SensitiveDataMaskerTest {

    /**
     * 验证空值或空字符串安全返回。
     */
    @Test
    void handlesNullAndEmptySafely() {
        assertNull(SensitiveDataMasker.mask(null));
        assertEquals("", SensitiveDataMasker.mask(""));
        assertEquals("普通文本无敏感信息", SensitiveDataMasker.mask("普通文本无敏感信息"));
    }

    /**
     * 验证 18 位二代居民身份证脱敏（包括末位为数字与末位为 X 的情况）。
     */
    @Test
    void masks18DigitIdCardCorrectly() {
        final String input1 = "用户身份证号为110101199003072345，请确认。";
        final String masked1 = SensitiveDataMasker.mask(input1);
        assertEquals("用户身份证号为110101********2345，请确认。", masked1);

        final String input2 = "尾号为X的证件：31011520001231888X已登记。";
        final String masked2 = SensitiveDataMasker.mask(input2);
        assertEquals("尾号为X的证件：310115********888X已登记。", masked2);
    }

    /**
     * 验证 15 位一代居民身份证脱敏。
     */
    @Test
    void masks15DigitIdCardCorrectly() {
        final String input = "历史一代身份证110101900307234需要升级。";
        final String masked = SensitiveDataMasker.mask(input);
        assertEquals("历史一代身份证110101******234需要升级。", masked);
    }

    /**
     * 验证 16 位与 19 位银行卡号脱敏（保留前 6 位与后 4 位，中间 6 位替换为星号）。
     */
    @Test
    void masksBankCardCorrectly() {
        final String input16 = "卡号6222021234567890扣款成功。";
        final String masked16 = SensitiveDataMasker.mask(input16);
        assertEquals("卡号622202******7890扣款成功。", masked16);

        final String input19 = "19位借记卡6222021234567890123交易完成。";
        final String masked19 = SensitiveDataMasker.mask(input19);
        assertEquals("19位借记卡622202******0123交易完成。", masked19);
    }

    /**
     * 验证 11 位手机号码脱敏（保留前 3 位与后 4 位，支持可选 +86 前缀）。
     */
    @Test
    void masksMobilePhoneCorrectly() {
        final String plain = "手机号13812345678收到验证码。";
        final String maskedPlain = SensitiveDataMasker.mask(plain);
        assertEquals("手机号138****5678收到验证码。", maskedPlain);

        final String international = "境外格式+86 13987654321与86-18911223344。";
        final String maskedIntl = SensitiveDataMasker.mask(international);
        assertTrue(maskedIntl.contains("+86 139****4321"));
        assertTrue(maskedIntl.contains("86-189****3344"));
    }

    /**
     * 验证电子邮箱地址脱敏（保留首尾字符与域名）。
     */
    @Test
    void masksEmailCorrectly() {
        final String standard = "发送到zhangsan@example.com和admin@company.com.cn";
        final String masked = SensitiveDataMasker.mask(standard);
        assertTrue(masked.contains("z***n@example.com"));
        assertTrue(masked.contains("a***n@company.com.cn"));

        final String shortEmail = "短前缀a@test.com与ab@test.com";
        final String maskedShort = SensitiveDataMasker.mask(shortEmail);
        assertTrue(maskedShort.contains("a***@test.com"));
    }

    /**
     * 验证 JSON 与键值对中的姓名字段脱敏（2字、3字、4字及以上中文名）。
     */
    @Test
    void masksNameInJsonAndKeyValueCorrectly() {
        final String json = "{\"name\":\"张三\",\"userName\":\"李四五\",\"realName\":\"欧阳六七\"}";
        final String maskedJson = SensitiveDataMasker.mask(json);
        assertTrue(maskedJson.contains("\"name\":\"张*\""));
        assertTrue(maskedJson.contains("\"userName\":\"李*五\""));
        assertTrue(maskedJson.contains("\"realName\":\"欧**七\""));

        final String kv = "持卡人姓名:王小明, 联系人: 诸葛孔明, 姓名：赵云";
        final String maskedKv = SensitiveDataMasker.mask(kv);
        assertTrue(maskedKv.contains("持卡人姓名:王*明"));
        assertTrue(maskedKv.contains("联系人: 诸**明"));
        assertTrue(maskedKv.contains("姓名：赵*"));
    }

    /**
     * 验证 JSON 与键值对中的居住地址脱敏（保留行政区划，脱敏门牌和楼室）。
     */
    @Test
    void masksAddressInJsonAndKeyValueCorrectly() {
        final String json = "{\"address\":\"北京市海淀区中关村南大街1号院3号楼\"}";
        final String maskedJson = SensitiveDataMasker.mask(json);
        assertEquals("{\"address\":\"北京市海淀区****\"}", maskedJson);

        final String kv = "居住地址: 上海市浦东新区张江高科科苑路88号302室";
        final String maskedKv = SensitiveDataMasker.mask(kv);
        assertEquals("居住地址: 上海市浦东新区****", maskedKv);
    }

    /**
     * 验证自由文本中的中文居住地址模式脱敏。
     */
    @Test
    void masksAddressInFreeTextCorrectly() {
        final String text = "客户住在浙江省杭州市西湖区文三路456号东方大厦2单元，工作正常。";
        final String masked = SensitiveDataMasker.mask(text);
        assertEquals("客户住在浙江省杭州市西湖区****，工作正常。", masked);
    }

    /**
     * 验证不含居住地址的普通业务文本不被地址正则误伤。
     */
    @Test
    void doesNotMaskNonAddressPlainText() {
        final String text = "产品于2026年10月14日成立，期限为三年，业绩比较基准为区间型，风险等级为R2。";
        assertEquals(text, SensitiveDataMasker.mask(text));

        final String dateText = "该产品到期日为2026-10-14，起息日为2026-01-01，请确认区间。";
        assertEquals(dateText, SensitiveDataMasker.mask(dateText));

        final String keywordText = "请说明市区两级政府对新能源项目的补贴政策与建设规模要求。";
        assertEquals(keywordText, SensitiveDataMasker.mask(keywordText));
    }

    /**
     * 验证包含全部六类敏感数据的复杂混合 Payload 能够同时被正确脱敏。
     */
    @Test
    void masksComplexMixedPayloadCorrectly() {
        final String payload = "{"
                + "\"name\":\"张三\","
                + "\"idCard\":\"110101199003072345\","
                + "\"phone\":\"13812345678\","
                + "\"bankCard\":\"6222021234567890\","
                + "\"email\":\"zhangsan@example.com\","
                + "\"address\":\"江苏省南京市鼓楼区中山北路88号302室\""
                + "}";
        final String masked = SensitiveDataMasker.mask(payload);

        assertTrue(masked.contains("\"name\":\"张*\""));
        assertTrue(masked.contains("\"idCard\":\"110101********2345\""));
        assertTrue(masked.contains("\"phone\":\"138****5678\""));
        assertTrue(masked.contains("\"bankCard\":\"622202******7890\""));
        assertTrue(masked.contains("\"email\":\"z***n@example.com\""));
        assertTrue(masked.contains("\"address\":\"江苏省南京市鼓楼区****\""));
    }

    /**
     * 验证敏感凭据（token、password）保底脱敏。
     */
    @Test
    void masksSensitiveCredentialsFallback() {
        final String text = "token=abcdef1234567890, password=myPassword123, Bearer eyJhbGciOiJIUzI1NiJ9.test";
        final String masked = SensitiveDataMasker.mask(text);
        assertTrue(masked.contains("token=***"));
        assertTrue(masked.contains("password=***"));
        assertTrue(masked.contains("Bearer ***"));
    }
}
