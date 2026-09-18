package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 产品信息快照持久化记录，映射 {@code dws_product_info_d} 物理表。
 */
@SuppressWarnings("PMD.CyclomaticComplexity")
@TableName("dws_product_info_d")
public class ProductInfoPersistenceRecord {

    /** 数据库内部自增主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 产品代码。 */
    @TableField("PRDC_CD")
    private String prdcCd;

    /** 母产品代码。 */
    @TableField("FML_PRDC_CD")
    private String fmlPrdcCd;

    /** 母产品标识。 */
    @TableField("FML_PRDC_IDNT")
    private String fmlPrdcIdnt;

    /** 母产品名称。 */
    @TableField("FTHR_PRDC_NM")
    private String fthrPrdcNm;

    /** 产品名称。 */
    @TableField("PRDC_NM")
    private String prdcNm;

    /** 产品简称。 */
    @TableField("PRDC_ABBR")
    private String prdcAbbr;

    /** 产品全称。 */
    @TableField("PRDC_FLL_NM")
    private String prdcFllNm;

    /** 系列代码。 */
    @TableField("CATENA_CD")
    private String catenaCd;

    /** 系列名称。 */
    @TableField("CATENA_NM")
    private String catenaNm;

    /** 开放类型名称。 */
    @TableField("OPN_TYP")
    private String opnTyp;

    /** 产品类型名称。 */
    @TableField("PRDC_TYP")
    private String prdcTyp;

    /** 产品类别名称。 */
    @TableField("PRDT_TP")
    private String prdtTp;

    /** 产品形态。 */
    @TableField("PRDC_FRM")
    private String prdcFrm;

    /** 发行方式名称。 */
    @TableField("ISS_MTHD")
    private String issMthd;

    /** 募集方式名称。 */
    @TableField("RS_MTHD")
    private String rsMthd;

    /** 募集币种。 */
    @TableField("RS_CRRN")
    private String rsCrrn;

    /** 期限类型名称。 */
    @TableField("TRM_TYP")
    private String trmTyp;

    /** 产品分类。 */
    @TableField("PRDC_CLSS")
    private String prdcClss;

    /** 投组单元名称。 */
    @TableField("GRP_UNT_NM")
    private String grpUntNm;

    /** 产品资产类型名称。 */
    @TableField("PRDC_ASST_TYP")
    private String prdcAsstTyp;

    /** 产品市场类型名称。 */
    @TableField("PRDC_MRKT_TYP")
    private String prdcMrktTyp;

    /** 养老理财产品标识。 */
    @TableField("PFTA_FIPR_IDNT")
    private String pftaFiprIdnt;

    /** 账户名称。 */
    @TableField("ACCN_NM")
    private String accnNm;

    /** 所属机构名称。 */
    @TableField("SUBORG_NM")
    private String suborgNm;

    /** 开发类型名称。 */
    @TableField("DVLP_TYP")
    private String dvlpTyp;

    /** 开放类别名称。 */
    @TableField("OPN_CLSS")
    private String opnClss;

    /** 产品品牌。 */
    @TableField("PRDC_BRND")
    private String prdcBrnd;

    /** 策略标签。 */
    @TableField("PRDC_PSTN")
    private String prdcPstn;

    /** 产品品牌二类。 */
    @TableField("PRDC_BRND_2")
    private String prdcBrnd2;

    /** 产品品牌三类。 */
    @TableField("PRDC_BRND_3")
    private String prdcBrnd3;

    /** 产品风险等级。 */
    @TableField("RISK_GRADE")
    private String riskGrade;

    /** 份额类型名称。 */
    @TableField("SHR_TYP")
    private String shrTyp;

    /** 是否个人养老金产品。 */
    @TableField("IS_PSNL_PNSN")
    private String isPsnlPnsn;

    /** 计息方式名称。 */
    @TableField("INTR_MTHD")
    private String intrMthd;

    /** 产品主题。 */
    @TableField("PRDC_THM")
    private String prdcThm;

    /** 投资经理姓名(监管口径)。 */
    @TableField("INVS_MNGR_NM")
    private String invsMngrNm;

