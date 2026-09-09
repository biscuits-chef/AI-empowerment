# GoldenDB 验证说明

示例使用 MySQL Connector/J、MyBatis-Plus 3.5.17 和 MySQL 风格 DDL。MyBatis-Plus 只负责数据访问映射，不代表目标 GoldenDB 必然兼容所有 MySQL 语法。不同 GoldenDB 产品版本的兼容细节可能不同；本文件是一份验证工作表，不代表对任何具体集群的能力承诺。

发布前记录：

- GoldenDB 产品版本、精确版本号、兼容模式、拓扑、可用区及故障切换策略；
- 受支持的连接器，以及 TLS/认证设置；
- MyBatis-Plus 生成的插入/更新 SQL、XML 联表 SQL、`ON DUPLICATE KEY UPDATE` 和 `TIMESTAMP(6)` 的兼容性；
- 分布/分片 Key 规则、同置表、全局索引、序列/自增行为；
- 支持的 DDL/DML、在线表结构变更约束、事务/隔离/锁行为；
- 使用有代表性的统计信息和数据量，为每条关键查询保留执行计划；
- 一期 `dws_product_info_d` 的复合主键、日期字符串、产品解析索引、产品数据权限和固定参数化 SQL 兼容性；
- 兼容保留的 `biz_semantic_trade_v` 字段、权限和索引；
- 连接/会话上限、全部副本的 HikariCP 预算、语句/请求截止时间；
- 备份、恢复、PITR、复制、RPO、RTO、容量、监控和运维责任人；
- 已演练的“扩展—迁移—收缩”及前滚流程。

绝不编辑已经执行的 Flyway 迁移。新增带版本号的迁移，并在滚动发布全过程保持兼容。

Flyway 创建 `dws_product_info_d` 空表和索引，数据中台负责同步完整业务快照；兼容交易语义视图仍由数据平台维护。字段契约、权限规则和启用门禁见 `phase-one-business-semantic-query.md`。

## V3 消息内容扩容验证

`V3__expand_message_content_capacity.sql` 将 `qa_message.content` 从 `TEXT` 扩展为 `MEDIUMTEXT`，用于消除 30,000 个中文字符可能超过 `TEXT` 字节容量的问题。发布前必须在目标 GoldenDB 验证：

- `MODIFY COLUMN ... MEDIUMTEXT NOT NULL` 的语法兼容性；
- 现有数据不丢失且字符集不发生变化；
- DDL 是否在线执行、锁表范围和持续时间；
- 主从复制、故障切换与备份恢复期间的行为；
- 若目标版本不支持在线修改，使用影子列或在线变更工具制定前滚方案。

## V4 结构化会话上下文验证

`V4__add_conversation_context.sql` 新增 `qa_conversation_context`，用于保存按用户和会话隔离的已验证实体、来源问题、上一意图、待追问候选及乐观版本。发布前必须在目标 GoldenDB 验证：

- `conversation_id` 主键与 `(owner_id, conversation_id)` 索引是否符合目标分片/同置规则；
- `state_json MEDIUMTEXT` 的容量、字符集、读写耗时和备份恢复行为；
- 版本条件更新在并发提问、重试和故障切换下只允许一个写入成功；
- 旧版本应用忽略新表、新版本应用可在滚动发布期间按需创建状态，迁移无需回填历史会话；
- 回滚应用版本时保留该表，后续通过前向迁移处理，不执行破坏性删除。
