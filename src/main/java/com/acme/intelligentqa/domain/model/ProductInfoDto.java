package com.acme.intelligentqa.domain.model;

import com.joyintech.datahub.subscribe.DhBaseDTO;
import java.io.Serializable;

/**
 * 产品信息数据传输对象，接收 DataHub 下发的产品信息。
 */
@SuppressWarnings("PMD.CyclomaticComplexity")
public class ProductInfoDto extends DhBaseDTO implements Serializable {

    /**
     * 序列化版本标识。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 产品代码。
     */
    private String PRDC_CD;
    /**
     * 母产品代码。
     */
    private String FML_PRDC_CD;
    /**
     * 母产品标识。
     */
    private String FML_PRDC_IDNT;
    /**
     * 母产品名称。
     */
    private String FTHR_PRDC_NM;
    /**
     * 产品名称。
     */
    private String PRDC_NM;
    /**
     * 产品简称。
     */
    private String PRDC_ABBR;
    /**
     * 产品全称。
     */
    private String PRDC_FLL_NM;
    /**
     * 系列代码。
     */
    private String CATENA_CD;
    /**
     * 系列名称。
     */
    private String CATENA_NM;
    /**
     * 开放类型名称。
     */
    private String OPN_TYP;
    /**
     * 产品类型名称。
     */
    private String PRDC_TYP;
    /**
     * 产品类别名称。
     */
    private String PRDT_TP;
    /**
     * 产品形态。
     */
    private String PRDC_FRM;
    /**
     * 发行方式名称。
     */
    private String ISS_MTHD;
    /**
     * 募集方式名称。
     */
    private String RS_MTHD;
    /**
     * 募集币种。
     */
    private String RS_CRRN;
    /**
     * 期限类型名称。
     */
    private String TRM_TYP;
    /**
     * 产品分类。
     */
    private String PRDC_CLSS;
    /**
     * 投组单元名称。
     */
    private String GRP_UNT_NM;
    /**
     * 产品资产类型名称。
     */
    private String PRDC_ASST_TYP;
    /**
     * 产品市场类型名称。
     */
    private String PRDC_MRKT_TYP;
    /**
     * 养老理财产品标识。
     */
    private String PFTA_FIPR_IDNT;
    /**
     * 账户名称。
     */
    private String ACCN_NM;
    /**
     * 所属机构名称。
     */
    private String SUBORG_NM;
    /**
     * 开发类型名称。
     */
    private String DVLP_TYP;
    /**
     * 开放类别名称。
     */
    private String OPN_CLSS;
    /**
     * 产品品牌。
     */
    private String PRDC_BRND;
    /**
     * 策略标签。
     */
    private String PRDC_PSTN;
    /**
     * 产品品牌二类。
     */
    private String PRDC_BRND_2;
    /**
     * 产品品牌三类。
     */
    private String PRDC_BRND_3;
    /**
     * 产品风险等级。
     */
    private String RISK_GRADE;
    /**
     * 份额类型名称。
     */
    private String SHR_TYP;
    /**
     * 是否个人养老金产品。
     */
    private String IS_PSNL_PNSN;
    /**
     * 计息方式名称。
     */
    private String INTR_MTHD;
    /**
     * 产品主题。
     */
    private String PRDC_THM;
    /**
     * 投资经理姓名(监管口径)。
     */
    private String INVS_MNGR_NM;
    /**
     * 投资经理名称(产品部口径)。
     */
    private String PRDC_INVS_MNGR;
    /**
     * 产品经理名称。
     */
    private String PRDC_MNGR_NM;
    /**
     * 产品到期日期。
     */
    private String EXPR_DT;
    /**
     * 定开基准日期。
     */
    private String END_PRD_EXPR_DT;
    /**
     * 分区日期。
     */
    private String ACCT_DT;

    /**
     * 构造产品信息数据传输对象。
     */
    public ProductInfoDto() {
    }

    /**
     * 获取产品代码。
     *
     * @return 产品代码。
     */
    public String getPRDC_CD() {
        return PRDC_CD;
    }

    /**
     * 设置产品代码。
     *
     * @param PRDC_CD 产品代码。
     */
    public void setPRDC_CD(final String PRDC_CD) {
        this.PRDC_CD = PRDC_CD;
    }

