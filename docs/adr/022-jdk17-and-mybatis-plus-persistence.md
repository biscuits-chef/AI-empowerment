# ADR-022：升级 JDK 17 并使用 MyBatis-Plus 3.5.5 替换原生 MyBatis

- 状态：已接受
- 日期：2026-09-17
- 负责人：服务团队
- 决策人：用户指定
- 关联需求：项目 JDK 基线升级至 17；使用 MyBatis-Plus 3.5.5 替换原生 MyBatis；esf 和 uias 依赖在 dev 环境走本地 jar，其他环境走 Maven 仓库
- 取代范围：完整取代 ADR-021；修订 ADR-005（固定 MyBatis-Plus 版本为 3.5.5）；更新 ADR-015 的主键自增策略适配说明

## 背景

1. 工程此前受限于兼容基线采用 Java 8 与 Spring Boot 2.7.18，并采用原生 MyBatis 3.5.19（`mybatis-spring-boot-starter` 2.3.2）进行持久化。
2. 随着长期受支持 Java LTS 基线演进及开发效率需求，用户明确要求将工程 JDK 版本从 1.8 升级至 17。
3. 用户明确要求使用 `mybatis-plus-boot-starter` 3.5.5 统一替换原生 MyBatis。
4. 本地开发环境（`dev`）缺少公司私有 Maven 仓库网络连通性，需要依赖工程 `libs/` 下已存放的 esf 与 uias 相关 jar 包；而其他环境（如集成测试、构建流水线和生产）需直接通过 Maven 仓库解析。

## 决策

1. **JDK 17 基线与编译器配置**：
   - 将 `pom.xml` 中 `<java.version>` 与 `maven.compiler.source`/`maven.compiler.target` 统一升级为 `17`。
   - Maven Enforcer 插件配置 `requireJavaVersion` 为 `[17,)`。
2. **MyBatis-Plus 3.5.5 依赖管理与持久化升级**：
   - 引入 `com.baomidou:mybatis-plus-boot-starter:3.5.5`，移除 `mybatis-spring-boot-starter` 与 `mybatis-spring-boot-starter-test`。
   - 补充 `httpclient`、`httpmime` 与 `protobuf-java:3.25.2` 等第三方运行时依赖，保障在 JDK 17 环境下相关库正常运行。
   - 持久化实体（Record）恢复配置 MyBatis-Plus 注解：`@TableName`、`@TableId(type = IdType.AUTO)` 以及对字段列名显式声明 `@TableField`，保留属性小驼峰与数据库列名一一对应。
   - Mapper 接口统一继承 `BaseMapper<Record>`，利用 MyBatis-Plus 提供的单表 CRUD 与 Lambda 条件构造能力；复杂联表查询与快照只读查询（如产品快照查询）保留显式 XML 映射。
   - 仓储层实现（如 `MybatisAnswerRepository`、`MybatisConversationRepository`、`MybatisConversationContextRepository`、`MybatisTemporaryFileRepository`）采用 `LambdaQueryWrapper` / `LambdaUpdateWrapper` 执行类型安全、防注入的条件检索与 CAS 更新。
   - 配置前缀从 `mybatis.*` 调整为 `mybatis-plus.*`，保留 `mapper-locations` 与 `map-underscore-to-camel-case=false`。
3. **多环境依赖分离（Profile 机制）**：
   - 在 `pom.xml` 中引入 `dev` profile（默认激活），在此 profile 下将 `esf-sdk`、`uias-spring-boot-starter`、`uias-sdk` 配置为 `system` 作用域指向 `${project.basedir}/libs/...`。
   - 在默认依赖中将上述构件声明为标准 Maven 依赖，供非本地 dev 环境（持续集成、生产环境等）通过中央/企业 Maven 私服解析。
4. **日志滚动与测试适配**：
   - 修复 `logback-spring.xml` 中 `SizeAndTimeBasedRollingPolicy` 文件名模板缺失 `%i` 标记问题，确保 Spring Boot 测试与运行时日志轮转无误。
   - 测试类统一采用 `@SpringBootTest` 结合专用配置覆盖原生 `@MybatisTest`，保障 MyBatis-Plus 上下文配置与自动配置机制完全加载。

## 失败语义与安全边界

- 所有 LambdaWrapper 均使用强类型实体属性方法引用，禁止使用裸字符串列名或动态拼接 SQL。
- 业务数据查询依然禁止大模型直接生成 SQL，严格经过标准语义模型与预批准参数化语句。
- 数据库主键自增策略（`IdType.AUTO`）与业务 UUID 唯一标识分离，外部 API 绝不暴露物理 ID。
- 严格遵循六边形架构依赖约束，领域层和应用层保持纯粹，不依赖任何 MyBatis-Plus 类或注解。

## 后果

收益：
- 运行时与编译期升级至 Java 17，获得更强的语言特性与性能优化支持。
- MyBatis-Plus 3.5.5 的引入显著简化通用单表 CRUD 逻辑代码，减少模板化 XML SQL 维护成本。
- 本地开发与流水线/生产构建实现依赖解耦，本地无需依赖外部私服即可运行测试与启动。

代价：
- 需要维护 `dev` profile 与生产 Maven 私服的依赖版本对齐。
- MyBatis-Plus 版本固定为 3.5.5，后续升级需评估其对 Spring Boot 2.7.x 的兼容性。

## 验证

- 完整门禁命令：`mvn -B -ntp clean verify`。
- 包括 171 个单元测试与架构测试全部通过（0 failure, 0 error, 0 skip）。
- `DatabasePrimaryKeyPolicyIT` 6 项集成测试验证自增主键回填策略正常工作。
- Checkstyle、PMD/CPD、SpotBugs、JaCoCo 覆盖率门禁、中文文档门禁与 ArchUnit 六边形架构门禁全部通过。
