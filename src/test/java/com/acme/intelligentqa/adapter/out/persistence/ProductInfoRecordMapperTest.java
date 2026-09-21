package com.acme.intelligentqa.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.ProductInfoPersistenceRecord;
import com.acme.intelligentqa.domain.model.ProductInfoDto;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 验证产品信息记录映射器的字段映射、日期标准化与异常校验。
 */
class ProductInfoRecordMapperTest {

    /**
     * 待测试的产品信息记录映射器。
     */
    private ProductInfoRecordMapper mapper;

    /**
     * 初始化测试实例。
     */
    @BeforeEach
    void setUp() {
        mapper = new ProductInfoRecordMapper();
    }

    /**
     * 验证所有字段从 DTO 正确映射到持久化记录。
     */
    @Test
    void mapsAllFieldsFromDtoToRecord() {
        final ProductInfoDto dto = createFullDto();
        final ProductInfoPersistenceRecord record = mapper.toRecord(dto, "2026-09-18");

        assertNotNull(record);
        assertEquals("P001", record.getPrdcCd());
        assertEquals("FP001", record.getFmlPrdcCd());
        assertEquals("FP_IDNT", record.getFmlPrdcIdnt());
        assertEquals("母产品名称", record.getFthrPrdcNm());
        assertEquals("测试产品", record.getPrdcNm());
        assertEquals("测试简称", record.getPrdcAbbr());
        assertEquals("测试产品全称", record.getPrdcFllNm());
        assertEquals("C001", record.getCatenaCd());
        assertEquals("系列一", record.getCatenaNm());
        assertEquals("开放式", record.getOpnTyp());
        assertEquals("固收", record.getPrdcTyp());
        assertEquals("类别A", record.getPrdtTp());
        assertEquals("形态1", record.getPrdcFrm());
        assertEquals("公募", record.getIssMthd());
        assertEquals("网下", record.getRsMthd());
        assertEquals("CNY", record.getRsCrrn());
        assertEquals("中长期", record.getTrmTyp());
        assertEquals("分类甲", record.getPrdcClss());
        assertEquals("单元A", record.getGrpUntNm());
        assertEquals("资产类", record.getPrdcAsstTyp());
        assertEquals("银行间", record.getPrdcMrktTyp());
        assertEquals("Y", record.getPftaFiprIdnt());
        assertEquals("主账户", record.getAccnNm());
        assertEquals("某某分行", record.getSuborgNm());
        assertEquals("自主", record.getDvlpTyp());
        assertEquals("定开", record.getOpnClss());
        assertEquals("品牌A", record.getPrdcBrnd());
        assertEquals("稳健", record.getPrdcPstn());
        assertEquals("子品牌1", record.getPrdcBrnd2());
        assertEquals("子品牌2", record.getPrdcBrnd3());
        assertEquals("R2", record.getRiskGrade());
        assertEquals("A类", record.getShrTyp());
        assertEquals("Y", record.getIsPsnlPnsn());
        assertEquals("定期", record.getIntrMthd());
        assertEquals("绿色金融", record.getPrdcThm());
        assertEquals("张三", record.getInvsMngrNm());
        assertEquals("李四", record.getPrdcInvsMngr());
        assertEquals("王五", record.getPrdcMngrNm());
        assertEquals("2027-12-31", record.getExprDt());
        assertEquals("2026-10-15", record.getEndPrdExprDt());
        assertEquals("2026-09-18", record.getAcctDt());
    }

    /**
     * 验证八位紧凑格式日期正确转换为标准连字符格式。
     */
    @Test
    void normalizesEightDigitDateFormat() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P002");
        dto.setEXPR_DT("20271231");
        dto.setEND_PRD_EXPR_DT("20261015");
        dto.setACCT_DT("20260918");

        final ProductInfoPersistenceRecord record = mapper.toRecord(dto, null);

