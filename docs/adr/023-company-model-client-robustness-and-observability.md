# ADR-023：大模型调用客户端协议健壮性增强与可观测性建设

- 状态：已接受
- 日期：2026-10-14
- 负责人：服务团队
- 决策人：用户指定
- 关联需求：对齐已验证的 backend (Puyin QA) 大模型 HTTP 调用协议与容错经验；保持六边形架构与前端两步式 SSE 契约不变；完善流式生命周期可观测性
- 修订范围：细化与增强 ADR-004 中的流事件解析、错误映射与日志约定

## 背景

1. 公司大模型在实际运行态中存在协议细节偏差（例如部分网关下出现双重 `data:data:` 前缀、`agent_error` 业务事件、内层嵌套 JSON 回答文本以及 `<think>...</think>` 深度思考标签）。
2. `backend` 工程经过实际联调和验证，沉淀了针对大模型 HTTP 调用的容错与日志策略。为了大幅降低生产联调与测试成本，需要将这些经验直接集成到当前服务的 `CompanyModelApiClient` 与 `CompanyModelEventDecoder` 中。
3. 必须在维持工程现有六边形架构边界、领域模型纯粹性以及面向前端的两步式 SSE 契约（202 异步提交 + GET 独立订阅事件流）完全不变的前提下完成改动。

## 决策驱动因素

- **测试成本与上线确定性**：直接复用经过验证的协议容错规则，避免生产联调因网关格式微调反复返工。
- **用户体验与安全隔离**：模型内部思考链（`<think>`）属于中间推理，不得泄露给前端或普通用户。
- **架构一致性与分层约束**：改动严格限制在 `infrastructure.companymodel` 适配层内部，不影响领域层、工作流与应用编排。
- **生产可观测性与防刷屏**：流式逐字输出频率极高，需要精细化区分控制帧与正文帧日志，避免磁盘刷爆。

## 决策

1. **底层传输层配置（`CoreConfiguration`）**：
   - 专用 `companyModelRestTemplate` 采用 `SimpleClientHttpRequestFactory`，显式配置直连无代理（`Proxy.NO_PROXY`）并禁用 HTTP 重定向（`followRedirects=false`）。
   - 消息转换器显式配置支持 `text/event-stream` 的 `StringHttpMessageConverter`（UTF-8），保障流式逐行解析时不发生乱码。
2. **协议容错与规范化（`CompanyModelApiClient` & `CompanyModelEventDecoder`）**：
   - **双前缀清洗**：在行累加处理中循环剥离多余的 `data:` 前缀，自动清洗形如 `data:data: {...}` 的非标格式，并忽略空数据帧与 `[DONE]` 终止帧。
   - **嵌套 JSON 提取**：在 `extractInnerAnswer` 中自动识别并解析外层包装为 JSON 字符串的 `Answer` 字段，提取内层真正纯文本回答。
   - **消息 ID 早捕获**：不仅监听标准 `message` 帧，同步监听 `message_start` 帧的 `id` 属性，确保在首个回答字符产生前即可捕获 `message_id`，为实时停止生成（`stop_message`）提供依据。
3. **思考链标签流式过滤**：
   - 引入跨数据块的状态机缓冲区，对 `<think>...</think>` 深度思考标签执行静默流式过滤。
   - 思考标签内部文本不向 `consumer` 推送，当标签跨越网络数据块（Chunk）时保持未决状态，待闭合后恢复正文输出，彻底防止推理过程外泄。
4. **精确异常分级与状态码预检**：
   - 在流读取前检查响应 HTTP 状态码，若 $\ge 400$ 立即快速失败并抛出 `COMPANY_MODEL_HTTP_ERROR`，避免在错误 HTML/JSON 页面上解析 SSE。
   - 在事件解码时显式识别模型返回的 `agent_error` 与 `error` 事件，记录警告日志并映射为 `COMPANY_MODEL_AGENT_ERROR` 依赖不可用异常。
5. **分级可观测性与生命周期日志（参考 backend 模式）**：
   - **高频正文跳过**：高频正文帧（`message`）跳过逐帧打印；其余关键控制帧（`message_start`、`message_cost`、`agent_error` 等）单行打印 `[AGENT RAW] event={} data={}`，截断至 500 字符。
   - **请求摘要而不落正文**：调用入口打印 `COMPANY MODEL API CALL -> POST {url}`；`chat_query` 只记录 `queryChars` 与 `responseMode`、`pubAgentJump` 等非敏感字段摘要，**严禁打印 Query 提示词全文、`UserID` 与 `AppConversationID`**；平台 `AppConversationID`、`MessageID` 等标识统一用 `maskPlatformId` 只保留末 4 位遮蔽后记录。对齐 backend 的 `buildChatQueryBody` 摘要日志，满足 `REQUIREMENTS.md` §12.3「禁止在日志中记录完整问题、答案、Query、平台会话 ID」。
   - **终态指标汇总**：流结束时统一输出 `COMPANY MODEL SSE DONE: forwarded {N} events` 及包含脱敏后 `messageId` 和总字符数的汇总日志。

## 后果

- **正面影响**：
  - 全面兼容网关层前缀重复、格式嵌套等非标行为，生产对接测试返工风险降至最低。
  - 深度思考内容对调用方透明过滤，不污染业务答案；停止生成逻辑获取 ID 的时效性大幅提升。
  - 日志兼具排查诊断价值与防刷屏安全性，不泄露大报文正文。
- **负面影响**：
  - 解码器需维护跨数据块标签缓冲区与累积状态，增加了内存中轻量级状态管理的复杂度（已通过 100% 单元测试保障）。

## 验证

- 针对事件解码器编写了包含双前缀、思考标签跨块、嵌套 JSON、错误帧等场景的完整测试 [`CompanyModelEventDecoderTest`](../../src/test/java/com/acme/intelligentqa/infrastructure/companymodel/CompanyModelEventDecoderTest.java)。
- 契约与流程测试 [`CompanyModelApiClientTest`](../../src/test/java/com/acme/intelligentqa/infrastructure/companymodel/CompanyModelApiClientTest.java) 验证了停止生成、生命周期日志以及异常分级行为。
- 通过 `mvn -B -ntp clean verify` 全部门禁：Checkstyle、PMD/CPD（圈复杂度 $\le 10$）、SpotBugs、ArchUnit、JaCoCo、Maven Enforcer、`ChineseDocumentationArchitectureTest`、`HexagonalArchitectureTest` 及全部单元/集成测试无失败。

## 回滚或取代

- 若公司大模型网关升级为严格规范的 OpenAI 兼容标准接口，可编写新的标准模型适配器并通过配置切换，本决策仅约束 HiAgent 运行态适配器内部实现。
