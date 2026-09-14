# ADR-005：使用 MyBatis-Plus 接入 GoldenDB

- 状态：已接受，物理主键部分已被 ADR-015 取代
- 日期：2026-08-19
- 负责人：服务团队
- 决策人：用户指定
- 关联需求：GoldenDB 会话、消息、回答和反馈持久化

## 背景

用户要求使用 MyBatis-Plus 接入数据库。工程此前存在普通 MyBatis Mapper 示例，但实际仓储仍通过 Spring JDBC 执行 SQL，形成两套持久化方式并增加维护成本。

## 决策

1. 使用兼容 Spring Boot 2 的 `mybatis-plus-boot-starter` 3.5.17，不再直接引入普通 MyBatis Spring Boot Starter。
2. 数据表记录使用 `@TableName`、`@TableId` 和显式字段映射；物理主键策略由 ADR-015 取代，领域对象仍不依赖 MyBatis-Plus。
3. 单表插入与条件更新使用 `BaseMapper` 和 Lambda Wrapper。
4. Owner 隔离、逻辑删除、消息联表、幂等读取和有界排序使用显式 XML SQL，以便审查权限条件和查询计划。
5. Flyway 继续作为唯一表结构迁移入口；本次仅替换访问实现，不修改现有表结构或已执行迁移。

## 后果

- 仓储端口和应用服务接口保持不变，可以替换数据库实现而不影响领域层。
- 需要同时审查 MyBatis-Plus 生成 SQL 与 XML SQL。
- `ON DUPLICATE KEY UPDATE`、时间精度和异常语义仍依赖目标 GoldenDB 的 MySQL 兼容能力。
- 本地 H2 MySQL 模式用于快速回归，不能作为 GoldenDB 兼容性证明。

## 验证与前滚

- `MybatisPlusPersistenceTest` 覆盖会话、逻辑删除、Owner 隔离、消息顺序、回答幂等、终态同步和反馈 Upsert。
- 发布前在真实 GoldenDB 执行迁移、SQL 契约、事务、锁、故障切换与关键查询 `EXPLAIN` 验证。
- 本次没有 DDL 变化；若必须撤销框架接入，可恢复原仓储适配器和依赖，不需要回滚数据。
