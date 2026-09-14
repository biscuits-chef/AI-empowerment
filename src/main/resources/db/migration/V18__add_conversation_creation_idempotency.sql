-- 统一提问接口使用同一个幂等键原子创建首次会话、问题和回答。
ALTER TABLE qa_conversation
    ADD COLUMN creation_idempotency_key VARCHAR(128) NULL AFTER owner_id,
    ADD UNIQUE KEY uk_qa_conversation_owner_creation_key (owner_id, creation_idempotency_key);
