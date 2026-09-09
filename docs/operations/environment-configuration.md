# 环境配置使用说明

## 目标

开发、测试和生产使用同一个不可变 JAR，通过 Spring Profile 和外部变量选择环境配置。配置文件只保存非敏感默认值；真实账号、密码、令牌、证书和生产地址由部署平台或密钥系统注入。

## 环境与配置文件

| 环境 | 启用值 | 配置文件 | 环境标识 |
|---|---|---|---|
| 开发 | `SPRING_PROFILES_ACTIVE=dev` | `application-dev.xml` | `DEVELOPMENT` |
| 测试 | `SPRING_PROFILES_ACTIVE=test` | `application-test.xml` | `TEST` |
| 生产 | `SPRING_PROFILES_ACTIVE=prod` | `application-prod.xml` | `PRODUCTION` |

`application.xml` 保存公共上限、超时、Jetty、MyBatis-Plus、Flyway 和监控配置。三个环境文件只覆盖连接信息、环境标识、演示模式、依赖开关和端点暴露范围。

四个配置文件均使用 Java 标准 XML Properties 格式：每个配置项写成 `<entry key="配置键">配置值</entry>`。`XmlApplicationEnvironmentPostProcessor` 在 Spring 容器创建前加载并合并文件，因此可以继续使用 Spring 的 `${变量名:默认值}` 占位符。XML 只是属性载体，不用于声明 Spring Bean；项目不再使用 Spring 应用 YAML 配置。

启动保护规则：

- 未启用 `dev`、`test` 或 `prod` 时拒绝启动；
- 同时启用两个或三个环境 Profile 时拒绝启动；
- Profile 与 `app.runtime.stage` 不一致时拒绝启动；
- `dev` 默认使用服务端模拟用户 `dev-user-001`，可通过 `DEV_AUTH_MODE=corporate` 切回公司统一认证；
- `test`、`prod` 固定使用 `corporate`，任何额外配置源尝试启用 `mock` 时拒绝启动；
- `prod` 固定设置 `app.qa.demo-mode=false`，不能被环境变量改回演示模式；
- 公司模型启用时必须同时提供已审核的真实 SSE 契约证明开关，否则启动失败；
- 生产数据库、Redis、公司模型地址和凭据没有代码内默认值，缺失时启动失败。

## 开发环境

Docker Compose 已默认启用 `dev`，可直接执行：

```bash
docker compose up --build
```

不使用 Compose 时，可按需设置：

```bash
export SPRING_PROFILES_ACTIVE=dev
export DEV_DB_URL='jdbc:mysql://127.0.0.1:3306/intelligent_qa?serverTimezone=UTC'
export DEV_DB_USERNAME='intelligent_qa'
export DEV_DB_PASSWORD='仅限本地的开发密码'
export DEV_AUTH_MODE='mock'
export DEV_MOCK_USER_ID='dev-user-001'
java -jar target/intelligent-qa-audit-service-0.1.0-SNAPSHOT.jar
```

开发配置默认启用模拟认证，所有 `/api/**` 请求都由服务端注入固定用户 `dev-user-001`，前端不需要传递用户编号或伪造认证 Header。可用 `DEV_MOCK_USER_ID` 更换本地身份；需要联调公司统一认证时设置 `DEV_AUTH_MODE=corporate`，此时未认证请求返回 `401`。模拟模式只用于本机开发，不能自动降级，也不会采信浏览器传入的身份。

开发配置默认启用明确标记的演示问答。接入开发环境真实依赖时，应设置 `DEV_QA_DEMO_MODE=false`，并补充对应的知识库、业务查询和模型配置。认证模式和问答演示模式是两个独立开关：关闭演示回答不会关闭模拟认证。

开发环境默认启用一期产品查询，也可显式设置 `DEV_BUSINESS_QUERY_ENABLED=true`。当前数据库必须已经由 Flyway 创建 `dws_product_info_d`，并由数据中台同步有效快照；未完成产品数据权限联调时不得据此宣称生产可用。

