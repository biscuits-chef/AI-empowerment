DROP TABLE IF EXISTS qa_answer_feedback;
DROP TABLE IF EXISTS qa_answer_cancel_task;
DROP TABLE IF EXISTS qa_answer_event;
DROP TABLE IF EXISTS qa_conversation_context;
DROP TABLE IF EXISTS qa_answer;
DROP TABLE IF EXISTS qa_message;
DROP TABLE IF EXISTS qa_conversation;
DROP TABLE IF EXISTS qa_question_file;
DROP TABLE IF EXISTS qa_file;

CREATE TABLE qa_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL UNIQUE,
    owner_id VARCHAR(128) NOT NULL,
    creation_idempotency_key VARCHAR(128) NULL,
    agent_type VARCHAR(32) NOT NULL DEFAULT 'SMART_DATA',
    title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL
);

CREATE UNIQUE INDEX uk_qa_conversation_owner_creation_key
    ON qa_conversation (owner_id, creation_idempotency_key);

CREATE INDEX idx_qa_conversation_owner_updated
    ON qa_conversation (owner_id, updated_at, public_id);

CREATE TABLE qa_conversation_context (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL UNIQUE,
    owner_id VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL,
    state_json MEDIUMTEXT NOT NULL,
    source_question_id VARCHAR(36) NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_qa_context_owner
    ON qa_conversation_context (owner_id, conversation_id);

CREATE TABLE qa_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL UNIQUE,
    conversation_id VARCHAR(36) NOT NULL,
    answer_id VARCHAR(36) NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_qa_message_conversation_created
    ON qa_message (conversation_id, created_at, public_id);

CREATE TABLE qa_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL UNIQUE,
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
    tracing_json_str CLOB NULL,
    intention_json_str CLOB NULL,
    retriever_resource BOOLEAN NULL,
    cancel_error_code VARCHAR(64) NULL,
    cancel_requested_at TIMESTAMP(6) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_qa_answer_owner_idempotency UNIQUE (owner_id, idempotency_key)
);

CREATE TABLE qa_answer_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    event_data MEDIUMTEXT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_qa_answer_event_answer_sequence
    ON qa_answer_event (answer_id, id);

CREATE TABLE qa_answer_cancel_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_id VARCHAR(36) NOT NULL UNIQUE,
    task_status VARCHAR(16) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    lease_owner VARCHAR(64) NULL,
    lease_until TIMESTAMP(6) NULL,
    last_error_code VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_qa_cancel_task_dispatch
    ON qa_answer_cancel_task (task_status, next_attempt_at, lease_until, answer_id);

CREATE TABLE qa_answer_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    feedback_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_qa_answer_feedback_business UNIQUE (answer_id, owner_id)
);

CREATE TABLE qa_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL UNIQUE,
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
    CONSTRAINT uk_qa_file_owner_idempotency UNIQUE (owner_id, idempotency_key)
);

CREATE INDEX idx_qa_file_conversation_created
    ON qa_file (conversation_id, created_at, public_id);

CREATE INDEX idx_qa_file_owner_status
    ON qa_file (owner_id, status);

CREATE TABLE qa_question_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id VARCHAR(36) NOT NULL,
    file_id VARCHAR(36) NOT NULL,
    usage_type VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_qa_question_file_business UNIQUE (question_id, file_id)
);

CREATE INDEX idx_qa_question_file_file
    ON qa_question_file (file_id);