    /**
     * 获取母产品代码。
     *
     * @return 母产品代码。
     */
    public String getFML_PRDC_CD() {
        return FML_PRDC_CD;
    }

    /**
     * 设置母产品代码。
     *
     * @param FML_PRDC_CD 母产品代码。
     */
    public void setFML_PRDC_CD(final String FML_PRDC_CD) {
        this.FML_PRDC_CD = FML_PRDC_CD;
    }

    /**
     * 获取母产品标识。
     *
     * @return 母产品标识。
     */
    public String getFML_PRDC_IDNT() {
        return FML_PRDC_IDNT;
    }

    /**
     * 设置母产品标识。
     *
     * @param FML_PRDC_IDNT 母产品标识。
     */
    public void setFML_PRDC_IDNT(final String FML_PRDC_IDNT) {
        this.FML_PRDC_IDNT = FML_PRDC_IDNT;
    }

    /**
     * 获取母产品名称。
     *
     * @return 母产品名称。
     */
    public String getFTHR_PRDC_NM() {
        return FTHR_PRDC_NM;
    }

    /**
     * 设置母产品名称。
     *
     * @param FTHR_PRDC_NM 母产品名称。
     */
    public void setFTHR_PRDC_NM(final String FTHR_PRDC_NM) {
        this.FTHR_PRDC_NM = FTHR_PRDC_NM;
    }

    /**
     * 获取产品名称。
     *
     * @return 产品名称。
     */
    public String getPRDC_NM() {
        return PRDC_NM;
    }

    /**
     * 设置产品名称。
     *
     * @param PRDC_NM 产品名称。
     */
    public void setPRDC_NM(final String PRDC_NM) {
        this.PRDC_NM = PRDC_NM;
    }

    /**
     * 获取产品简称。
     *
     * @return 产品简称。
     */
    public String getPRDC_ABBR() {
        return PRDC_ABBR;
    }

    /**
     * 设置产品简称。
     *
     * @param PRDC_ABBR 产品简称。
     */
    public void setPRDC_ABBR(final String PRDC_ABBR) {
        this.PRDC_ABBR = PRDC_ABBR;
    }

    /**
     * 获取产品全称。
     *
     * @return 产品全称。
     */
    public String getPRDC_FLL_NM() {
        return PRDC_FLL_NM;
    }

    /**
     * 设置产品全称。
     *
     * @param PRDC_FLL_NM 产品全称。
     */
    public void setPRDC_FLL_NM(final String PRDC_FLL_NM) {
        this.PRDC_FLL_NM = PRDC_FLL_NM;
    }

    /**
     * 获取系列代码。
     *
     * @return 系列代码。
     */
    public String getCATENA_CD() {
        return CATENA_CD;
    }

    /**
     * 设置系列代码。
     *
     * @param CATENA_CD 系列代码。
     */
    public void setCATENA_CD(final String CATENA_CD) {
        this.CATENA_CD = CATENA_CD;
    }

    /**
     * 获取系列名称。
     *
     * @return 系列名称。
     */
    public String getCATENA_NM() {
        return CATENA_NM;
    }

    /**
     * 设置系列名称。
     *
     * @param CATENA_NM 系列名称。
     */
    public void setCATENA_NM(final String CATENA_NM) {
        this.CATENA_NM = CATENA_NM;
    }

    /**
     * 获取开放类型名称。
     *
     * @return 开放类型名称。
     */
    public String getOPN_TYP() {
        return OPN_TYP;
    }

    /**
     * 设置开放类型名称。
     *
     * @param OPN_TYP 开放类型名称。
     */
    public void setOPN_TYP(final String OPN_TYP) {
        this.OPN_TYP = OPN_TYP;
    }

    /**
     * 获取产品类型名称。
     *
     * @return 产品类型名称。
     */
    public String getPRDC_TYP() {
        return PRDC_TYP;
    }

    /**
     * 设置产品类型名称。
     *
     * @param PRDC_TYP 产品类型名称。
     */
    public void setPRDC_TYP(final String PRDC_TYP) {
        this.PRDC_TYP = PRDC_TYP;
    }

