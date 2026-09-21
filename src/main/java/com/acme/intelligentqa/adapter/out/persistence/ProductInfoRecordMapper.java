package com.acme.intelligentqa.adapter.out.persistence;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.ProductInfoPersistenceRecord;
import com.acme.intelligentqa.domain.model.ProductInfoDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 产品信息 DTO 与持久化 Record 之间的转换映射器，负责数据清洗、日期标准化与必填项校验。
 */
@Component
public class ProductInfoRecordMapper {

    /**
     * 严格日历解析器，格式为 yyyy-MM-dd。
     */
    private static final DateTimeFormatter STRICT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    /**
     * 构造产品信息转换映射器。
     */
    public ProductInfoRecordMapper() {
    }

    /**
     * 将单个 ProductInfoDto 转换为 ProductInfoPersistenceRecord。
     *
     * @param dto 产品信息传输对象。
     * @param fallbackSnapshotDate 兜底快照分区日期（格式 yyyy-MM-dd 或 yyyyMMdd），在 DTO 未显式提供时使用。
     * @return 转换后的持久化记录。
     * @throws IllegalArgumentException 当必填字段缺失或日期格式非法时抛出。
     */
    public ProductInfoPersistenceRecord toRecord(final ProductInfoDto dto, final String fallbackSnapshotDate) {
        if (dto == null) {
            return null;
        }
        final String prdcCd = trimToNull(dto.getPRDC_CD());
        if (prdcCd == null) {
            throw new IllegalArgumentException("产品代码 PRDC_CD 不能为空");
        }

        final String rawAcctDt = hasText(dto.getACCT_DT()) ? dto.getACCT_DT() : fallbackSnapshotDate;
        final String acctDt = normalizeDate(rawAcctDt, true, "ACCT_DT");
        final String exprDt = normalizeDate(dto.getEXPR_DT(), false, "EXPR_DT");
        final String endPrdExprDt = normalizeDate(dto.getEND_PRD_EXPR_DT(), false, "END_PRD_EXPR_DT");

        final ProductInfoPersistenceRecord record = new ProductInfoPersistenceRecord();
        record.setPrdcCd(prdcCd);
        record.setFmlPrdcCd(trimToNull(dto.getFML_PRDC_CD()));
        record.setFmlPrdcIdnt(trimToNull(dto.getFML_PRDC_IDNT()));
        record.setFthrPrdcNm(trimToNull(dto.getFTHR_PRDC_NM()));
        record.setPrdcNm(trimToNull(dto.getPRDC_NM()));
        record.setPrdcAbbr(trimToNull(dto.getPRDC_ABBR()));
        record.setPrdcFllNm(trimToNull(dto.getPRDC_FLL_NM()));
        record.setCatenaCd(trimToNull(dto.getCATENA_CD()));
        record.setCatenaNm(trimToNull(dto.getCATENA_NM()));
        record.setOpnTyp(trimToNull(dto.getOPN_TYP()));
        record.setPrdcTyp(trimToNull(dto.getPRDC_TYP()));
        record.setPrdtTp(trimToNull(dto.getPRDT_TP()));
        record.setPrdcFrm(trimToNull(dto.getPRDC_FRM()));
        record.setIssMthd(trimToNull(dto.getISS_MTHD()));
        record.setRsMthd(trimToNull(dto.getRS_MTHD()));
        record.setRsCrrn(trimToNull(dto.getRS_CRRN()));
        record.setTrmTyp(trimToNull(dto.getTRM_TYP()));
        record.setPrdcClss(trimToNull(dto.getPRDC_CLSS()));
        record.setGrpUntNm(trimToNull(dto.getGRP_UNT_NM()));
        record.setPrdcAsstTyp(trimToNull(dto.getPRDC_ASST_TYP()));
        record.setPrdcMrktTyp(trimToNull(dto.getPRDC_MRKT_TYP()));
        record.setPftaFiprIdnt(trimToNull(dto.getPFTA_FIPR_IDNT()));
        record.setAccnNm(trimToNull(dto.getACCN_NM()));
        record.setSuborgNm(trimToNull(dto.getSUBORG_NM()));
        record.setDvlpTyp(trimToNull(dto.getDVLP_TYP()));
        record.setOpnClss(trimToNull(dto.getOPN_CLSS()));
        record.setPrdcBrnd(trimToNull(dto.getPRDC_BRND()));
        record.setPrdcPstn(trimToNull(dto.getPRDC_PSTN()));
        record.setPrdcBrnd2(trimToNull(dto.getPRDC_BRND_2()));
        record.setPrdcBrnd3(trimToNull(dto.getPRDC_BRND_3()));
        record.setRiskGrade(trimToNull(dto.getRISK_GRADE()));
        record.setShrTyp(trimToNull(dto.getSHR_TYP()));
        record.setIsPsnlPnsn(trimToNull(dto.getIS_PSNL_PNSN()));
        record.setIntrMthd(trimToNull(dto.getINTR_MTHD()));
        record.setPrdcThm(trimToNull(dto.getPRDC_THM()));
        record.setInvsMngrNm(trimToNull(dto.getINVS_MNGR_NM()));
        record.setPrdcInvsMngr(trimToNull(dto.getPRDC_INVS_MNGR()));
        record.setPrdcMngrNm(trimToNull(dto.getPRDC_MNGR_NM()));
        record.setExprDt(exprDt);
        record.setEndPrdExprDt(endPrdExprDt);
        record.setAcctDt(acctDt);
        return record;
    }

