-- 维护窗口切换：消息 UUID 继续用于公开引用，但不再作为物理主键。
ALTER TABLE qa_message
    CHANGE COLUMN id public_id VARCHAR(36) NOT NULL,
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_qa_message_public_id (public_id),
    DROP INDEX idx_qa_message_conversation_created,
    ADD KEY idx_qa_message_conversation_created (conversation_id, created_at, public_id);
