-- 维护窗口切换：上下文使用自增 id 主键，一个会话仍只允许一份上下文。
ALTER TABLE qa_conversation_context
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_qa_conversation_context_conversation (conversation_id);
