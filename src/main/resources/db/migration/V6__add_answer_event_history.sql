CREATE TABLE qa_answer_event (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    answer_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    event_data MEDIUMTEXT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_qa_answer_event_answer_sequence (answer_id, event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
