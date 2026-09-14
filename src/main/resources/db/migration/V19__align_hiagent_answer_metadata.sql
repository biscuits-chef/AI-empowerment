-- 将公司 HiAgent 回答元数据列名与运行态 API 字段保持一致。
-- 系统自身的 conversation_id、question_id 和 content 具有不同领域语义，不在此迁移中改名。
ALTER TABLE qa_answer
    CHANGE COLUMN provider_message_id message_id VARCHAR(128) NULL,
    ADD COLUMN app_conversation_id VARCHAR(128) NULL,
    ADD COLUMN query_id VARCHAR(128) NULL,
    ADD COLUMN task_id VARCHAR(128) NULL,
    ADD COLUMN total_tokens INT NULL,
    ADD COLUMN latency DOUBLE NULL,
    ADD COLUMN tracing_json_str MEDIUMTEXT NULL,
    ADD COLUMN intention_json_str MEDIUMTEXT NULL,
    ADD COLUMN retriever_resource TINYINT(1) NULL;
