CREATE TABLE qa_file (
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

CREATE TABLE qa_question_file (
    question_id VARCHAR(36) NOT NULL,
    file_id VARCHAR(36) NOT NULL,
    usage_type VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (question_id, file_id),
    KEY idx_qa_question_file_file (file_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