    /** 投资经理名称(产品部口径)。 */
    @TableField("PRDC_INVS_MNGR")
    private String prdcInvsMngr;

    /** 产品经理名称。 */
    @TableField("PRDC_MNGR_NM")
    private String prdcMngrNm;

    /** 产品到期日期。 */
    @TableField("EXPR_DT")
    private String exprDt;

    /** 定开基准日期。 */
    @TableField("END_PRD_EXPR_DT")
    private String endPrdExprDt;

    /** 每日全量快照分区日期。 */
    @TableField("ACCT_DT")
    private String acctDt;

    /**
     * 构造产品信息快照持久化记录。
     */
    public ProductInfoPersistenceRecord() {
    }

    /**
     * 获取数据库内部自增主键。
     *
     * @return 数据库内部自增主键。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置数据库内部自增主键。
     *
     * @param id 数据库内部自增主键。
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 获取产品代码。
     *
     * @return 产品代码。
     */
    public String getPrdcCd() {
        return prdcCd;
    }

    /**
     * 设置产品代码。
     *
     * @param prdcCd 产品代码。
     */
    public void setPrdcCd(final String prdcCd) {
        this.prdcCd = prdcCd;
    }

    /**
     * 获取母产品代码。
     *
     * @return 母产品代码。
     */
    public String getFmlPrdcCd() {
        return fmlPrdcCd;
    }

    /**
     * 设置母产品代码。
     *
     * @param fmlPrdcCd 母产品代码。
     */
    public void setFmlPrdcCd(final String fmlPrdcCd) {
        this.fmlPrdcCd = fmlPrdcCd;
    }

    /**
     * 获取母产品标识。
     *
     * @return 母产品标识。
     */
    public String getFmlPrdcIdnt() {
        return fmlPrdcIdnt;
    }

    /**
     * 设置母产品标识。
     *
     * @param fmlPrdcIdnt 母产品标识。
     */
    public void setFmlPrdcIdnt(final String fmlPrdcIdnt) {
        this.fmlPrdcIdnt = fmlPrdcIdnt;
    }

    /**
     * 获取母产品名称。
     *
     * @return 母产品名称。
     */
    public String getFthrPrdcNm() {
        return fthrPrdcNm;
    }

    /**
     * 设置母产品名称。
     *
     * @param fthrPrdcNm 母产品名称。
     */
    public void setFthrPrdcNm(final String fthrPrdcNm) {
        this.fthrPrdcNm = fthrPrdcNm;
    }

    /**
     * 获取产品名称。
     *
     * @return 产品名称。
     */
    public String getPrdcNm() {
        return prdcNm;
    }

    /**
     * 设置产品名称。
     *
     * @param prdcNm 产品名称。
     */
    public void setPrdcNm(final String prdcNm) {
        this.prdcNm = prdcNm;
    }

    /**
     * 获取产品简称。
     *
     * @return 产品简称。
     */
    public String getPrdcAbbr() {
        return prdcAbbr;
    }

    /**
     * 设置产品简称。
     *
     * @param prdcAbbr 产品简称。
     */
    public void setPrdcAbbr(final String prdcAbbr) {
        this.prdcAbbr = prdcAbbr;
    }

    /**
     * 获取产品全称。
     *
     * @return 产品全称。
     */
    public String getPrdcFllNm() {
        return prdcFllNm;
    }

    /**
     * 设置产品全称。
     *
     * @param prdcFllNm 产品全称。
     */
    public void setPrdcFllNm(final String prdcFllNm) {
        this.prdcFllNm = prdcFllNm;
    }

    /**
     * 获取系列代码。
     *
     * @return 系列代码。
     */
    public String getCatenaCd() {
        return catenaCd;
    }

    /**
     * 设置系列代码。
     *
     * @param catenaCd 系列代码。
     */
    public void setCatenaCd(final String catenaCd) {
        this.catenaCd = catenaCd;
    }

    /**
     * 获取系列名称。
     *
     * @return 系列名称。
     */
    public String getCatenaNm() {
        return catenaNm;
    }

    /**
     * 设置系列名称。
     *
     * @param catenaNm 系列名称。
     */
    public void setCatenaNm(final String catenaNm) {
        this.catenaNm = catenaNm;
    }