        assertNotNull(record);
        assertEquals("2027-12-31", record.getExprDt());
        assertEquals("2026-10-15", record.getEndPrdExprDt());
        assertEquals("2026-09-18", record.getAcctDt());
    }

    /**
     * 验证当 DTO 缺少分区日期时使用元数据兜底日期。
     */
    @Test
    void usesFallbackSnapshotDateWhenAcctDtMissing() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P003");
        dto.setACCT_DT("");

        final ProductInfoPersistenceRecord record = mapper.toRecord(dto, "20260918");

        assertNotNull(record);
        assertEquals("2026-09-18", record.getAcctDt());
    }

    /**
     * 验证缺少产品代码时抛出非法参数异常。
     */
    @Test
    void throwsWhenPrdcCdMissing() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setACCT_DT("2026-09-18");

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toRecord(dto, "2026-09-18"));
        assertTrue(ex.getMessage().contains("PRDC_CD"));
    }

    /**
     * 验证缺少快照分区日期且无兜底日期时抛出异常。
     */
    @Test
    void throwsWhenAcctDtAndFallbackMissing() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P004");

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toRecord(dto, null));
        assertTrue(ex.getMessage().contains("ACCT_DT"));
    }

    /**
     * 验证无效日历日期（如二月30日）抛出异常。
     */
    @Test
    void throwsOnInvalidCalendarDate() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P005");
        dto.setACCT_DT("2026-02-30");

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toRecord(dto, null));
        assertTrue(ex.getMessage().contains("无效日历日期"));
    }

    /**
     * 验证非法格式日期（如长度不符）抛出异常。
     */
    @Test
    void throwsOnInvalidDateFormat() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P006");
        dto.setACCT_DT("2026/09/18");

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toRecord(dto, null));
        assertTrue(ex.getMessage().contains("日期格式非法"));
    }

    /**
     * 验证空列表与空 DTO 的安全处理。
     */
    @Test
    void handlesNullAndEmpty() {
        assertNull(mapper.toRecord(null, "2026-09-18"));
        assertTrue(mapper.toRecords(null, "2026-09-18").isEmpty());
        assertTrue(mapper.toRecords(Collections.emptyList(), "2026-09-18").isEmpty());

        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P007");
        dto.setACCT_DT("2026-09-18");
        final List<ProductInfoPersistenceRecord> list =
                mapper.toRecords(Collections.singletonList(dto), null);
        assertEquals(1, list.size());
        assertEquals("P007", list.get(0).getPrdcCd());
    }

    /**
     * 构造完整的全字段测试 DTO。
     *
     * @return 全字段填充的 ProductInfoDto。
     */
    private ProductInfoDto createFullDto() {
        final ProductInfoDto dto = new ProductInfoDto();
        dto.setPRDC_CD("P001");
        dto.setFML_PRDC_CD("FP001");
        dto.setFML_PRDC_IDNT("FP_IDNT");
        dto.setFTHR_PRDC_NM("母产品名称");
        dto.setPRDC_NM("测试产品");
        dto.setPRDC_ABBR("测试简称");
        dto.setPRDC_FLL_NM("测试产品全称");
        dto.setCATENA_CD("C001");
        dto.setCATENA_NM("系列一");
        dto.setOPN_TYP("开放式");
        dto.setPRDC_TYP("固收");
        dto.setPRDT_TP("类别A");
        dto.setPRDC_FRM("形态1");
        dto.setISS_MTHD("公募");
        dto.setRS_MTHD("网下");
        dto.setRS_CRRN("CNY");
        dto.setTRM_TYP("中长期");
        dto.setPRDC_CLSS("分类甲");
        dto.setGRP_UNT_NM("单元A");
        dto.setPRDC_ASST_TYP("资产类");
        dto.setPRDC_MRKT_TYP("银行间");
        dto.setPFTA_FIPR_IDNT("Y");
        dto.setACCN_NM("主账户");
        dto.setSUBORG_NM("某某分行");
        dto.setDVLP_TYP("自主");
        dto.setOPN_CLSS("定开");
        dto.setPRDC_BRND("品牌A");
        dto.setPRDC_PSTN("稳健");
        dto.setPRDC_BRND_2("子品牌1");
        dto.setPRDC_BRND_3("子品牌2");
        dto.setRISK_GRADE("R2");
        dto.setSHR_TYP("A类");
        dto.setIS_PSNL_PNSN("Y");
        dto.setINTR_MTHD("定期");
        dto.setPRDC_THM("绿色金融");
        dto.setINVS_MNGR_NM("张三");
        dto.setPRDC_INVS_MNGR("李四");
        dto.setPRDC_MNGR_NM("王五");
        dto.setEXPR_DT("2027-12-31");
        dto.setEND_PRD_EXPR_DT("2026-10-15");
        dto.setACCT_DT("2026-09-18");
        return dto;
    }
}
