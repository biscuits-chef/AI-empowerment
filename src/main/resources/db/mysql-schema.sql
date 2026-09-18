-- MySQL 本地全量表结构与初始测试数据
-- 适配 MySQL 8.x / 9.x，无需 Flyway 版本强校验，支持直接启动自动初始化

CREATE TABLE IF NOT EXISTS qa_conversation (
    id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY idx_qa_conversation_owner_updated (owner_id, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS qa_conversation_context (
    conversation_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL,
    state_json MEDIUMTEXT NOT NULL,
    source_question_id VARCHAR(36) NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (conversation_id),
    KEY idx_qa_context_owner (owner_id, conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS qa_message (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    answer_id VARCHAR(36) NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_qa_message_conversation_created (conversation_id, created_at),
    KEY idx_qa_message_answer (answer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS qa_answer (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    trace_id VARCHAR(36) NOT NULL,
    regenerated_from_answer_id VARCHAR(36) NULL,
    status VARCHAR(24) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    error_code VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    cancel_idempotency_key VARCHAR(128) NULL,
    cancel_reason VARCHAR(32) NULL,
    cancelled_stage VARCHAR(24) NULL,
    provider_message_id VARCHAR(128) NULL,
    cancel_error_code VARCHAR(64) NULL,
    cancel_requested_at TIMESTAMP(6) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_answer_owner_idempotency (owner_id, idempotency_key),
    KEY idx_qa_answer_conversation_created (conversation_id, created_at),
    KEY idx_qa_answer_question (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS qa_answer_event (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    answer_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    event_data MEDIUMTEXT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_qa_answer_event_answer_sequence (answer_id, event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS qa_answer_cancel_task (
    answer_id VARCHAR(36) NOT NULL,
    task_status VARCHAR(16) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    lease_owner VARCHAR(64) NULL,
    lease_until TIMESTAMP(6) NULL,
    last_error_code VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (answer_id),
    KEY idx_qa_cancel_task_dispatch (task_status, next_attempt_at, lease_until)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS qa_answer_feedback (
    answer_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    feedback_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (answer_id, owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS qa_file (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    usage_type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_file_owner_idempotency (owner_id, idempotency_key),
    KEY idx_qa_file_conversation_created (conversation_id, created_at),
    KEY idx_qa_file_owner_status (owner_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS qa_question_file (
    question_id VARCHAR(36) NOT NULL,
    file_id VARCHAR(36) NOT NULL,
    usage_type VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (question_id, file_id),
    KEY idx_qa_question_file_file (file_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS dws_product_info_d (
    id BIGINT NOT NULL AUTO_INCREMENT,
    PRDC_CD VARCHAR(80) NOT NULL COMMENT '产品代码',
    FML_PRDC_CD VARCHAR(60) NULL COMMENT '母产品代码',
    FML_PRDC_IDNT VARCHAR(255) NULL COMMENT '母产品标识',
    FTHR_PRDC_NM VARCHAR(255) NULL COMMENT '母产品名称',
    PRDC_NM VARCHAR(255) NULL COMMENT '产品名称',
    PRDC_ABBR VARCHAR(255) NULL COMMENT '产品简称',
    PRDC_FLL_NM VARCHAR(255) NULL COMMENT '产品全称',
    CATENA_CD VARCHAR(255) NULL COMMENT '系列代码',
    CATENA_NM VARCHAR(255) NULL COMMENT '系列名称',
    OPN_TYP VARCHAR(255) NULL COMMENT '开放类型名称',
    PRDC_TYP VARCHAR(255) NULL COMMENT '产品类型名称',
    PRDT_TP VARCHAR(255) NULL COMMENT '产品类别名称',
    PRDC_FRM VARCHAR(255) NULL COMMENT '产品形态',
    ISS_MTHD VARCHAR(255) NULL COMMENT '发行方式名称',
    RS_MTHD VARCHAR(255) NULL COMMENT '募集方式名称',
    RS_CRRN VARCHAR(255) NULL COMMENT '募集币种',
    TRM_TYP VARCHAR(255) NULL COMMENT '期限类型名称',
    PRDC_CLSS VARCHAR(255) NULL COMMENT '产品分类',
    GRP_UNT_NM VARCHAR(255) NULL COMMENT '投组单元名称',
    PRDC_ASST_TYP VARCHAR(255) NULL COMMENT '产品资产类型名称',
    PRDC_MRKT_TYP VARCHAR(255) NULL COMMENT '产品市场类型名称',
    PFTA_FIPR_IDNT VARCHAR(10) NULL COMMENT '养老理财产品标识',
    ACCN_NM VARCHAR(255) NULL COMMENT '账户名称',
    SUBORG_NM VARCHAR(255) NULL COMMENT '所属机构名称',
    DVLP_TYP VARCHAR(255) NULL COMMENT '开发类型名称',
    OPN_CLSS VARCHAR(255) NULL COMMENT '开放类别名称',
    PRDC_BRND VARCHAR(255) NULL COMMENT '产品品牌',
    PRDC_PSTN VARCHAR(255) NULL COMMENT '策略标签',
    PRDC_BRND_2 VARCHAR(255) NULL COMMENT '产品品牌二类',
    PRDC_BRND_3 VARCHAR(255) NULL COMMENT '产品品牌三类',
    RISK_GRADE VARCHAR(255) NULL COMMENT '产品风险等级',
    SHR_TYP VARCHAR(255) NULL COMMENT '份额类型名称',
    IS_PSNL_PNSN VARCHAR(1) NULL COMMENT '是否个人养老金产品，Y 表示是，N 表示否',
    INTR_MTHD VARCHAR(255) NULL COMMENT '计息方式名称',
    PRDC_THM VARCHAR(255) NULL COMMENT '产品主题',
    INVS_MNGR_NM VARCHAR(30) NULL COMMENT '投资经理姓名，监管口径',
    PRDC_INVS_MNGR VARCHAR(50) NULL COMMENT '投资经理名称，产品部口径',
    PRDC_MNGR_NM VARCHAR(255) NULL COMMENT '产品经理名称，最近一次处理人',
    EXPR_DT VARCHAR(10) NULL COMMENT '产品到期日期，格式 yyyy-MM-dd',
    END_PRD_EXPR_DT VARCHAR(10) NULL COMMENT '定开基准日期，格式 yyyy-MM-dd',
    ACCT_DT VARCHAR(10) NOT NULL COMMENT '每日全量快照分区日期，格式 yyyy-MM-dd',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dws_product_info_code_snapshot (PRDC_CD, ACCT_DT),
    KEY idx_dws_product_info_dt_code (ACCT_DT, PRDC_CD),
    KEY idx_dws_product_info_dt_name (ACCT_DT, PRDC_NM, PRDC_CD),
    KEY idx_dws_product_info_dt_abbr (ACCT_DT, PRDC_ABBR, PRDC_CD),
    KEY idx_dws_product_info_dt_full_name (ACCT_DT, PRDC_FLL_NM, PRDC_CD),
    KEY idx_dws_product_info_dt_open_date (ACCT_DT, END_PRD_EXPR_DT, PRDC_CD),
    KEY idx_dws_product_info_dt_mat_date (ACCT_DT, EXPR_DT, PRDC_CD)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS test_business_trade (
    authorized_user_id VARCHAR(128) NOT NULL,
    trade_serial_number VARCHAR(64) NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    trader_name VARCHAR(100),
    trade_date DATE,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (authorized_user_id, trade_serial_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE OR REPLACE VIEW biz_semantic_trade_v AS SELECT * FROM test_business_trade;

-- 插入产品快照基线数据
INSERT IGNORE INTO dws_product_info_d (PRDC_CD, PRDC_NM, PRDC_ABBR, PRDC_FLL_NM, PRDC_MNGR_NM, INVS_MNGR_NM, PRDC_INVS_MNGR, PRDC_FRM, EXPR_DT, END_PRD_EXPR_DT, ACCT_DT) VALUES
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '旧产品经理', '旧监管投资经理', '旧产品部投资经理', '封闭式', '2026-12-31', NULL, '2026-09-01'),
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '产品经理甲', '监管投资经理甲', '产品部投资经理甲', '封闭式', '2026-12-31', NULL, '2026-09-07'),
('P002', '悦享6号', '悦享六号', '悦享6号定期开放理财产品', '产品经理乙', '监管投资经理乙', '产品部投资经理乙', '定期开放式', '2027-06-30', '2026-09-08', '2026-09-07'),
('P003', '稳享1号', '稳享一号', '稳享1号固定收益类理财产品', '产品经理丙', '监管投资经理丙', '产品部投资经理丙', '封闭式', '2026-09-08', NULL, '2026-09-07'),
('P004', '双命中产品', '双命中', '双日期命中测试产品', '产品经理丁', '监管投资经理丁', '产品部投资经理丁', '定期开放式', '2026-09-08', '2026-09-08', '2026-09-07'),
('P005', '普通产品', '普通', '普通测试产品', '产品经理戊', '监管投资经理戊', '产品部投资经理戊', '封闭式', '2027-12-31', NULL, '2026-09-07'),
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '未来产品经理', '未来监管投资经理', '未来产品部投资经理', '封闭式', '2026-12-31', NULL, '2026-09-09');

INSERT IGNORE INTO test_business_trade (authorized_user_id, trade_serial_number, product_code, trader_name, trade_date, updated_at) VALUES
('dev-user-001', 'T001', 'P001', '交易员甲', '2026-08-01', CURRENT_TIMESTAMP),
('user-a', 'T001', 'P001', '交易员甲', '2026-08-01', CURRENT_TIMESTAMP),
('user-b', 'T001', 'P001', '交易员乙', '2026-08-01', CURRENT_TIMESTAMP);