    /**
     * 获取开放类型名称。
     *
     * @return 开放类型名称。
     */
    public String getOpnTyp() {
        return opnTyp;
    }

    /**
     * 设置开放类型名称。
     *
     * @param opnTyp 开放类型名称。
     */
    public void setOpnTyp(final String opnTyp) {
        this.opnTyp = opnTyp;
    }

    /**
     * 获取产品类型名称。
     *
     * @return 产品类型名称。
     */
    public String getPrdcTyp() {
        return prdcTyp;
    }

    /**
     * 设置产品类型名称。
     *
     * @param prdcTyp 产品类型名称。
     */
    public void setPrdcTyp(final String prdcTyp) {
        this.prdcTyp = prdcTyp;
    }

    /**
     * 获取产品类别名称。
     *
     * @return 产品类别名称。
     */
    public String getPrdtTp() {
        return prdtTp;
    }

    /**
     * 设置产品类别名称。
     *
     * @param prdtTp 产品类别名称。
     */
    public void setPrdtTp(final String prdtTp) {
        this.prdtTp = prdtTp;
    }

    /**
     * 获取产品形态。
     *
     * @return 产品形态。
     */
    public String getPrdcFrm() {
        return prdcFrm;
    }

    /**
     * 设置产品形态。
     *
     * @param prdcFrm 产品形态。
     */
    public void setPrdcFrm(final String prdcFrm) {
        this.prdcFrm = prdcFrm;
    }

    /**
     * 获取发行方式名称。
     *
     * @return 发行方式名称。
     */
    public String getIssMthd() {
        return issMthd;
    }

    /**
     * 设置发行方式名称。
     *
     * @param issMthd 发行方式名称。
     */
    public void setIssMthd(final String issMthd) {
        this.issMthd = issMthd;
    }

    /**
     * 获取募集方式名称。
     *
     * @return 募集方式名称。
     */
    public String getRsMthd() {
        return rsMthd;
    }

    /**
     * 设置募集方式名称。
     *
     * @param rsMthd 募集方式名称。
     */
    public void setRsMthd(final String rsMthd) {
        this.rsMthd = rsMthd;
    }

    /**
     * 获取募集币种。
     *
     * @return 募集币种。
     */
    public String getRsCrrn() {
        return rsCrrn;
    }

    /**
     * 设置募集币种。
     *
     * @param rsCrrn 募集币种。
     */
    public void setRsCrrn(final String rsCrrn) {
        this.rsCrrn = rsCrrn;
    }

    /**
     * 获取期限类型名称。
     *
     * @return 期限类型名称。
     */
    public String getTrmTyp() {
        return trmTyp;
    }

    /**
     * 设置期限类型名称。
     *
     * @param trmTyp 期限类型名称。
     */
    public void setTrmTyp(final String trmTyp) {
        this.trmTyp = trmTyp;
    }

    /**
     * 获取产品分类。
     *
     * @return 产品分类。
     */
    public String getPrdcClss() {
        return prdcClss;
    }

    /**
     * 设置产品分类。
     *
     * @param prdcClss 产品分类。
     */
    public void setPrdcClss(final String prdcClss) {
        this.prdcClss = prdcClss;
    }

    /**
     * 获取投组单元名称。
     *
     * @return 投组单元名称。
     */
    public String getGrpUntNm() {
        return grpUntNm;
    }

    /**
     * 设置投组单元名称。
     *
     * @param grpUntNm 投组单元名称。
     */
    public void setGrpUntNm(final String grpUntNm) {
        this.grpUntNm = grpUntNm;
    }

    /**
     * 获取产品资产类型名称。
     *
     * @return 产品资产类型名称。
     */
    public String getPrdcAsstTyp() {
        return prdcAsstTyp;
    }

    /**
     * 设置产品资产类型名称。
     *
     * @param prdcAsstTyp 产品资产类型名称。
     */
    public void setPrdcAsstTyp(final String prdcAsstTyp) {
        this.prdcAsstTyp = prdcAsstTyp;
    }

