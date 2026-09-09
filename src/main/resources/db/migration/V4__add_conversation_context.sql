CREATE TABLE qa_conversation_context (
    conversation_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL,
    state_json MEDIUMTEXT NOT NULL,
    source_question_id VARCHAR(36) NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (conversation_id),
    KEY idx_qa_context_owner (owner_id, conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