## 测试环境

```bash
export SPRING_PROFILES_ACTIVE=test
export TEST_DB_URL='由测试环境提供'
export TEST_DB_USERNAME='由测试环境密钥系统提供'
export TEST_DB_PASSWORD='由测试环境密钥系统提供'
export TEST_REDIS_HOST='由测试环境提供'
export TEST_COMPANY_MODEL_BASE_URL='由测试环境提供'
export TEST_COMPANY_MODEL_ENABLED=true
export TEST_COMPANY_MODEL_STREAM_CONTRACT_VERIFIED=true
export TEST_BUSINESS_QUERY_ENABLED=false
java -jar target/intelligent-qa-audit-service-0.1.0-SNAPSHOT.jar
```

测试环境默认关闭演示模式和公司模型，只有显式完成依赖配置后才启用真实调用。不得把生产地址或生产凭据复制到测试环境。

测试环境固定使用 `app.auth.mode=corporate`。测试替身应在测试代码中显式构造认证上下文，不得通过运行配置启用开发模拟用户。

## 生产环境

生产环境至少需要由部署平台或密钥系统注入：

```text
SPRING_PROFILES_ACTIVE=prod
PROD_DB_URL
PROD_DB_USERNAME
PROD_DB_PASSWORD
PROD_REDIS_HOST
PROD_REDIS_PASSWORD
PROD_COMPANY_MODEL_BASE_URL
PROD_COMPANY_MODEL_STREAM_CONTRACT_VERIFIED
PROD_BUSINESS_QUERY_ENABLED
```

生产环境固定启用公司模型和 Redis 配置、固定关闭演示模式，并强制使用公司统一认证。`PROD_COMPANY_MODEL_STREAM_CONTRACT_VERIFIED` 没有默认放行值；只有真实脱敏 SSE 样例已经固化为契约测试且通过责任人审查时才可设为 `true`。当前公司认证适配器、生产 Redis 事件适配器、知识库和受控业务查询仍属于发布阻塞项；配置文件分离和开发模拟用户不代表服务已经达到生产就绪。

## 公共调优变量

以下配置在三个环境中使用相同变量名，但必须由各环境的部署清单分别赋值和审查：

- `DB_POOL_MAX_SIZE`、`DB_POOL_MIN_IDLE` 和数据库超时；
- `HTTP_MAX_THREADS`；
- `QA_*` 回答长度、上下文、线程池和停止任务参数；
- `BUSINESS_QUERY_MAXIMUM_ROWS` 和 `BUSINESS_SEMANTIC_MODEL_VERSION`；
- `COMPANY_MODEL_CONNECT_TIMEOUT_MS`、`COMPANY_MODEL_READ_TIMEOUT_MS` 和请求大小上限；
- `REDIS_TIMEOUT`、`REDIS_CONNECT_TIMEOUT`。

不得依赖开发默认值推导测试或生产容量。生产参数必须基于 10 QPS 问答提交量、真实回答时长、SSE 并发数和依赖配额压测确定。

## 识别当前环境

通过受保护的 Actuator Info 端点查看：

```text
GET /actuator/info
```

返回的 `app.environment` 为 `DEVELOPMENT`、`TEST` 或 `PRODUCTION`，`app.active-profile` 为对应的 `dev`、`test` 或 `prod`。Micrometer 指标同时包含 `environment` 公共标签。端点和标签只暴露环境名称，不得加入地址、账号或凭据。

## 发布检查

1. 使用同一构建产物从测试环境晋级到生产环境。
2. 审查 Profile、变量前缀、密钥引用和外部地址，禁止跨环境混用。
3. 先验证 `/actuator/info` 环境标识，再执行数据库迁移和业务冒烟测试。
4. 确认日志、指标、Trace 和告警均带有正确环境标签。
5. 生产发布不得使用 `dev` 或 `test` Profile，也不得通过额外配置源覆盖 `app.runtime.stage`。