    /**
     * 获取产品市场类型名称。
     *
     * @return 产品市场类型名称。
     */
    public String getPrdcMrktTyp() {
        return prdcMrktTyp;
    }

    /**
     * 设置产品市场类型名称。
     *
     * @param prdcMrktTyp 产品市场类型名称。
     */
    public void setPrdcMrktTyp(final String prdcMrktTyp) {
        this.prdcMrktTyp = prdcMrktTyp;
    }

    /**
     * 获取养老理财产品标识。
     *
     * @return 养老理财产品标识。
     */
    public String getPftaFiprIdnt() {
        return pftaFiprIdnt;
    }

    /**
     * 设置养老理财产品标识。
     *
     * @param pftaFiprIdnt 养老理财产品标识。
     */
    public void setPftaFiprIdnt(final String pftaFiprIdnt) {
        this.pftaFiprIdnt = pftaFiprIdnt;
    }

    /**
     * 获取账户名称。
     *
     * @return 账户名称。
     */
    public String getAccnNm() {
        return accnNm;
    }

    /**
     * 设置账户名称。
     *
     * @param accnNm 账户名称。
     */
    public void setAccnNm(final String accnNm) {
        this.accnNm = accnNm;
    }

    /**
     * 获取所属机构名称。
     *
     * @return 所属机构名称。
     */
    public String getSuborgNm() {
        return suborgNm;
    }

    /**
     * 设置所属机构名称。
     *
     * @param suborgNm 所属机构名称。
     */
    public void setSuborgNm(final String suborgNm) {
        this.suborgNm = suborgNm;
    }

    /**
     * 获取开发类型名称。
     *
     * @return 开发类型名称。
     */
    public String getDvlpTyp() {
        return dvlpTyp;
    }

    /**
     * 设置开发类型名称。
     *
     * @param dvlpTyp 开发类型名称。
     */
    public void setDvlpTyp(final String dvlpTyp) {
        this.dvlpTyp = dvlpTyp;
    }

    /**
     * 获取开放类别名称。
     *
     * @return 开放类别名称。
     */
    public String getOpnClss() {
        return opnClss;
    }

    /**
     * 设置开放类别名称。
     *
     * @param opnClss 开放类别名称。
     */
    public void setOpnClss(final String opnClss) {
        this.opnClss = opnClss;
    }

    /**
     * 获取产品品牌。
     *
     * @return 产品品牌。
     */
    public String getPrdcBrnd() {
        return prdcBrnd;
    }

    /**
     * 设置产品品牌。
     *
     * @param prdcBrnd 产品品牌。
     */
    public void setPrdcBrnd(final String prdcBrnd) {
        this.prdcBrnd = prdcBrnd;
    }

    /**
     * 获取策略标签。
     *
     * @return 策略标签。
     */
    public String getPrdcPstn() {
        return prdcPstn;
    }

    /**
     * 设置策略标签。
     *
     * @param prdcPstn 策略标签。
     */
    public void setPrdcPstn(final String prdcPstn) {
        this.prdcPstn = prdcPstn;
    }

    /**
     * 获取产品品牌二类。
     *
     * @return 产品品牌二类。
     */
    public String getPrdcBrnd2() {
        return prdcBrnd2;
    }

    /**
     * 设置产品品牌二类。
     *
     * @param prdcBrnd2 产品品牌二类。
     */
    public void setPrdcBrnd2(final String prdcBrnd2) {
        this.prdcBrnd2 = prdcBrnd2;
    }

    /**
     * 获取产品品牌三类。
     *
     * @return 产品品牌三类。
     */
    public String getPrdcBrnd3() {
        return prdcBrnd3;
    }

    /**
     * 设置产品品牌三类。
     *
     * @param prdcBrnd3 产品品牌三类。
     */
    public void setPrdcBrnd3(final String prdcBrnd3) {
        this.prdcBrnd3 = prdcBrnd3;
    }

    /**
     * 获取产品风险等级。
     *
     * @return 产品风险等级。
     */
    public String getRiskGrade() {
        return riskGrade;
    }

