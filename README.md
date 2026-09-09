# 智能问答与智能审核平台（技术代号）

基于 Java 8、Spring Boot 2.7、MyBatis-Plus、GoldenDB、Redis、Maven、OBS 和 Docker 的企业内部智能服务。正式服务名称尚未确认，当前工程名为 `intelligent-qa-audit-service`。

第一阶段聚焦智能问答：会话管理、接入公司知识库平台查询接口、受控业务数据查询、大模型流式回答、回答反馈和重新发起原问题。知识上传维护由公司知识库平台负责，不在本系统范围内；合同智能比对、申赎确认单核对及合同智能审核保留为后续独立阶段。

> 当前已完成供应商无关的核心端口、提问 Agent 类型识别与路由、只开放双通道查询的场景计划与节点执行器、GoldenDB 持久化骨架、会话游标分页与活动删除保护、回答事件/产物恢复、`dws_product_info_d` 产品快照表及三个首批产品问答查询、REST/SSE API、仅限开发环境的模拟用户、本地演示适配器和公司 HiAgent HTTP 适配器。Agent 展示目录由前端本地配置，后端不提供目录接口。知识库、公司模型真实环境、GoldenDB 数据权限、公司统一认证及生产 Redis 事件流尚未完成联调，因此本版本不是可直接上线的成品。

公司大模型平台已确定为 HiAgent 智能体创设平台。工程已经实现文档规定的“创建会话 → `chat_query` 流式调用”HTTP 适配器，但尚未获得实际服务地址、智能体变量、鉴权方式和脱敏流事件样例，因此默认关闭。

## 关键设计

- 六边形架构：领域层不依赖 Spring、数据库或模型厂商 SDK。
- 两步式问答：先提交问题并持久化，再通过 SSE 订阅回答。
- 完整停止闭环：停止请求持久化、并发状态保护、公司模型 `stop_message` 调用、失败重试与进程重启恢复。
- 两类受控意图：产品/交易基础信息，以及最新产品文档与关键要素。
- 双通道证据：知识库与业务数据库独立查询，确定性对账后才允许模型组织答案。
- 场景计划编排：`SMART_DATA` 仅映射到 `DUAL_CHANNEL_QA` 版本 `1`；执行器依次调用问题理解、知识检索、业务查询、证据对账和回答生成节点，并记录节点生命周期。其他场景暂不注册、不支持。
- 冲突时强制展示双源数据并提示人工复核；证据不足或意图低置信度时拒绝猜测。
- 指代消解和追问：已验证实体可跨历史窗口复用；对象缺失或歧义时返回持久化追问，用户回复候选序号、名称或唯一标识后恢复原意图。
- 最近有效对话同时传入意图识别和模型请求；结构化实体与待追问状态持久化到 GoldenDB，Token 自适应摘要仍列为上线前任务。
- GoldenDB 保存会话、问题、回答、状态、反馈和结构化上下文，是事实来源；MyBatis-Plus 负责数据访问，Flyway 仍是唯一表结构迁移入口。
- 一期附件已具备上传、列表、删除、用途选择、Owner/会话隔离、V5 元数据、问题文件关联和历史用户消息附件恢复；开发环境使用受限本地存储，生产 OBS/安全扫描/解析/OCR 未完成前失败关闭。
- Redis 计划承担跨实例事件重放、协调和限流；当前内存事件适配器仅供本地开发。
- 禁止大模型生成任意 SQL 并直接执行；一期业务查询通过标准语义字段、受控 Query Plan 和两个固定参数化 MyBatis 语句执行。
- 未配置真实依赖时默认失败关闭；`QA_DEMO_MODE=true` 仅启用明确标记的演示回答。
- 公司模型适配器严格发送 `UserID`、`AppConversationID`、`Query`、`ResponseMode=streaming` 和 `PubAgentJump=false`，并解析 SSE `data: json`。

## 目录

```text
src/main/java/com/acme/intelligentqa/
├── domain/model
├── domain/port/in
├── domain/port/out
├── application/service
├── application/workflow         # 场景计划、执行上下文、执行器与节点
├── adapter/in/web
├── adapter/out/persistence
│   └── mybatis             # MyBatis-Plus Record、BaseMapper 与显式联表 SQL
├── adapter/out/business     # 一期产品快照表固定 MyBatis 查询
├── adapter/out/ai
├── adapter/out/stream
├── infrastructure/companymodel
├── config
└── common/error
```

## 构建与质量门禁

### 中文注释规范

