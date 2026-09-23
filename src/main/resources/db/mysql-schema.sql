-- =============================================================================
-- MySQL / GoldenDB 全量表结构与初始测试数据（完整终态版）
-- 包含 DROP TABLE IF EXISTS，适配 MySQL 8.x / 9.x / GoldenDB
-- 执行后可直接从零建立与当前代码、MyBatis-Plus 实体及 Flyway V21 严格一致的全新数据库
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 安全清理已有视图与表（按依赖倒序清理）
-- -----------------------------------------------------------------------------
DROP VIEW IF EXISTS biz_semantic_trade_v;
DROP TABLE IF EXISTS
    test_business_trade,
    qa_question_file,
    qa_file,
    qa_answer_feedback,
    qa_answer_cancel_task,
    qa_answer_event,
    qa_answer,
    qa_message,
    qa_conversation_context,
    qa_conversation,
    dws_product_info_d;


-- -----------------------------------------------------------------------------
-- 2. 全量核心表定义（V21 终态：自增物理主键 id + 业务键 public_id）
-- -----------------------------------------------------------------------------

-- 2.1 会话主表 (qa_conversation)
CREATE TABLE qa_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    creation_idempotency_key VARCHAR(128) NULL,
    agent_type VARCHAR(32) NOT NULL DEFAULT 'SMART_DATA',
    title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_conversation_public_id (public_id),
    UNIQUE KEY uk_qa_conversation_owner_creation_key (owner_id, creation_idempotency_key),
    KEY idx_qa_conversation_owner_updated (owner_id, updated_at, public_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.2 会话上下文表 (qa_conversation_context)
CREATE TABLE qa_conversation_context (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL,
    state_json MEDIUMTEXT NOT NULL,
    source_question_id VARCHAR(36) NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_conversation_context_conversation (conversation_id),
    KEY idx_qa_context_owner (owner_id, conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.3 消息明细表 (qa_message)
CREATE TABLE qa_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    answer_id VARCHAR(36) NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_message_public_id (public_id),
    KEY idx_qa_message_conversation_created (conversation_id, created_at, public_id),
    KEY idx_qa_message_answer (answer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.4 回答主表 (qa_answer)
CREATE TABLE qa_answer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    trace_id VARCHAR(36) NOT NULL,
    regenerated_from_answer_id VARCHAR(36) NULL,
    app_conversation_id VARCHAR(128) NULL,
    status VARCHAR(24) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    error_code VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    cancel_idempotency_key VARCHAR(128) NULL,
    cancel_reason VARCHAR(32) NULL,
    cancelled_stage VARCHAR(24) NULL,
    message_id VARCHAR(128) NULL,
    query_id VARCHAR(128) NULL,
    task_id VARCHAR(128) NULL,
    total_tokens INT NULL,
    latency DOUBLE NULL,
    tracing_json_str MEDIUMTEXT NULL,
    intention_json_str MEDIUMTEXT NULL,
    retriever_resource TINYINT(1) NULL,
    cancel_error_code VARCHAR(64) NULL,
    cancel_requested_at TIMESTAMP(6) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_answer_public_id (public_id),
    UNIQUE KEY uk_qa_answer_owner_idempotency (owner_id, idempotency_key),
    KEY idx_qa_answer_conversation_created (conversation_id, created_at),
    KEY idx_qa_answer_question (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.5 回答事件历史表 (qa_answer_event)
CREATE TABLE qa_answer_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    answer_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    event_data MEDIUMTEXT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_qa_answer_event_answer_sequence (answer_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.6 回答停止任务表 (qa_answer_cancel_task)
CREATE TABLE qa_answer_cancel_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    answer_id VARCHAR(36) NOT NULL,
    task_status VARCHAR(16) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    lease_owner VARCHAR(64) NULL,
    lease_until TIMESTAMP(6) NULL,
    last_error_code VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_answer_cancel_task_answer (answer_id),
    KEY idx_qa_cancel_task_dispatch (task_status, next_attempt_at, lease_until, answer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.7 回答评价表 (qa_answer_feedback)
CREATE TABLE qa_answer_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    answer_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    feedback_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_answer_feedback_business (answer_id, owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.8 临时文件表 (qa_file，一期只读兼容)
CREATE TABLE qa_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
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
    UNIQUE KEY uk_qa_file_public_id (public_id),
    UNIQUE KEY uk_qa_file_owner_idempotency (owner_id, idempotency_key),
    KEY idx_qa_file_conversation_created (conversation_id, created_at, public_id),
    KEY idx_qa_file_owner_status (owner_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.9 问题附件关联表 (qa_question_file，一期只读兼容)
CREATE TABLE qa_question_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id VARCHAR(36) NOT NULL,
    file_id VARCHAR(36) NOT NULL,
    usage_type VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_question_file_business (question_id, file_id),
    KEY idx_qa_question_file_file (file_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2.10 产品主题每日全量快照表 (dws_product_info_d)
CREATE TABLE dws_product_info_d (
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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '产品主题每日全量快照表';

-- -----------------------------------------------------------------------------
-- 3. 本地开发/测试可选辅助表与视图（业务流水与产品测试数据）
-- -----------------------------------------------------------------------------

-- 3.1 业务交易测试底表 (test_business_trade) 与 视图 (biz_semantic_trade_v)
CREATE TABLE test_business_trade (
    id BIGINT NOT NULL AUTO_INCREMENT,
    authorized_user_id VARCHAR(128) NOT NULL,
    trade_serial_number VARCHAR(64) NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    trader_name VARCHAR(100) NULL,
    trade_date DATE NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_test_trade_auth_sn (authorized_user_id, trade_serial_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE VIEW biz_semantic_trade_v AS SELECT * FROM test_business_trade;

-- 3.2 初始测试数据：产品主题快照
INSERT INTO dws_product_info_d (
    PRDC_CD, PRDC_NM, PRDC_ABBR, PRDC_FLL_NM, PRDC_MNGR_NM,
    INVS_MNGR_NM, PRDC_INVS_MNGR, PRDC_FRM, EXPR_DT,
    END_PRD_EXPR_DT, ACCT_DT
) VALUES
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '旧产品经理', '旧监管投资经理', '旧产品部投资经理', '封闭式', '2026-12-31', NULL, '2026-09-01'),
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '产品经理甲', '监管投资经理甲', '产品部投资经理甲', '封闭式', '2026-12-31', NULL, '2026-09-07'),
('P002', '悦享6号', '悦享六号', '悦享6号定期开放理财产品', '产品经理乙', '监管投资经理乙', '产品部投资经理乙', '定期开放式', '2027-06-30', '2026-09-08', '2026-09-07'),
('P003', '稳享1号', '稳享一号', '稳享1号固定收益类理财产品', '产品经理丙', '监管投资经理丙', '产品部投资经理丙', '封闭式', '2026-09-08', NULL, '2026-09-07'),
('P004', '双命中产品', '双命中', '双日期命中测试产品', '产品经理丁', '监管投资经理丁', '产品部投资经理丁', '定期开放式', '2026-09-08', '2026-09-08', '2026-09-07'),
('P005', '普通产品', '普通', '普通测试产品', '产品经理戊', '监管投资经理戊', '产品部投资经理戊', '封闭式', '2027-12-31', NULL, '2026-09-07'),
('P001', '悦享3号', '悦享三号', '悦享3号固定收益类理财产品', '未来产品经理', '未来监管投资经理', '未来产品部投资经理', '封闭式', '2026-12-31', NULL, '2026-09-09');

-- 3.3 初始测试数据：交易事实底表
INSERT INTO test_business_trade (
    authorized_user_id, trade_serial_number, product_code,
    trader_name, trade_date, updated_at
) VALUES
('user-a', 'T001', 'P001', '交易员甲', DATE '2026-08-01', CURRENT_TIMESTAMP),
('user-b', 'T001', 'P001', '交易员乙', DATE '2026-08-01', CURRENT_TIMESTAMP);