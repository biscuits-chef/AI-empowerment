DROP VIEW IF EXISTS biz_semantic_trade_v;
DROP TABLE IF EXISTS test_business_trade;
DROP TABLE IF EXISTS dws_product_info_d;

CREATE TABLE dws_product_info_d (
    PRDC_CD VARCHAR(80) NOT NULL,
    PRDC_NM VARCHAR(255),
    PRDC_ABBR VARCHAR(255),
    PRDC_FLL_NM VARCHAR(255),
    PRDC_MNGR_NM VARCHAR(255),
    INVS_MNGR_NM VARCHAR(30),
    PRDC_INVS_MNGR VARCHAR(50),
    PRDC_FRM VARCHAR(255),
    PROD_MAT_DT VARCHAR(10),
    PERI_OPEN_BASE_DT VARCHAR(10),
    DT VARCHAR(10) NOT NULL,
    PRIMARY KEY (PRDC_CD, DT)
);

CREATE TABLE test_business_trade (
    authorized_user_id VARCHAR(128) NOT NULL,
    trade_serial_number VARCHAR(64) NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    trader_name VARCHAR(100),
    trade_date DATE,
    updated_at TIMESTAMP NOT NULL
);

CREATE VIEW biz_semantic_trade_v AS SELECT * FROM test_business_trade;

INSERT INTO dws_product_info_d VALUES
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

INSERT INTO test_business_trade VALUES
('user-a', 'T001', 'P001', '交易员甲', DATE '2026-08-01', CURRENT_TIMESTAMP),
('user-b', 'T001', 'P001', '交易员乙', DATE '2026-08-01', CURRENT_TIMESTAMP);
