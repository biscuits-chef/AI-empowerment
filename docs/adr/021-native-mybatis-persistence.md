# ADR-021：使用原生 MyBatis 取代 MyBatis-Plus

- 状态：已接受
- 日期：2026-09-14
- 负责人：服务团队
- 决策人：用户指定
- 关联需求：将 MyBatis-Plus 改为 MyBatis
- 取代范围：完整取代 ADR-005；取代 ADR-015 中依赖 `IdType.AUTO` 的验证方式，不改变主键策略

## 背景

应用系统表此前同时使用 MyBatis-Plus `BaseMapper`、Lambda Wrapper 和显式 MyBatis SQL，业务查询本身已经使用固定原生 MyBatis Mapper。用户明确要求统一改为 MyBatis。继续保留两套 SQL 生成与字段映射机制会扩大权限、条件更新和 GoldenDB 兼容性的审查面。

Java 8 与 Spring Boot 2.7.18 是当前兼容基线。MyBatis 官方兼容矩阵将 `mybatis-spring-boot-starter` 2.3.x 对应到 Spring Boot 2.7 和 Java 8；2.3.2 发布说明显示其使用 Spring Boot 2.7.18。为避免框架切换把当前已解析的 MyBatis 核心 3.5.19 静默降为 Starter 默认的 3.5.14，本项目显式管理 MyBatis 3.5.19。

## 决策

1. 使用 `mybatis-spring-boot-starter` 与 `mybatis-spring-boot-starter-test` 2.3.2，并通过依赖管理固定 `org.mybatis:mybatis` 3.5.19。
2. Maven Enforcer 禁止全部 `com.baomidou:*` 直接或传递依赖。
3. Mapper 不继承通用 CRUD 接口，只声明仓储实际需要的具名方法；不在项目内重新实现 Wrapper 或通用 CRUD 抽象。
4. 持久化 Record 是普通 Java POJO，不使用表、主键或字段 ORM 注解。字段名继续严格按数据库列名转为小驼峰。
5. 保持 `map-underscore-to-camel-case=false`，所有 Record 查询使用显式 `resultMap`，避免字段静默错配。
6. 原 Lambda Wrapper 条件转换为固定、参数化且可审查的 XML SQL。Owner、逻辑删除、活动状态、幂等、乐观版本和租约条件不得弱化。
7. 自增插入显式省略物理 `id`。需要读取生成值的会话、消息、回答、上下文、文件和回答事件插入统一使用 `useGeneratedKeys="true"`、`keyProperty="id"` 与 `keyColumn="id"`；回答事件生成值继续作为 SSE 序号。
8. 配置前缀从 `mybatis-plus.*` 改为 `mybatis.*`，保留 Mapper 路径、关闭自动驼峰、语句级本地缓存、默认 Fetch Size、五秒语句超时和空值 JDBC 类型。
9. 领域端口、应用事务、REST/SSE、Flyway、表结构、公开 UUID 和业务唯一键全部保持不变，本次不新增数据库迁移。
10. 原生 MyBatis 类型仅允许出现在出站适配器或配置层；领域层和应用层不得依赖持久化框架。

## 失败语义与安全边界

- 回答创建只把回答唯一键竞争识别为幂等重放；后续用户消息、助手消息或会话更新时间写入失败必须使整个事务回滚。
- 状态迁移和乐观锁继续依赖受影响行数判定并发胜者，不引入通用数据库自动重试。
- 所有用户值继续使用 `#{}` 参数绑定，禁止 `${}`、动态表名、动态列名和模型生成 SQL。
- Spring 数据访问异常继续转换为稳定应用异常；目标 GoldenDB 的重复键、超时、死锁和连接异常翻译必须在真实环境验证。

## 后果

收益：

- 所有 SQL 与字段映射均显式可审查，权限和并发谓词只有一个实现来源。
- 生产依赖中移除 MyBatis-Plus，降低框架数量和隐式 SQL 行为。
- 业务查询计划、领域端口和外部 API 无需改变。

代价：

- 简单 CRUD 也需要维护 Mapper 方法和 XML。
- 新增字段必须同步更新插入语句、查询列清单和 `resultMap`。
- Starter 2.3.x 属于 Java 8/Spring Boot 2.7 兼容末线，升级到受支持 Java LTS 与 Spring Boot 版本时需要同步升级 MyBatis Starter。

## 验证

- 原生 `@MybatisTest` 覆盖会话、消息、回答、上下文、事件、取消任务、文件历史兼容和业务固定查询。
- 完整应用测试断言 MyBatis 运行时配置、Mapper Statement 装载和 JDBC 生成键策略。
- 集成测试检查最终表结构、显式 ResultMap、数据库字段命名和六类生成键 Mapper。
- Enforcer 与源码扫描证明生产及测试代码不存在 MyBatis-Plus 依赖、注解、Wrapper 或旧配置前缀。
- 完整门禁为 `mvn -B -ntp clean verify`。

本地 H2 只能证明快速行为回归。生产发布前仍须在真实 GoldenDB 验证生成键回填、固定 SQL、Owner 隔离、状态 CAS、乐观锁、事务、异常转换、语句超时、`ON DUPLICATE KEY UPDATE`、关键 `EXPLAIN` 和故障切换。

## 发布与回滚

本次没有 DDL、数据格式或 API 变化。完成同库兼容验证后可以金丝雀和滚动发布；回滚时整体恢复上一不可变应用制品及其 `mybatis-plus.*` 配置，不执行数据库逆向迁移。部署平台若独立注入旧配置前缀，必须在发布窗口同步切换，禁止依赖未知属性被静默忽略。

## 参考

- [MyBatis Spring Boot Starter 官方兼容说明](https://github.com/mybatis/spring-boot-starter)
- [MyBatis Spring Boot Starter 2.3.2 官方发布说明](https://github.com/mybatis/spring-boot-starter/releases/tag/mybatis-spring-boot-2.3.2)