    /**
     * 获取产品类别名称。
     *
     * @return 产品类别名称。
     */
    public String getPRDT_TP() {
        return PRDT_TP;
    }

    /**
     * 设置产品类别名称。
     *
     * @param PRDT_TP 产品类别名称。
     */
    public void setPRDT_TP(final String PRDT_TP) {
        this.PRDT_TP = PRDT_TP;
    }

    /**
     * 获取产品形态。
     *
     * @return 产品形态。
     */
    public String getPRDC_FRM() {
        return PRDC_FRM;
    }

    /**
     * 设置产品形态。
     *
     * @param PRDC_FRM 产品形态。
     */
    public void setPRDC_FRM(final String PRDC_FRM) {
        this.PRDC_FRM = PRDC_FRM;
    }

    /**
     * 获取发行方式名称。
     *
     * @return 发行方式名称。
     */
    public String getISS_MTHD() {
        return ISS_MTHD;
    }

    /**
     * 设置发行方式名称。
     *
     * @param ISS_MTHD 发行方式名称。
     */
    public void setISS_MTHD(final String ISS_MTHD) {
        this.ISS_MTHD = ISS_MTHD;
    }

    /**
     * 获取募集方式名称。
     *
     * @return 募集方式名称。
     */
    public String getRS_MTHD() {
        return RS_MTHD;
    }

    /**
     * 设置募集方式名称。
     *
     * @param RS_MTHD 募集方式名称。
     */
    public void setRS_MTHD(final String RS_MTHD) {
        this.RS_MTHD = RS_MTHD;
    }

    /**
     * 获取募集币种。
     *
     * @return 募集币种。
     */
    public String getRS_CRRN() {
        return RS_CRRN;
    }

    /**
     * 设置募集币种。
     *
     * @param RS_CRRN 募集币种。
     */
    public void setRS_CRRN(final String RS_CRRN) {
        this.RS_CRRN = RS_CRRN;
    }

    /**
     * 获取期限类型名称。
     *
     * @return 期限类型名称。
     */
    public String getTRM_TYP() {
        return TRM_TYP;
    }

    /**
     * 设置期限类型名称。
     *
     * @param TRM_TYP 期限类型名称。
     */
    public void setTRM_TYP(final String TRM_TYP) {
        this.TRM_TYP = TRM_TYP;
    }

    /**
     * 获取产品分类。
     *
     * @return 产品分类。
     */
    public String getPRDC_CLSS() {
        return PRDC_CLSS;
    }

    /**
     * 设置产品分类。
     *
     * @param PRDC_CLSS 产品分类。
     */
    public void setPRDC_CLSS(final String PRDC_CLSS) {
        this.PRDC_CLSS = PRDC_CLSS;
    }

    /**
     * 获取投组单元名称。
     *
     * @return 投组单元名称。
     */
    public String getGRP_UNT_NM() {
        return GRP_UNT_NM;
    }

    /**
     * 设置投组单元名称。
     *
     * @param GRP_UNT_NM 投组单元名称。
     */
    public void setGRP_UNT_NM(final String GRP_UNT_NM) {
        this.GRP_UNT_NM = GRP_UNT_NM;
    }

    /**
     * 获取产品资产类型名称。
     *
     * @return 产品资产类型名称。
     */
    public String getPRDC_ASST_TYP() {
        return PRDC_ASST_TYP;
    }

    /**
     * 设置产品资产类型名称。
     *
     * @param PRDC_ASST_TYP 产品资产类型名称。
     */
    public void setPRDC_ASST_TYP(final String PRDC_ASST_TYP) {
        this.PRDC_ASST_TYP = PRDC_ASST_TYP;
    }

    /**
     * 获取产品市场类型名称。
     *
     * @return 产品市场类型名称。
     */
    public String getPRDC_MRKT_TYP() {
        return PRDC_MRKT_TYP;
    }

    /**
     * 设置产品市场类型名称。
     *
     * @param PRDC_MRKT_TYP 产品市场类型名称。
     */
    public void setPRDC_MRKT_TYP(final String PRDC_MRKT_TYP) {
        this.PRDC_MRKT_TYP = PRDC_MRKT_TYP;
    }

