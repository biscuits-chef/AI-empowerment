-- 维护窗口切换：停止任务使用自增 id 主键，一个回答仍只允许一个停止任务。
ALTER TABLE qa_answer_cancel_task
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_qa_answer_cancel_task_answer (answer_id),
    DROP INDEX idx_qa_cancel_task_dispatch,
    ADD KEY idx_qa_cancel_task_dispatch (
        task_status, next_attempt_at, lease_until, answer_id
    );
