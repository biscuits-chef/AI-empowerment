# GoldenDB 验证说明

示例使用 MySQL Connector/J、MyBatis-Plus 3.5.17 和 MySQL 风格 DDL。MyBatis-Plus 只负责数据访问映射，不代表目标 GoldenDB 必然兼容所有 MySQL 语法。不同 GoldenDB 产品版本的兼容细节可能不同；本文件是一份验证工作表，不代表对任何具体集群的能力承诺。

发布前记录：

- GoldenDB 产品版本、精确版本号、兼容模式、拓扑、可用区及故障切换策略；
- 受支持的连接器，以及 TLS/认证设置；
- MyBatis-Plus 生成的插入/更新 SQL、XML 联表 SQL、`ON DUPLICATE KEY UPDATE` 和 `TIMESTAMP(6)` 的兼容性；
- 分布/分片 Key 规则、同置表、全局索引，以及所有表 `id BIGINT AUTO_INCREMENT` 的序列、自增和热点行为；
- 支持的 DDL/DML、在线表结构变更约束、事务/隔离/锁行为；
- 使用有代表性的统计信息和数据量，为每条关键查询保留执行计划；
- 一期 `dws_product_info_d` 的自增 `id` 主键、`PRDC_CD + DT` 业务唯一键、日期字符串、产品解析索引、产品数据权限和固定参数化 SQL 兼容性；
- 兼容保留的 `biz_semantic_trade_v` 字段、权限和索引；
- 连接/会话上限、全部副本的 HikariCP 预算、语句/请求截止时间；
- 备份、恢复、PITR、复制、RPO、RTO、容量、监控和运维责任人；
- 已演练的“扩展—迁移—收缩”及前滚流程。

绝不编辑已经执行的 Flyway 迁移。新增带版本号的迁移，并在滚动发布全过程保持兼容。

Flyway 创建 `dws_product_info_d` 空表和索引，数据中台负责同步完整业务快照；兼容交易语义视图仍由数据平台维护。字段契约、权限规则和启用门禁见 `phase-one-business-semantic-query.md`。

## V8～V17 统一 id 自增主键验证

所有表最终必须且只能使用 `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`，不得存在 `pk_id`，UUID 和复合业务键只保留为普通唯一业务字段。四张资源表的旧 UUID `id` 改名为 `public_id`；事件表的 `event_id` 保留原值并改名为 `id`。

Java 持久化 Record 必须以数据库列为准按小驼峰命名：去掉下划线，并将下划线后的首字母大写，例如 `public_id -> publicId`、`message_id -> messageId`。物理列 `id` 只能映射为 `id`，不得使用 `databaseId` 等别名；该约束由集成测试扫描 `@TableId`、`@TableField` 和 Mapper ResultMap。

该合同切换不能与旧应用普通滚动混跑。生产必须显式设置 `PROD_FLYWAY_TARGET=17`，并在停写、任务排空和一致性备份后执行。发布前必须在目标 GoldenDB 验证：

- 每张表迁移前后行数、UUID、业务字段和逻辑引用不变，`id` 全部非空、唯一且自增；
- `public_id` 及反馈、停止任务、上下文、问题文件和产品快照业务唯一键继续拒绝重复；
- `event_id` 改名后历史数值、最大值和 SSE `Last-Event-ID` 重放边界完全不变；
- 列改名、主键交换、表和二级索引重建、元数据锁、临时空间与复制延迟符合批准窗口；
- 分片、主备切换和扩缩容期间自增值全局唯一且 JDBC 生成键回填可靠；
- 数据中台使用显式列清单并忽略产品表 `id`，完整快照发布和失败重试语义不变；
- 旧应用不能直接回滚到新结构，已准备兼容回滚版本或完成一致性备份恢复演练。

任一证据缺失均阻塞生产执行；不得用 H2 或 MySQL 风格脚本推断 GoldenDB 兼容性。

## V20 会话 Agent 类型验证

`V20__persist_conversation_agent_type.sql` 为 `qa_conversation` 增加不可空 `agent_type`，默认 `SMART_DATA` 用于兼容历史会话和滚动发布期间的旧应用。发布前必须在目标 GoldenDB 验证：

- 全量历史会话均得到 `SMART_DATA`，没有空值或截断值；
- 新应用首问显式写入类型，后续问题只读取会话值且不能更新；
- 新旧应用短暂并存时旧应用插入仍可使用默认值，不影响会话创建幂等唯一键；
- 加列操作的元数据锁、执行时长、复制延迟和失败前滚符合批准窗口；
- 后续开放更多 Agent 前，应评审并移除依赖默认值的旧写路径，避免新会话被静默归类为 `SMART_DATA`。

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
