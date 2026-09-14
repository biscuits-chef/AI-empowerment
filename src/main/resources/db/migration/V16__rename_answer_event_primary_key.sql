-- 事件序号原值不变，只把自增主键列从 event_id 统一改名为 id。
ALTER TABLE qa_answer_event
    CHANGE COLUMN event_id id BIGINT NOT NULL AUTO_INCREMENT,
    DROP INDEX idx_qa_answer_event_answer_sequence,
    ADD KEY idx_qa_answer_event_answer_sequence (answer_id, id);
