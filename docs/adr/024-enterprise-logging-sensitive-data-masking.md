# ADR-024：企业级日志敏感数据脱敏与安全防护规范

- 状态：已接受
- 日期：2026-10-14
- 负责人：服务团队
- 决策人：用户指定
- 关联需求：对系统日志中的个人敏感数据（身份证、银行卡号、手机号、姓名、居住地址、邮箱地址）进行标准化脱敏处理，杜绝敏感数据在日志文件及控制台明文暴露

## 背景

1. 严禁在日志中打印凭据、个人敏感数据或完整不可信 Payload。
2. 在大模型交互、客户问答与业务检索过程中，日志可能记录包含用户身份与联系方式的报文。若仅依赖开发人员在每次编码时手动遮蔽，极易因疏忽造成敏感信息明文落盘。
3. 必须建立既有**框架级无感全局兜底**、又有**应用层主动脱敏接口**的双层日志防护架构，覆盖身份证号、银行卡号、手机号码、中文姓名、居住地址和电子邮箱等全部高风险类别。

## 决策驱动因素

- **零遗漏合规安全（Zero-Leakage Compliance）**：框架层必须能够拦截所有通过 SLF4J / Logback 输出的日志，提供全局托底，杜绝单点人为疏漏。
- **业务排查可读性（Diagnosability）**：脱敏不能简单将整行替换为 `***`，必须遵循保留安全特征码（如卡号首尾、手机号运营商与尾号、发卡地前缀与域名）的原则，兼顾问题排查与审计追溯。
- **性能与可维护性**：脱敏在日志输出高频主干路径上运行，正则表达式必须防范灾难性回溯（ReDoS），算法拆分必须遵循极简内聚原则（单方法圈复杂度 $\le 5$）。

## 决策

1. **双层防护体系（Double Shielding Architecture）**：
   - **Logback 框架级全局兜底**：实现 Logback 自定义消息转换器 [`SensitiveDataConverter`](../../src/main/java/com/acme/intelligentqa/common/log/SensitiveDataConverter.java)，并在 [`logback-spring.xml`](../../src/main/resources/logback-spring.xml) 中配置 `<conversionRule conversionWord="msg" .../>`、`m` 与 `message`。系统内所有通过 SLF4J 输出到控制台（CONSOLE）、文件（FILE）和错误日志（ERROR_FILE）的消息，无论由框架、第三方库还是业务代码打印，均自动经过脱敏过滤后才写出。
   - **应用层主动脱敏工具**：提供核心脱敏引擎工具类 [`SensitiveDataMasker`](../../src/main/java/com/acme/intelligentqa/common/log/SensitiveDataMasker.java)，并在大模型日志工具 [`CompanyModelLogText`](../../src/main/java/com/acme/intelligentqa/infrastructure/companymodel/CompanyModelLogText.java) 与客户端 `logRequestBody` 中显式调用，在报文序列化及字符截断前即完成脱敏。

