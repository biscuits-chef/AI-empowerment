DROP VIEW IF EXISTS biz_semantic_trade_v;
DROP TABLE IF EXISTS test_business_trade;
DROP TABLE IF EXISTS dws_product_info_d;

CREATE TABLE dws_product_info_d (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    PRDC_CD VARCHAR(80) NOT NULL,
    FML_PRDC_CD VARCHAR(60),
    FML_PRDC_IDNT VARCHAR(255),
    FTHR_PRDC_NM VARCHAR(255),
    PRDC_NM VARCHAR(255),
    PRDC_ABBR VARCHAR(255),
    PRDC_FLL_NM VARCHAR(255),
    CATENA_CD VARCHAR(255),
    CATENA_NM VARCHAR(255),
    OPN_TYP VARCHAR(255),
    PRDC_TYP VARCHAR(255),
    PRDT_TP VARCHAR(255),
    PRDC_FRM VARCHAR(255),
    ISS_MTHD VARCHAR(255),
    RS_MTHD VARCHAR(255),
    RS_CRRN VARCHAR(255),
    TRM_TYP VARCHAR(255),
    PRDC_CLSS VARCHAR(255),
    GRP_UNT_NM VARCHAR(255),
    PRDC_ASST_TYP VARCHAR(255),
    PRDC_MRKT_TYP VARCHAR(255),
    PFTA_FIPR_IDNT VARCHAR(10),
    ACCN_NM VARCHAR(255),
    SUBORG_NM VARCHAR(255),
    DVLP_TYP VARCHAR(255),
    OPN_CLSS VARCHAR(255),
    PRDC_BRND VARCHAR(255),
    PRDC_PSTN VARCHAR(255),
    PRDC_BRND_2 VARCHAR(255),
    PRDC_BRND_3 VARCHAR(255),
    RISK_GRADE VARCHAR(255),
    SHR_TYP VARCHAR(255),
    IS_PSNL_PNSN VARCHAR(1),
    INTR_MTHD VARCHAR(255),
    PRDC_THM VARCHAR(255),
    INVS_MNGR_NM VARCHAR(30),
    PRDC_INVS_MNGR VARCHAR(50),
    PRDC_MNGR_NM VARCHAR(255),
    EXPR_DT VARCHAR(10),
    END_PRD_EXPR_DT VARCHAR(10),
    ACCT_DT VARCHAR(10) NOT NULL,
    CONSTRAINT uk_dws_product_info_code_snapshot UNIQUE (PRDC_CD, ACCT_DT)
);

CREATE TABLE test_business_trade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    authorized_user_id VARCHAR(128) NOT NULL,
    trade_serial_number VARCHAR(64) NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    trader_name VARCHAR(100),
    trade_date DATE,
    updated_at TIMESTAMP NOT NULL
);

CREATE VIEW biz_semantic_trade_v AS SELECT * FROM test_business_trade;

INSERT INTO dws_product_info_d (
    PRDC_CD, PRDC_NM, PRDC_ABBR, PRDC_FLL_NM, PRDC_MNGR_NM,
    INVS_MNGR_NM, PRDC_INVS_MNGR, PRDC_FRM, EXPR_DT,
    END_PRD_EXPR_DT, ACCT_DT
) VALUES
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '旧产品经理',
 '旧监管投资经理', '旧产品部投资经理', '封闭式', '2026-12-31', NULL, '2026-09-01'),
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '产品经理甲',
 '监管投资经理甲', '产品部投资经理甲', '封闭式', '2026-12-31', NULL, '2026-09-07'),
('P002', '悦享6号', '悦享六号', '悦享6号定期开放理财产品', '产品经理乙',
 '监管投资经理乙', '产品部投资经理乙', '定期开放式', '2027-06-30', '2026-09-08', '2026-09-07'),
('P003', '稳享1号', '稳享一号', '稳享1号固定收益类理财产品', '产品经理丙',
 '监管投资经理丙', '产品部投资经理丙', '封闭式', '2026-09-08', NULL, '2026-09-07'),
('P004', '双命中产品', '双命中', '双日期命中测试产品', '产品经理丁',
 '监管投资经理丁', '产品部投资经理丁', '定期开放式', '2026-09-08', '2026-09-08', '2026-09-07'),
('P005', '普通产品', '普通', '普通测试产品', '产品经理戊',
 '监管投资经理戊', '产品部投资经理戊', '封闭式', '2027-12-31', NULL, '2026-09-07'),
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '未来产品经理',
 '未来监管投资经理', '未来产品部投资经理', '封闭式', '2026-12-31', NULL, '2026-09-09');

INSERT INTO test_business_trade (
    authorized_user_id, trade_serial_number, product_code,
    trader_name, trade_date, updated_at
) VALUES
('user-a', 'T001', 'P001', '交易员甲', DATE '2026-08-01', CURRENT_TIMESTAMP),
('user-b', 'T001', 'P001', '交易员乙', DATE '2026-08-01', CURRENT_TIMESTAMP);