- 所有类、接口、枚举、枚举值、成员字段、构造器和方法（包括私有成员）必须使用中文 Javadoc；所有入参必须有中文 `@param`，非 `void` 方法必须有中文 `@return`，显式异常必须有中文 `@throws`。
- 复杂业务规则、状态转换、并发控制、幂等、降级与安全边界使用中文行内注释解释设计原因。
- 注释必须与代码同步，不逐行复述实现，不保留注释掉的旧代码，不用注释代替清晰命名；协议字段和第三方术语可保留英文。
- `ChineseDocumentationArchitectureTest` 会扫描生产与测试 Java 源码，遗漏中文声明注释或入参标签时完整构建会失败。

需要 JDK 8 和 Maven 3.9+：

```bash
mvn -B -ntp clean verify
```

该命令执行 Checkstyle、PMD/CPD、SpotBugs、ArchUnit、JaCoCo、Maven Enforcer、单元测试和集成测试门禁。

当前基线于 2026-09-03 在 OpenJDK 8u452、Maven 3.9.9 上通过完整门禁：117 个自动化测试无失败、无错误、无跳过，Checkstyle、PMD/CPD、SpotBugs、ArchUnit、中文注释检查、JaCoCo 和可执行 JAR 构建全部成功。新增回归覆盖双通道场景计划顺序、版本、节点生命周期记录、前置条件跳过与合法短路，以及开发模拟用户、非开发环境启动保护、Agent 类型必填/非法值拒绝与执行路由、稳定游标、活动会话删除保护、执行事件与产物恢复、一期产品/交易语义解析、查询规划、固定 SQL 编译、Owner 行权限与结果转换、附件大小、类型、文件头、失败补偿、归属、状态、延迟清理、随问题展示、历史恢复和重新发起时的附件继承边界；持久化回归同时覆盖 30,000 个中文字符的完整回答、助手消息、结构化追问上下文、执行事件和问题文件关联。本地门禁不替代真实 GoldenDB、Redis、OBS、知识库和大模型环境验证。

## 本地运行

```bash
docker compose up --build
```

Compose 使用 MySQL 8.4 作为 GoldenDB 本地兼容替身，并显式启用演示模式。演示回答不会连接公司知识库、业务数据库或真实大模型，不能用于业务判断。

应用以 Spring Boot 可执行 JAR 运行，内嵌 Web 容器为 Jetty，不使用 Tomcat，也不需要部署到外部应用服务器。`HTTP_MAX_THREADS` 控制 Jetty 最大工作线程数，默认值为 `200`；生产值必须结合 SSE 长连接数量、请求耗时和容量测试确定。

## 环境配置

必须通过 `SPRING_PROFILES_ACTIVE` 明确选择且只能选择一个环境：

| 环境 | Profile | 配置文件 | 连接变量前缀 |
|---|---|---|---|
| 开发 | `dev` | `application-dev.xml` | `DEV_*` |
| 测试 | `test` | `application-test.xml` | `TEST_*` |
| 生产 | `prod` | `application-prod.xml` | `PROD_*` |

公共配置使用 `application.xml`。四个文件均采用 Java 标准 XML Properties 格式，由 `XmlApplicationEnvironmentPostProcessor` 在 Spring 容器创建前加载，不使用 YAML。系统环境变量、JVM 参数和命令行参数仍比 XML 配置具有更高优先级。

未选择环境、同时选择多个环境，或 Profile 与 `app.runtime.stage` 不一致时，应用会拒绝启动。生产配置固定关闭演示模式，且数据库、Redis、公司模型地址和凭据均没有默认值。当前环境可通过 `/actuator/info` 的 `app.environment` 查看，也会作为 Micrometer 的 `environment` 公共标签。

数据库连接使用对应环境的 `*_DB_URL`、`*_DB_USERNAME` 和 `*_DB_PASSWORD` 注入；连接池等跨环境调优项继续使用 `DB_POOL_*`。完整变量和启动示例见 `docs/operations/environment-configuration.md`。

应用使用 MyBatis-Plus 3.5.17 的 Spring Boot 2 Starter；普通单表写入和条件更新使用 `BaseMapper`/Lambda Wrapper，涉及 Owner 隔离、逻辑删除和消息联表的查询保留有界显式 SQL。

开发环境默认启用 `dws_product_info_d` 查询；测试和生产仍默认关闭。生产启用前需完成数据中台全量快照同步、产品数据权限、真实 GoldenDB 与独立只读账号验证：

```text
DEV_BUSINESS_QUERY_ENABLED=true
TEST_BUSINESS_QUERY_ENABLED=false
PROD_BUSINESS_QUERY_ENABLED=false
BUSINESS_QUERY_MAXIMUM_ROWS=100
BUSINESS_SEMANTIC_MODEL_VERSION=phase1-v1
```

