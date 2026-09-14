-- 维护窗口切换：反馈使用自增 id 主键，原复合键继续防止重复评价。
ALTER TABLE qa_answer_feedback
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_qa_answer_feedback_business (answer_id, owner_id);
