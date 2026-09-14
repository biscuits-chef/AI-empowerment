-- 会话 Agent 类型仅在首次提问时写入，后续提问从会话读取且不可修改。
-- 默认值保证旧应用与新结构短暂并存时仍能创建一期智能问数会话。
ALTER TABLE qa_conversation
    ADD COLUMN agent_type VARCHAR(32) NOT NULL DEFAULT 'SMART_DATA' AFTER creation_idempotency_key;
