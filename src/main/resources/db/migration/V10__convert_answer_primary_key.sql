-- 维护窗口切换：回答 UUID 和 Owner 幂等键继续承担业务唯一性。
ALTER TABLE qa_answer
    CHANGE COLUMN id public_id VARCHAR(36) NOT NULL,
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_qa_answer_public_id (public_id);