    /**
     * 设置产品风险等级。
     *
     * @param riskGrade 产品风险等级。
     */
    public void setRiskGrade(final String riskGrade) {
        this.riskGrade = riskGrade;
    }

    /**
     * 获取份额类型名称。
     *
     * @return 份额类型名称。
     */
    public String getShrTyp() {
        return shrTyp;
    }

    /**
     * 设置份额类型名称。
     *
     * @param shrTyp 份额类型名称。
     */
    public void setShrTyp(final String shrTyp) {
        this.shrTyp = shrTyp;
    }

    /**
     * 获取是否个人养老金产品。
     *
     * @return 是否个人养老金产品。
     */
    public String getIsPsnlPnsn() {
        return isPsnlPnsn;
    }

    /**
     * 设置是否个人养老金产品。
     *
     * @param isPsnlPnsn 是否个人养老金产品。
     */
    public void setIsPsnlPnsn(final String isPsnlPnsn) {
        this.isPsnlPnsn = isPsnlPnsn;
    }

    /**
     * 获取计息方式名称。
     *
     * @return 计息方式名称。
     */
    public String getIntrMthd() {
        return intrMthd;
    }

    /**
     * 设置计息方式名称。
     *
     * @param intrMthd 计息方式名称。
     */
    public void setIntrMthd(final String intrMthd) {
        this.intrMthd = intrMthd;
    }

    /**
     * 获取产品主题。
     *
     * @return 产品主题。
     */
    public String getPrdcThm() {
        return prdcThm;
    }

    /**
     * 设置产品主题。
     *
     * @param prdcThm 产品主题。
     */
    public void setPrdcThm(final String prdcThm) {
        this.prdcThm = prdcThm;
    }

    /**
     * 获取投资经理姓名(监管口径)。
     *
     * @return 投资经理姓名(监管口径)。
     */
    public String getInvsMngrNm() {
        return invsMngrNm;
    }

    /**
     * 设置投资经理姓名(监管口径)。
     *
     * @param invsMngrNm 投资经理姓名(监管口径)。
     */
    public void setInvsMngrNm(final String invsMngrNm) {
        this.invsMngrNm = invsMngrNm;
    }

    /**
     * 获取投资经理名称(产品部口径)。
     *
     * @return 投资经理名称(产品部口径)。
     */
    public String getPrdcInvsMngr() {
        return prdcInvsMngr;
    }

    /**
     * 设置投资经理名称(产品部口径)。
     *
     * @param prdcInvsMngr 投资经理名称(产品部口径)。
     */
    public void setPrdcInvsMngr(final String prdcInvsMngr) {
        this.prdcInvsMngr = prdcInvsMngr;
    }

    /**
     * 获取产品经理名称。
     *
     * @return 产品经理名称。
     */
    public String getPrdcMngrNm() {
        return prdcMngrNm;
    }

    /**
     * 设置产品经理名称。
     *
     * @param prdcMngrNm 产品经理名称。
     */
    public void setPrdcMngrNm(final String prdcMngrNm) {
        this.prdcMngrNm = prdcMngrNm;
    }

    /**
     * 获取产品到期日期。
     *
     * @return 产品到期日期。
     */
    public String getExprDt() {
        return exprDt;
    }

    /**
     * 设置产品到期日期。
     *
     * @param exprDt 产品到期日期。
     */
    public void setExprDt(final String exprDt) {
        this.exprDt = exprDt;
    }

    /**
     * 获取定开基准日期。
     *
     * @return 定开基准日期。
     */
    public String getEndPrdExprDt() {
        return endPrdExprDt;
    }

    /**
     * 设置定开基准日期。
     *
     * @param endPrdExprDt 定开基准日期。
     */
    public void setEndPrdExprDt(final String endPrdExprDt) {
        this.endPrdExprDt = endPrdExprDt;
    }

    /**
     * 获取每日全量快照分区日期。
     *
     * @return 每日全量快照分区日期。
     */
    public String getAcctDt() {
        return acctDt;
    }

    /**
     * 设置每日全量快照分区日期。
     *
     * @param acctDt 每日全量快照分区日期。
     */
    public void setAcctDt(final String acctDt) {
        this.acctDt = acctDt;
    }
}
