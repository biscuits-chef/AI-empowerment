ALTER TABLE qa_answer
    ADD COLUMN cancel_idempotency_key VARCHAR(128) NULL,
    ADD COLUMN cancel_reason VARCHAR(32) NULL,
    ADD COLUMN cancelled_stage VARCHAR(24) NULL,
    ADD COLUMN provider_message_id VARCHAR(128) NULL,
    ADD COLUMN cancel_error_code VARCHAR(64) NULL,
    ADD COLUMN cancel_requested_at TIMESTAMP(6) NULL,
    ADD COLUMN cancelled_at TIMESTAMP(6) NULL;

CREATE TABLE qa_answer_cancel_task (
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