    /**
     * 获取养老理财产品标识。
     *
     * @return 养老理财产品标识。
     */
    public String getPFTA_FIPR_IDNT() {
        return PFTA_FIPR_IDNT;
    }

    /**
     * 设置养老理财产品标识。
     *
     * @param PFTA_FIPR_IDNT 养老理财产品标识。
     */
    public void setPFTA_FIPR_IDNT(final String PFTA_FIPR_IDNT) {
        this.PFTA_FIPR_IDNT = PFTA_FIPR_IDNT;
    }

    /**
     * 获取账户名称。
     *
     * @return 账户名称。
     */
    public String getACCN_NM() {
        return ACCN_NM;
    }

    /**
     * 设置账户名称。
     *
     * @param ACCN_NM 账户名称。
     */
    public void setACCN_NM(final String ACCN_NM) {
        this.ACCN_NM = ACCN_NM;
    }

    /**
     * 获取所属机构名称。
     *
     * @return 所属机构名称。
     */
    public String getSUBORG_NM() {
        return SUBORG_NM;
    }

    /**
     * 设置所属机构名称。
     *
     * @param SUBORG_NM 所属机构名称。
     */
    public void setSUBORG_NM(final String SUBORG_NM) {
        this.SUBORG_NM = SUBORG_NM;
    }

    /**
     * 获取开发类型名称。
     *
     * @return 开发类型名称。
     */
    public String getDVLP_TYP() {
        return DVLP_TYP;
    }

    /**
     * 设置开发类型名称。
     *
     * @param DVLP_TYP 开发类型名称。
     */
    public void setDVLP_TYP(final String DVLP_TYP) {
        this.DVLP_TYP = DVLP_TYP;
    }

    /**
     * 获取开放类别名称。
     *
     * @return 开放类别名称。
     */
    public String getOPN_CLSS() {
        return OPN_CLSS;
    }

    /**
     * 设置开放类别名称。
     *
     * @param OPN_CLSS 开放类别名称。
     */
    public void setOPN_CLSS(final String OPN_CLSS) {
        this.OPN_CLSS = OPN_CLSS;
    }

    /**
     * 获取产品品牌。
     *
     * @return 产品品牌。
     */
    public String getPRDC_BRND() {
        return PRDC_BRND;
    }

    /**
     * 设置产品品牌。
     *
     * @param PRDC_BRND 产品品牌。
     */
    public void setPRDC_BRND(final String PRDC_BRND) {
        this.PRDC_BRND = PRDC_BRND;
    }

    /**
     * 获取策略标签。
     *
     * @return 策略标签。
     */
    public String getPRDC_PSTN() {
        return PRDC_PSTN;
    }

    /**
     * 设置策略标签。
     *
     * @param PRDC_PSTN 策略标签。
     */
    public void setPRDC_PSTN(final String PRDC_PSTN) {
        this.PRDC_PSTN = PRDC_PSTN;
    }

    /**
     * 获取产品品牌二类。
     *
     * @return 产品品牌二类。
     */
    public String getPRDC_BRND_2() {
        return PRDC_BRND_2;
    }

    /**
     * 设置产品品牌二类。
     *
     * @param PRDC_BRND_2 产品品牌二类。
     */
    public void setPRDC_BRND_2(final String PRDC_BRND_2) {
        this.PRDC_BRND_2 = PRDC_BRND_2;
    }

    /**
     * 获取产品品牌三类。
     *
     * @return 产品品牌三类。
     */
    public String getPRDC_BRND_3() {
        return PRDC_BRND_3;
    }

    /**
     * 设置产品品牌三类。
     *
     * @param PRDC_BRND_3 产品品牌三类。
     */
    public void setPRDC_BRND_3(final String PRDC_BRND_3) {
        this.PRDC_BRND_3 = PRDC_BRND_3;
    }

    /**
     * 获取产品风险等级。
     *
     * @return 产品风险等级。
     */
    public String getRISK_GRADE() {
        return RISK_GRADE;
    }

    /**
     * 设置产品风险等级。
     *
     * @param RISK_GRADE 产品风险等级。
     */
    public void setRISK_GRADE(final String RISK_GRADE) {
        this.RISK_GRADE = RISK_GRADE;
    }

