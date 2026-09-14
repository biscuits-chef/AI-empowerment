-- 维护窗口切换：原 UUID 主键改为公开业务唯一键，物理主键统一为自增 id。
ALTER TABLE qa_conversation
    CHANGE COLUMN id public_id VARCHAR(36) NOT NULL,
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_qa_conversation_public_id (public_id),
    DROP INDEX idx_qa_conversation_owner_updated,
    ADD KEY idx_qa_conversation_owner_updated (owner_id, updated_at, public_id);
