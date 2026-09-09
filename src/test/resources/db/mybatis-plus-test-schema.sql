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
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(128) NOT NULL,
    title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL
);

CREATE INDEX idx_qa_conversation_owner_updated
    ON qa_conversation (owner_id, updated_at);

CREATE TABLE qa_conversation_context (
    conversation_id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL,
    state_json MEDIUMTEXT NOT NULL,
    source_question_id VARCHAR(36) NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_qa_context_owner
    ON qa_conversation_context (owner_id, conversation_id);

CREATE TABLE qa_message (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    answer_id VARCHAR(36) NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_qa_message_conversation_created
    ON qa_message (conversation_id, created_at);

CREATE TABLE qa_answer (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
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
    CONSTRAINT uk_qa_answer_owner_idempotency UNIQUE (owner_id, idempotency_key)
);

CREATE TABLE qa_answer_event (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    event_data MEDIUMTEXT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_qa_answer_event_answer_sequence
    ON qa_answer_event (answer_id, event_id);

CREATE TABLE qa_answer_cancel_task (
    answer_id VARCHAR(36) NOT NULL PRIMARY KEY,
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
    ON qa_answer_cancel_task (task_status, next_attempt_at, lease_until);

CREATE TABLE qa_answer_feedback (
    answer_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    feedback_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (answer_id, owner_id)
);

CREATE TABLE qa_file (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
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

CREATE TABLE qa_question_file (
    question_id VARCHAR(36) NOT NULL,
    file_id VARCHAR(36) NOT NULL,
    usage_type VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (question_id, file_id)
);