    /**
     * 获取份额类型名称。
     *
     * @return 份额类型名称。
     */
    public String getSHR_TYP() {
        return SHR_TYP;
    }

    /**
     * 设置份额类型名称。
     *
     * @param SHR_TYP 份额类型名称。
     */
    public void setSHR_TYP(final String SHR_TYP) {
        this.SHR_TYP = SHR_TYP;
    }

    /**
     * 获取是否个人养老金产品。
     *
     * @return 是否个人养老金产品。
     */
    public String getIS_PSNL_PNSN() {
        return IS_PSNL_PNSN;
    }

    /**
     * 设置是否个人养老金产品。
     *
     * @param IS_PSNL_PNSN 是否个人养老金产品。
     */
    public void setIS_PSNL_PNSN(final String IS_PSNL_PNSN) {
        this.IS_PSNL_PNSN = IS_PSNL_PNSN;
    }

    /**
     * 获取计息方式名称。
     *
     * @return 计息方式名称。
     */
    public String getINTR_MTHD() {
        return INTR_MTHD;
    }

    /**
     * 设置计息方式名称。
     *
     * @param INTR_MTHD 计息方式名称。
     */
    public void setINTR_MTHD(final String INTR_MTHD) {
        this.INTR_MTHD = INTR_MTHD;
    }

    /**
     * 获取产品主题。
     *
     * @return 产品主题。
     */
    public String getPRDC_THM() {
        return PRDC_THM;
    }

    /**
     * 设置产品主题。
     *
     * @param PRDC_THM 产品主题。
     */
    public void setPRDC_THM(final String PRDC_THM) {
        this.PRDC_THM = PRDC_THM;
    }

    /**
     * 获取投资经理姓名(监管口径)。
     *
     * @return 投资经理姓名(监管口径)。
     */
    public String getINVS_MNGR_NM() {
        return INVS_MNGR_NM;
    }

    /**
     * 设置投资经理姓名(监管口径)。
     *
     * @param INVS_MNGR_NM 投资经理姓名(监管口径)。
     */
    public void setINVS_MNGR_NM(final String INVS_MNGR_NM) {
        this.INVS_MNGR_NM = INVS_MNGR_NM;
    }

    /**
     * 获取投资经理名称(产品部口径)。
     *
     * @return 投资经理名称(产品部口径)。
     */
    public String getPRDC_INVS_MNGR() {
        return PRDC_INVS_MNGR;
    }

    /**
     * 设置投资经理名称(产品部口径)。
     *
     * @param PRDC_INVS_MNGR 投资经理名称(产品部口径)。
     */
    public void setPRDC_INVS_MNGR(final String PRDC_INVS_MNGR) {
        this.PRDC_INVS_MNGR = PRDC_INVS_MNGR;
    }

    /**
     * 获取产品经理名称。
     *
     * @return 产品经理名称。
     */
    public String getPRDC_MNGR_NM() {
        return PRDC_MNGR_NM;
    }

    /**
     * 设置产品经理名称。
     *
     * @param PRDC_MNGR_NM 产品经理名称。
     */
    public void setPRDC_MNGR_NM(final String PRDC_MNGR_NM) {
        this.PRDC_MNGR_NM = PRDC_MNGR_NM;
    }

    /**
     * 获取产品到期日期。
     *
     * @return 产品到期日期。
     */
    public String getEXPR_DT() {
        return EXPR_DT;
    }

    /**
     * 设置产品到期日期。
     *
     * @param EXPR_DT 产品到期日期。
     */
    public void setEXPR_DT(final String EXPR_DT) {
        this.EXPR_DT = EXPR_DT;
    }

    /**
     * 获取定开基准日期。
     *
     * @return 定开基准日期。
     */
    public String getEND_PRD_EXPR_DT() {
        return END_PRD_EXPR_DT;
    }

    /**
     * 设置定开基准日期。
     *
     * @param END_PRD_EXPR_DT 定开基准日期。
     */
    public void setEND_PRD_EXPR_DT(final String END_PRD_EXPR_DT) {
        this.END_PRD_EXPR_DT = END_PRD_EXPR_DT;
    }