2. **六类个人敏感数据 + 凭据令牌兜底的标准化脱敏策略**：
   - **身份证号（ID Card）**：
     - 18 位二代居民身份证（含末位 X）：保留前 6 位（发卡地区）与后 4 位，中间 8 位出生日期替换为 `********`。例如：`110101199003072345` $\rightarrow$ `110101********2345`。
     - 15 位一代居民身份证：保留前 6 位与后 3 位，中间 6 位替换为 `******`。例如：`110101900307234` $\rightarrow$ `110101******234`。
   - **银行卡号（Bank Card）**：
     - 16 至 19 位纯数字银行卡号：保留前 6 位（BIN 码）与后 4 位（尾号），中间统一替换为 `******`。例如：`6222021234567890` $\rightarrow$ `622202******7890`。
   - **手机号码（Mobile Phone）**：
     - 11 位中国大陆手机号码（支持可选 `+86` 或 `86-` 前缀）：保留前 3 位与后 4 位，中间 4 位替换为 `****`。例如：`13812345678` $\rightarrow$ `138****5678`、`+86 13987654321` $\rightarrow$ `+86 139****4321`。
   - **电子邮箱（Email Address）**：
     - 用户名前缀部分：长度 $\le 2$ 时保留首字符加 `***`；长度 $\ge 3$ 时保留首尾字符、中间替换为 `***`；域名后缀完整保留以供服务排查。例如：`zhangsan@example.com` $\rightarrow$ `z***n@example.com`、`a@test.com` $\rightarrow$ `a***@test.com`。
   - **姓名（Name）**：
     - 通过 JSON/键值对属性名（`name`, `userName`, `realName`, `customerName`, `clientName`, `姓名`, `持卡人`, `联系人`, `经办人` 等）及显式前缀匹配：
       - 2 字中文名：隐去名，如 `张三` $\rightarrow$ `张*`；
       - 3 字中文名：隐去中间字，如 `李四五` $\rightarrow$ `李*五`；
       - 4 字及以上复姓或少数民族姓名：保留首尾字符，中间隐去，如 `欧阳六七` $\rightarrow$ `欧**七`。
   - **居住地址（Residential Address）**：
     - 识别结构化属性（`address`, `addr`, `residentialAddress`, `居住地址` 等）及中文省市区县道路文本模式；
     - 策略：保留行政区划（如 `北京市海淀区`、`上海市浦东新区`、`江苏省南京市鼓楼区`），将其后的路、街、号、楼、室等具体居住敏感细节统一替换为 `****`。例如：`北京市海淀区中关村南大街1号院` $\rightarrow$ `北京市海淀区****`。
   - **凭据与令牌兜底**：
     - 键值对或 JSON 中出现的 `token`, `password`, `secret`, `authorization`, `credential` 等统一替换为 `***`；`Bearer` 令牌遮蔽。

3. **执行时序与防干扰机制**：
   - 先执行身份证识别再执行银行卡号识别，避免 18 位身份证被卡号规则误拦截；
   - 自由文本地址正则显式限定不可越过引号及标点，且若已被字段级脱敏（含 `****`）则直接跳过，防止嵌套二次破坏。

## 后果

- **正面影响**：
  - 生产系统具备了金融级完备的日志脱敏能力，完全满足安全合规与审计检查要求。
  - 框架级全局生效机制彻底消除了因新增业务模块遗漏脱敏代码导致数据泄露的安全隐患。
  - 所有方法圈复杂度均 $\le 5$（门禁上限为 10），保持代码极致整洁易读。
- **负面影响**：
  - 每条输出到 Appender 的日志均需经过轻量级正则流水线，对超高吞吐日志场景存在极微弱的 CPU 开销（经压测在单条日志纳秒级，不构成性能瓶颈）。

## 验证

- 新增单元测试 [`SensitiveDataMaskerTest`](../../src/test/java/com/acme/intelligentqa/common/log/SensitiveDataMaskerTest.java)，全面覆盖 6 类敏感数据的单项、边界值、异常值以及包含全部 6 类数据的复杂嵌套 JSON Payload，并包含不含地址的普通业务文本反向用例。
- 新增集成测试 [`SensitiveDataConverterTest`](../../src/test/java/com/acme/intelligentqa/common/log/SensitiveDataConverterTest.java)，验证 Logback 事件管道与自定义转换器的正确对接。
- 通过 `mvn -B -ntp clean verify` 全部门禁：Checkstyle 0 违规、PMD 0 违规、SpotBugs 0 bug、ArchUnit、JaCoCo 覆盖率门禁、Maven Enforcer 均通过；`ChineseDocumentationArchitectureTest` 与 `HexagonalArchitectureTest` 100% 通过。

## 回滚或取代

- 若未来基础设施团队提供服务网格或统一集中式日志采集端脱敏 Agent，可通过在 `logback-spring.xml` 中移除 `<conversionRule>` 配置一键平滑退化，核心代码无强耦合。