    /**
     * 将 ProductInfoDto 列表批量转换为持久化记录列表。
     *
     * @param dtoList 产品信息传输对象列表。
     * @param fallbackSnapshotDate 兜底快照分区日期。
     * @return 转换后的持久化记录列表。
     */
    public List<ProductInfoPersistenceRecord> toRecords(
            final List<ProductInfoDto> dtoList, final String fallbackSnapshotDate) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        final List<ProductInfoPersistenceRecord> records = new ArrayList<>(dtoList.size());
        for (final ProductInfoDto dto : dtoList) {
            final ProductInfoPersistenceRecord record = toRecord(dto, fallbackSnapshotDate);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    /**
     * 规范化并校验日期字符串为严格的 yyyy-MM-dd 格式。
     *
     * @param rawDate 原始日期文本（支持 yyyyMMdd 或 yyyy-MM-dd）。
     * @param required 是否必填。
     * @param fieldName 字段名称，用于异常提示。
     * @return 规范化后的 yyyy-MM-dd 日期字符串。
     * @throws IllegalArgumentException 当格式不合规或日历日期无效时抛出。
     */
    public static String normalizeDate(final String rawDate, final boolean required, final String fieldName) {
        final String trimmed = trimToNull(rawDate);
        if (trimmed == null) {
            if (required) {
                throw new IllegalArgumentException("字段 " + fieldName + " 不能为空");
            }
            return null;
        }

        final String standardDate = formatDateString(trimmed, rawDate, fieldName);
        validateCalendarDate(standardDate, rawDate, fieldName);
        return standardDate;
    }

    /**
     * 将符合格式要求的原始文本转换为连字符日期文本。
     *
     * @param trimmed 已去除空白的非空文本。
     * @param rawDate 原始输入文本。
     * @param fieldName 字段名称。
     * @return 连字符格式日期文本。
     */
    private static String formatDateString(final String trimmed, final String rawDate, final String fieldName) {
        if (trimmed.length() == 8 && trimmed.matches("\\d{8}")) {
            return trimmed.substring(0, 4) + "-" + trimmed.substring(4, 6) + "-" + trimmed.substring(6, 8);
        }
        if (trimmed.length() == 10 && trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return trimmed;
        }
        throw new IllegalArgumentException("字段 " + fieldName + " 日期格式非法: " + rawDate
                + "，仅支持 yyyy-MM-dd 或 yyyyMMdd");
    }

    /**
     * 校验日期文本是否符合真实日历日期。
     *
     * @param standardDate 连字符格式日期。
     * @param rawDate 原始输入文本。
     * @param fieldName 字段名称。
     */
    private static void validateCalendarDate(
            final String standardDate, final String rawDate, final String fieldName) {
        try {
            LocalDate.parse(standardDate, STRICT_DATE_FORMATTER);
        } catch (final DateTimeParseException exception) {
            throw new IllegalArgumentException("字段 " + fieldName + " 包含无效日历日期: " + rawDate, exception);
        }
    }

    /**
     * 去除前后空白，空字符串转为 null。
     *
     * @param value 原始字符串。
     * @return 清洗后的字符串。
     */
    private static String trimToNull(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 检查字符串是否具有非空白字符。
     *
     * @param value 待检查字符串。
     * @return 包含有效字符时返回 true。
     */
    private static boolean hasText(final String value) {
        return value != null && !value.trim().isEmpty();
    }
}