    /**
     * 获取分区日期。
     *
     * @return 分区日期。
     */
    public String getACCT_DT() {
        return ACCT_DT;
    }

    /**
     * 设置分区日期。
     *
     * @param ACCT_DT 分区日期。
     */
    public void setACCT_DT(final String ACCT_DT) {
        this.ACCT_DT = ACCT_DT;
    }

    /**
     * 获取字段映射规则。
     *
     * @return 字段映射数组。
     */
    @Override
    public FieldMapping[] fieldMappings() {
        return Mappings.create()
            .map(0, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_CD(value)))
            .map(1, ((obj, value) -> ((ProductInfoDto) obj).setFML_PRDC_CD(value)))
            .map(2, ((obj, value) -> ((ProductInfoDto) obj).setFML_PRDC_IDNT(value)))
            .map(3, ((obj, value) -> ((ProductInfoDto) obj).setFTHR_PRDC_NM(value)))
            .map(4, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_NM(value)))
            .map(5, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_ABBR(value)))
            .map(6, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_FLL_NM(value)))
            .map(7, ((obj, value) -> ((ProductInfoDto) obj).setCATENA_CD(value)))
            .map(8, ((obj, value) -> ((ProductInfoDto) obj).setCATENA_NM(value)))
            .map(9, ((obj, value) -> ((ProductInfoDto) obj).setOPN_TYP(value)))
            .map(10, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_TYP(value)))
            .map(11, ((obj, value) -> ((ProductInfoDto) obj).setPRDT_TP(value)))
            .map(12, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_FRM(value)))
            .map(13, ((obj, value) -> ((ProductInfoDto) obj).setISS_MTHD(value)))
            .map(14, ((obj, value) -> ((ProductInfoDto) obj).setRS_MTHD(value)))
            .map(15, ((obj, value) -> ((ProductInfoDto) obj).setRS_CRRN(value)))
            .map(16, ((obj, value) -> ((ProductInfoDto) obj).setTRM_TYP(value)))
            .map(17, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_CLSS(value)))
            .map(18, ((obj, value) -> ((ProductInfoDto) obj).setGRP_UNT_NM(value)))
            .map(19, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_ASST_TYP(value)))
            .map(20, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_MRKT_TYP(value)))
            .map(21, ((obj, value) -> ((ProductInfoDto) obj).setPFTA_FIPR_IDNT(value)))
            .map(22, ((obj, value) -> ((ProductInfoDto) obj).setACCN_NM(value)))
            .map(23, ((obj, value) -> ((ProductInfoDto) obj).setSUBORG_NM(value)))
            .map(24, ((obj, value) -> ((ProductInfoDto) obj).setDVLP_TYP(value)))
            .map(25, ((obj, value) -> ((ProductInfoDto) obj).setOPN_CLSS(value)))
            .map(26, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_BRND(value)))
            .map(27, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_PSTN(value)))
            .map(28, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_BRND_2(value)))
            .map(29, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_BRND_3(value)))
            .map(30, ((obj, value) -> ((ProductInfoDto) obj).setRISK_GRADE(value)))
            .map(31, ((obj, value) -> ((ProductInfoDto) obj).setSHR_TYP(value)))
            .map(32, ((obj, value) -> ((ProductInfoDto) obj).setIS_PSNL_PNSN(value)))
            .map(33, ((obj, value) -> ((ProductInfoDto) obj).setINTR_MTHD(value)))
            .map(34, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_THM(value)))
            .map(35, ((obj, value) -> ((ProductInfoDto) obj).setINVS_MNGR_NM(value)))
            .map(36, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_INVS_MNGR(value)))
            .map(37, ((obj, value) -> ((ProductInfoDto) obj).setPRDC_MNGR_NM(value)))
            .map(38, ((obj, value) -> ((ProductInfoDto) obj).setEXPR_DT(value)))
            .map(39, ((obj, value) -> ((ProductInfoDto) obj).setEND_PRD_EXPR_DT(value)))
            .map(40, ((obj, value) -> ((ProductInfoDto) obj).setACCT_DT(value)))
            .toArray();
    }
}
