-- 维护窗口切换：附件关系使用自增 id 主键，原问题与文件组合继续唯一。
ALTER TABLE qa_question_file
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_qa_question_file_business (question_id, file_id);