完整字段、权限和上线门禁见 `docs/database/phase-one-business-semantic-query.md`。

接口要求已认证 `Principal`。`dev` 默认启用服务端模拟用户 `dev-user-001`，前端无需传递用户 ID；可通过 `DEV_MOCK_USER_ID` 修改本地用户，或设置 `DEV_AUTH_MODE=corporate` 切回统一认证。`test` 和 `prod` 固定为 `corporate`，尝试启用模拟认证会拒绝启动。服务端不会采信浏览器传入的任意用户 ID。

## 主要 API

- `POST /api/v1/chats`：新建会话
- `GET /api/v1/chats?cursor=...&limit=30`：稳定游标会话列表
- `GET /api/v1/chats/{chatId}/messages`：历史消息；用户消息包含随该次问题提交的安全附件元数据
- `PATCH /api/v1/chats/{chatId}`：修改名称
- `DELETE /api/v1/chats/{chatId}`：逻辑删除；活动回答存在时返回 `409` 和“会话正在执行，请停止后删除”
- `POST /api/v1/chats/{chatId}/files`：上传临时附件
- `GET /api/v1/chats/{chatId}/files`：查询会话附件
- `DELETE /api/v1/chats/{chatId}/files/{fileId}`：逻辑删除临时附件；已随问题提交的对象延迟清理
- `POST /api/v1/chats/{chatId}/questions`：提交问题；请求体必须携带前端所选 `agentType`，一期仅受理 `SMART_DATA`
- `GET /api/v1/answers/{answerId}/events`：SSE 流式回答
- `GET /api/v1/answers/{answerId}`：回答快照
- `POST /api/v1/answers/{answerId}/regenerations`：把原问题作为新问答轮次重新发起，并保留原问答
- `POST /api/v1/answers/{answerId}/cancellation`：停止生成回答
- `PUT /api/v1/answers/{answerId}/feedback`：喜欢/不喜欢

完整契约见 `docs/api/intelligent-qa-api.md`。

## 生产发布阻塞项

- 确认正式服务名、Maven 坐标和 Java 包名。
- 接入公司统一认证页面跳转、回跳及用户、部门、知识权限；一期不建设自有登录页。
- 完成公司知识库平台的查询接口、认证方式、权限字段、错误语义和 SLA 确认，并实现生产适配器；该接入属于第一阶段。
- 确认产品/交易字段字典、别名、主键、文档版本规则、字段权威来源及冲突容忍规则。
- 用标注数据实现并验证意图识别、实体提取和证据对账生产适配器。
- 由数据平台同步并签署 `dws_product_info_d` 每日全量快照，确认产品数据权限，配置独立只读数据源，并保存固定 SQL 的真实 GoldenDB `EXPLAIN` 证据。
- 确认公司 HiAgent 目标智能体、底层模型版本、部署位置及数据留存政策。
- 取得公司 HiAgent 服务地址、网络访问方式、智能体必填 `Inputs`、鉴权方式及真实脱敏 SSE 事件样例，并完成契约联调。
- 确认 SSO 用户标识是否满足 HiAgent `UserID` 长度 1～20；不满足时建立稳定、不可逆且可审计的映射规则。
- 实现并验证生产 Redis 事件流适配器。
- 以事务性 Outbox 或持久任务队列替换生成任务的本机线程投递，并实现超时任务恢复扫描；停止任务本身已经使用持久任务、租约和恢复扫描。
- 在真实 GoldenDB 验证表结构、索引、事务、分片和故障切换。
- 为历史消息补充分页；为幂等请求保存请求指纹并在键冲突时返回 `409`。会话稳定游标分页已经实现。
- 接入真实授权候选目录与生产意图识别器，完成 Token 自适应摘要、主题切换和长对话评测。
- 将回答终态与结构化上下文更新纳入同一事务或可靠事件/补偿闭环，并通过中途故障测试；当前两次写入不是原子提交。
- 确认内部数据分级、保留期限、审计和内容安全规则。
- 按真实回答时长完成持续 10 QPS 问答提交的流式负载与故障测试；其他 API 容量仍待确认。
- 按 `10 × P95 完整回答秒数` 计算并发容量：若回答耗时 30～60 秒，则需要承载约 300～600 个并发生成任务，当前本地线程池默认值不能作为生产容量依据。
- 使用业务黄金问题集验证答案质量并完成业务签署。

完整技术方案见 `docs/technical-solution-design-v2.md`，V1.0 评审稿仅保留用于历史追溯。详细需求见 `REQUIREMENTS.md`，架构见 `ARCHITECTURE.md`，上线前完成 `docs/operations/production-readiness-checklist.md`。
