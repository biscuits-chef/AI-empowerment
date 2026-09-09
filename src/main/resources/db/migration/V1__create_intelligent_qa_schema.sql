CREATE TABLE qa_conversation (
    id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY idx_qa_conversation_owner_updated (owner_id, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE qa_message (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    answer_id VARCHAR(36) NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_qa_message_conversation_created (conversation_id, created_at),
    KEY idx_qa_message_answer (answer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE qa_answer (
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
    PRIMARY KEY (id),
    UNIQUE KEY uk_qa_answer_owner_idempotency (owner_id, idempotency_key),
    KEY idx_qa_answer_conversation_created (conversation_id, created_at),
    KEY idx_qa_answer_question (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE qa_answer_feedback (
    answer_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    feedback_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (answer_id, owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
