-- 维护窗口切换：文件 UUID 继续用于 API 与 OBS 对象键，但不再作为物理主键。
ALTER TABLE qa_file
    CHANGE COLUMN id public_id VARCHAR(36) NOT NULL,
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_qa_file_public_id (public_id),
    DROP INDEX idx_qa_file_conversation_created,
    ADD KEY idx_qa_file_conversation_created (conversation_id, created_at, public_id);
