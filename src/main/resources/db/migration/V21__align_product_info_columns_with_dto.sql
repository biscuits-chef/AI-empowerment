-- 将产品信息快照表字段名与 DataHub ProductInfoDto 实体类保持一致。
-- PROD_MAT_DT -> EXPR_DT, PERI_OPEN_BASE_DT -> END_PRD_EXPR_DT, DT -> ACCT_DT。
ALTER TABLE dws_product_info_d
    CHANGE COLUMN PROD_MAT_DT EXPR_DT VARCHAR(10) NULL COMMENT '产品到期日期，格式 yyyy-MM-dd',
    CHANGE COLUMN PERI_OPEN_BASE_DT END_PRD_EXPR_DT VARCHAR(10) NULL COMMENT '定开基准日期，格式 yyyy-MM-dd',
    CHANGE COLUMN DT ACCT_DT VARCHAR(10) NOT NULL COMMENT '每日全量快照分区日期，格式 yyyy-MM-dd',
    DROP INDEX uk_dws_product_info_code_snapshot,
    DROP INDEX idx_dws_product_info_dt_code,
    DROP INDEX idx_dws_product_info_dt_name,
    DROP INDEX idx_dws_product_info_dt_abbr,
    DROP INDEX idx_dws_product_info_dt_full_name,
    DROP INDEX idx_dws_product_info_dt_open_date,
    DROP INDEX idx_dws_product_info_dt_mat_date,
    ADD UNIQUE KEY uk_dws_product_info_code_snapshot (PRDC_CD, ACCT_DT),
    ADD KEY idx_dws_product_info_dt_code (ACCT_DT, PRDC_CD),
    ADD KEY idx_dws_product_info_dt_name (ACCT_DT, PRDC_NM, PRDC_CD),
    ADD KEY idx_dws_product_info_dt_abbr (ACCT_DT, PRDC_ABBR, PRDC_CD),
    ADD KEY idx_dws_product_info_dt_full_name (ACCT_DT, PRDC_FLL_NM, PRDC_CD),
    ADD KEY idx_dws_product_info_dt_open_date (ACCT_DT, END_PRD_EXPR_DT, PRDC_CD),
    ADD KEY idx_dws_product_info_dt_mat_date (ACCT_DT, EXPR_DT, PRDC_CD);
