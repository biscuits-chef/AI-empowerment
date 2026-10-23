# ADR-025：问答流水线编排上移至智能体中枢与多属性高扩展性解析改造

- 状态：已接受
- 日期：2026-10-23
- 负责人：服务团队
- 决策人：用户指定
- 关联需求：对齐参考工程 `backend` (Puyin QA) 生产验证调用链路；将问答流水线逻辑由本地硬编码迁移至专业智能体平台（HiAgent）统一编排；消除本地未配置意图识别微服务引发的链路阻断；支持大模型返回对象的多属性解析（`rec_questions`、`time_cost`、`answer`）与未来属性无限扩展能力。
- 详细设计：参见 [问答流程纯大模型直连与多属性高扩展性解析改造设计方案](../llm-direct-qa-and-extensible-payload-refactor.md)

---

## 一、背景与问题

1. **流水线编排演进与架构合理性**：
   - 在系统建设初期（ADR-003、ADR-012），系统在本地 Java 服务中设计了多阶段串行工作流（意图识别、知识库检索、业务数据查询、确定性对账、回答生成）。
   - 随着公司 AI 中枢与智能体平台（HiAgent）的成熟演进，**真实的生产智能体本身即是具备“意图识别、路由决策、多知识库检索与工具链调度”能力的完整智能编排中枢**。
   - 若继续在本地 Java 层维护一套简化易错的前置检索与规则识别，不仅造成与底层智能体平台的功能重复建设与逻辑割裂，更限制了底层流水线自主调优和知识库挂接的能力。因此，**将问答的核心逻辑真正移动到大模型智能体的流水线编排中完成**，是符合云原生 AI 架构演进的合理方向。
2. **本地前置节点死锁阻断**：
   - 系统配置 `qa.demo-mode=false` 时，原工作流首先进入本地意图识别节点 `QuestionUnderstandingWorkflowNode`。由于当前本地并未部署或配置独立的外部意图识别微服务，实现类抛出 `DependencyUnavailableException("INTENT_RECOGNITION_UNCONFIGURED")`，导致请求在第 1 步直接中断，真正的大模型调用 `CompanyModelApiClient.generate` 永远无法执行；而在 `demo-mode=true` 时又直接返回 Mock 答案，绕过了真实大模型调用。
3. **输出多属性与扩展性诉求**：
   - 智能体平台在完成流水线问答后，返回的数据不仅包含回答正文 `answer`，还包含推荐追问列表 `rec_questions`、服务端执行耗时 `time_cost` 等重要业务元数据，且未来可能随平台升级新增 `sources`、`confidence`、`token_usage` 等属性。原有仅提取单一纯文本 `answer` 的模式无法满足业务需求，必须具备高扩展性解析能力。

---

## 二、决策驱动因素

- **流水线职责清晰化（Separation of Concerns）**：将意图识别、知识检索与推理决策完整交还给底座专业智能体平台（HiAgent 流水线），本地工程聚焦于企业级门面、会话状态机、安全脱敏与审计持久化。
- **解除非 Demo 模式阻断**：精简本地执行计划，打通用户提问到真实生产智能体平台的全链路调用。
- **精准协议对齐**：准确对齐参考工程 `backend` 中的结构化 `Query` 协议，清晰区分“深度思考（DeepThink）”开关与“HTTP 传输层流式响应”。
- **高扩展性与向前兼容（Forward Compatibility）**：采用 Jackson 动态属性机制，模型平台新增任何未知字段时无需修改代码或重新发版。
- **架构纯粹性**：严格遵循六边形架构，模型平台 DTO 变动不污染领域核心模型。

---

## 三、架构决策

1. **场景工作流编排改造**：
   - 在 `ScenarioPlanRegistry` 中，将问答执行计划调整为直通回答生成节点（`ANSWER_GENERATION`），由底座智能体平台的流水线全权负责检索与生成，跳过本地未配置的冗余前置节点。
2. **结构化 Query 精准组装**：
   - 在 `CompanyModelApiClient` 中，将用户提问包装为 JSON 结构体字符串上送至 `Query` 字段：
     ```json
     {"mode":"auto","content":"<用户提问>","stream":"0"}
     ```
     - `mode=auto`：指示平台启用智能体模式（由平台内部流水线完成知识检索与意图识别）；
     - `stream`：对应智能体平台的**深度思考开关**（`"1"` 开启思考，`"0"` 默认关闭思考，避免产生非预期推理链并增加耗时）；
     - 顶层参数保持 `"ResponseMode": "streaming"` 确保 HTTP 传输层流式。
3. **高扩展性返回模型（`CompanyModelPayload`）**：
   - 显式定义 `answer`、`recQuestions`（映射 `rec_questions`）、`timeCost`（映射 `time_cost`）；
   - 使用 Jackson 的 `@JsonAnySetter` 和 `@JsonAnyGetter` 建立 `extraFields` 动态池，自动装配任何未知新增字段。
4. **SSE 解码与耗时分级可观测性**：
   - `CompanyModelEventDecoder` 在增量推送正文的同时，对携带结构体的事件帧反序列化为 `CompanyModelPayload`；
   - 明确 `time_cost` 为智能体服务端推理耗时；若模型帧未返回，客户端端到端耗时仅作为监控和可观测性补充，不混淆两类指标语义；
   - 提取的 `rec_questions` 与 `time_cost` 写入领域模型，并在完成时通过 SSE 广播。

---

## 四、改动文件清单

- `com.acme.intelligentqa.infrastructure.companymodel.CompanyModelPayload`（新增）：高扩展性模型响应 DTO；
- `com.acme.intelligentqa.infrastructure.companymodel.CompanyModelEventDecoder`（修改）：增强 JSON 结构体与扩展字段解析；
- `com.acme.intelligentqa.infrastructure.companymodel.CompanyModelApiClient`（修改）：Query 结构化组装与深度思考参数适配；
- `com.acme.intelligentqa.domain.port.out.LanguageModelPort`（修改）：`GenerationResult` 扩展 `recQuestions`、`timeCost` 和 `extraFields`；
- `com.acme.intelligentqa.application.workflow.ScenarioPlanRegistry`（修改）：执行计划调整为直通回答生成节点；
- `com.acme.intelligentqa.application.workflow.node.AnswerGenerationWorkflowNode`（修改）：将模型扩展属性存入工作流上下文；
- `com.acme.intelligentqa.domain.model.AnswerSnapshot`（修改）：聚合根快照补充推荐问题与耗时属性；
- `com.acme.intelligentqa.application.service.AnswerCompletionHandler`（新增）：集中协调问答落盘、元数据发布与完成事件广播，降低服务圈复杂度；
- `com.acme.intelligentqa.application.service.QuestionAnswerService`（修改）：流式事件广播增加推荐问题与耗时，协同前端契约演进；
- 测试用例与架构门禁测试（修改/新增）：确保 100% 门禁通过。

---

## 五、验证与门禁标准

1. 单元与集成测试：覆盖带推荐问题、耗时、未知字段的模型帧反序列化，以及直连大模型的全链路工作流；
2. 中文 Javadoc 门禁：所有新加与修改的类、方法、字段必须有完整规范的中文注释；
3. 执行 `mvn -B -ntp clean verify` 全部门禁检查通过。
