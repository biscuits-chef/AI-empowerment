-- 维护窗口切换：产品快照使用自增 id 主键，代码与快照日期继续唯一。
-- 数据中台必须使用显式列清单并忽略数据库生成的 id。
ALTER TABLE dws_product_info_d
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_dws_product_info_code_snapshot (PRDC_CD, DT);
