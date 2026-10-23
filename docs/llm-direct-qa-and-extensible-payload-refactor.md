# 问答流水线编排上移至智能体中枢与多属性高扩展性解析改造设计方案

- **文档状态**：已批准 / 待实施
- **日期**：2026-10-23
- **涉及系统**：AI-empowerment（后端）、AI-empowerment-web（前端契约对齐）
- **参考实现**：`~/Downloads/code/backend`（Puyin QA 生产验证工程）

---

## 一、改造背景与架构合理性分析（Why）

### 1.1 问答流水线职责上移与架构合理性演进
在系统建设初期（ADR-003、ADR-012），问答场景（`DUAL_CHANNEL_QA`）被设计为由 `ScenarioPlanRegistry` 编排的本地五阶段串行工作流：
1. `QUESTION_UNDERSTANDING`（意图识别）
2. `KNOWLEDGE_RETRIEVAL`（知识库检索）
3. `BUSINESS_DATA_QUERY`（业务数据查询）
4. `EVIDENCE_RECONCILIATION`（证据对账）
5. `ANSWER_GENERATION`（大模型回答生成）

**架构演进与职责划分：**
- **生产智能体中枢（HiAgent）的能力定位**：在实际生产运行环境中，公司智能体中枢本身就是一个功能完善的 AI 流水线编排系统，原生集成了“多意图理解、语义路由、多知识库挂接检索与大模型深度推理”。
- **架构合理性论证（将流水线逻辑移动到大模型中枢编排）**：
  - 若在本地 Java 服务中继续硬编码维护一套简化的意图规则识别与前置检索，不仅属于重复造轮子，而且容易与云端智能体平台的推理逻辑发生双重割裂；
  - **将问答流水线逻辑（意图识别、知识匹配、业务路由）整体上移至大模型智能体平台的流水线编排中完成**，由平台自主编排检索与推理，充分发挥底座中枢的编排扩展性；
  - **本地 Java 服务（AI-empowerment）的职责聚焦**：严格遵循六边形架构，聚焦于**企业级应用门面、会话状态机、本地数据权限控制、流式事件增量转发、安全脱敏（ADR-024 约束）与聚合根持久化**。系统职责划分更加清晰、内聚。

### 1.2 本地五阶段工作流死锁阻断消除
除了架构合理性演进外，本次改造直接消除了阻断调用的系统死锁矛盾：
- **当 `qa.demo-mode=true` 时**：系统处于 Mock 演示模式，直接返回预置的演示答案，根本**不会真正调用**底层的大模型客户端 `CompanyModelApiClient.generate(...)`；
- **当 `qa.demo-mode=false` 时**：系统期望调用真实服务，但工作流首先进入第 1 步 `QuestionUnderstandingWorkflowNode`，该节点调用出站端口 `IntentRecognitionPort`。由于当前本地并未部署或配置独立的外部意图识别微服务，实现类 `IntentRecognitionAdapter` 会直接抛出：
  ```
  DependencyUnavailableException("INTENT_RECOGNITION_UNCONFIGURED", "intent recognition adapter is not configured")
  ```
  导致请求在第一步就直接中断报错，后续步骤（包括最后一步调用真实的 `CompanyModelApiClient`）永远无法被执行。
- **解耦消除阻断**：流水线职责上移至平台后，本地工作流精简为直通 `ANSWER_GENERATION`，彻底解除了外部意图服务缺失带来的死锁阻断。

### 1.3 大模型输出多属性解析与未来扩展性诉求
智能体平台在完成流水线问答后，返回的数据对象不仅包含正文 `answer`，还包含重要业务元数据：
- **`rec_questions`**：智能体平台流水线根据回答内容推荐的后续追问列表（如 `["如何办理该业务？", "需要什么材料？"]`）；
- **`time_cost`**：智能体平台内部执行总耗时（毫秒数），用于服务端推理性能度量；
- **未知/未来扩展字段**：随着智能体平台算法与流水线插件的迭代，未来可能会新增 `sources`（知识引用出处）、`confidence`（置信度得分）、`token_usage`（Token 消耗）、`audit_status`（合规审计状态）等属性。

**设计要求：**
- 必须能够在流式传输协议下，优雅反序列化这些结构化属性，并将推荐问题与耗时实时交付给前端；
- 必须具备**极高的前向兼容与扩展能力**：未来外部模型平台新增任何新字段时，Java 代码无需每次都改动 DTO、重新编译和发版，即可自动捕获、透传并持久化。

---

## 二、总体架构与技术方案（What & How）

### 2.1 问答执行流对比

#### 改造前（本地多阶段串行，容易阻塞死锁）：
```
[用户提问]
   │
   ▼
[ScenarioPlanRegistry: DUAL_CHANNEL_QA]
   ├── 1. QUESTION_UNDERSTANDING (IntentRecognitionAdapter 抛错 INTENT_RECOGNITION_UNCONFIGURED) ──✖ 死锁
   ├── 2. KNOWLEDGE_RETRIEVAL
   ├── 3. BUSINESS_DATA_QUERY
   ├── 4. EVIDENCE_RECONCILIATION
   └── 5. ANSWER_GENERATION (CompanyModelApiClient 永远无法被执行)
```

#### 改造后（流水线职责上移至智能体中枢编排）：
```
[用户提问]
   │
   ▼
[ScenarioPlanRegistry: 执行计划演进]
   └── 直通 ANSWER_GENERATION (调用 CompanyModelApiClient)
          │
          ▼
   [CompanyModelApiClient 组装结构化 Query]
   {"UserID": "...", "AppConversationID": "...", "Query": "{\"mode\":\"auto\",\"content\":\"用户提问\",\"stream\":\"0\"}"}
          │ POST /chat_query (SSE 流式连接，ResponseMode=streaming)
          ▼
   [HiAgent 云端流水线自主完成意图识别、知识匹配与生成]
          │
          ▼
   [CompanyModelEventDecoder 逐帧流式解码]
          ├── ① 正文增量帧：流式输出 answer 文本分片，实时过滤 <think> 思考链，下发前端打字机
          ├── ② 结构化结算帧：解析包含 answer、rec_questions、time_cost 的 JSON 对象
          └── ③ 动态扩展池：通过 @JsonAnySetter 自动装配所有未知的新增扩展字段
          │
          ▼
   [SSE 事件广播 & 领域模型更新]
          ├── 向前端推送 metadata 事件（包含耗时 time_cost、推荐追问列表）
          └── 持久化至 AnswerSnapshot 领域快照与审计日志
```

### 2.2 请求协议精准对齐（backend Query 结构体）
严格对齐参考工程 `backend` 中的 `DefaultAgentClient.buildChatQueryBody`：
大模型平台的 `/chat_query` 接口要求 `Query` 字段为一段被序列化的 JSON 结构体字符串：
```json
{
  "mode": "auto",
  "content": "用户的真实提问内容",
  "stream": "0"
}
```
- **`mode=auto`**：指示平台启用智能体模式（由平台流水线自主执行知识库检索与意图识别）；
- **`stream` 字段语义（深度思考开关）**：
  - 该字段对应平台的**深度思考/推理模式（DeepThink / Reasoning）**；
  - 默认为 `"0"`（关闭深度思考，常规流式响应，避免额外的 `<think>` 标签输出及耗时增加）；
  - 仅在业务或前端显式指定开启深度思考时置为 `"1"`；
- **HTTP 传输层流式保障**：由请求体顶层字段 `"ResponseMode": "streaming"` 驱动，确保底层建立稳定长连接。

### 2.3 响应解码与高扩展性对象模型（Extensible Model Payload）
利用 Jackson 的动态属性机制设计 `CompanyModelPayload`：
```java
public class CompanyModelPayload {
    private String answer;
    private List<String> recQuestions;
    private Integer timeCost;
    
    /** 动态捕获所有未来新增的未知扩展属性 */
    private final Map<String, Object> extraFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void putExtraField(String key, Object value) {
        this.extraFields.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getExtraFields() {
        return this.extraFields;
    }
}
```

### 2.4 流式与 JSON 序列化的统一处理机制
针对流式传输与 JSON 解析的天然差异，采用两阶段解析策略：
1. **增量流式阶段（逐 chunk）**：
   - 模型在吐字过程中，通过 SSE `message` / `tool_message` 逐帧下发；
   - 解码器即时截取增量，过滤 `<think>` 标签，把纯文本 `delta` 推给前端打字机。
2. **结构化结算阶段（帧级别反序列化）**：
   - 当模型返回包含结构体对象的事件帧（或在 `message` / `message_end` 帧内带有嵌套 JSON 字符串）时，解码器调用 `objectMapper.readValue(..., CompanyModelPayload.class)`；
   - 提取出 `rec_questions`、`time_cost` 以及所有未知名属性；
   - **耗时分层可观测**：`time_cost` 记录为平台服务端执行耗时；客户端从发起请求到收到终止帧的本地时间差（`Duration.between(startTime, endTime).toMillis()`）作为端到端可观测性日志记录，不混淆两类指标定义。

---

## 三、改动文件清单及原因说明

按照六边形分层架构原则，本次改造需要调整的文件清单如下：

| 序号 | 文件路径 | 层级 | 变更类型 | 改动内容与原因 |
| :--- | :--- | :--- | :--- | :--- |
| **1** | `com.acme.intelligentqa.infrastructure.companymodel.CompanyModelPayload` | 基础设施层 | **新增** | **创建可无限扩展的模型返回载体**。<br>显式声明 `answer`、`recQuestions`（映射 `rec_questions`）、`timeCost`（映射 `time_cost`）；配置 `@JsonAnySetter`/`@JsonAnyGetter` 动态收集未知扩展属性。 |
| **2** | `com.acme.intelligentqa.infrastructure.companymodel.CompanyModelEventDecoder` | 基础设施层 | **修改** | **增强结构体反序列化与多字段解析**。<br>在现有抽取纯正文的基础上，升级内层 JSON 识别逻辑，当发现结构体包含推荐问题、耗时或扩展属性时，完整解析并保存在当前流上下文中，提供给外部消费。 |
| **3** | `com.acme.intelligentqa.infrastructure.companymodel.CompanyModelApiClient` | 基础设施层 | **修改** | **对齐 Query 结构化上送与思考开关控制**。<br>1. 将 `Query` 按 backend 格式序列化为 `{"mode":"auto","content":...,"stream":"0"}`；<br>2. 记录端到端耗时用于日志可观测；<br>3. 将解码器解析出的推荐问题列表、耗时及动态字段打包返回。 |
| **4** | `com.acme.intelligentqa.domain.port.out.LanguageModelPort` | 领域端口层 | **修改** | **扩展领域模型出站契约**。<br>在 `GenerationResult` 中增加 `recQuestions`、`timeCost` 与 `extraFields` 字段，允许基础设施层将大模型输出的这些丰富元数据安全传递到应用层。 |
| **5** | `com.acme.intelligentqa.application.workflow.ScenarioPlanRegistry` | 应用工作流层 | **修改** | **调整执行计划，流水线编排上移至大模型**。<br>将问答场景的执行节点精简为直通 `ANSWER_GENERATION`（回答生成节点），彻底避免未配置本地意图识别时抛出的 `INTENT_RECOGNITION_UNCONFIGURED` 异常。 |
| **6** | `com.acme.intelligentqa.application.workflow.node.AnswerGenerationWorkflowNode` | 应用工作流层 | **修改** | **接收并沉淀模型扩展属性**。<br>从 `GenerationResult` 中取出 `recQuestions` 与 `timeCost`，写入 `QuestionWorkflowContext`，并在回答完成时写入领域聚合快照。 |
| **7** | `com.acme.intelligentqa.domain.model.AnswerSnapshot` | 领域模型层 | **修改** | **扩展领域聚合快照字段**。<br>在聚合快照 `AnswerSnapshot` 中增加 `recQuestions` 和 `timeCost` 字段与访问方法，支持持久化存储与历史会话回显。 |
| **8** | `com.acme.intelligentqa.application.service.AnswerCompletionHandler` | 应用服务层 | **新增** | **回答完成收敛协调器**。<br>集中协调问答落盘、元数据发布与完成事件广播，降低服务圈复杂度，满足 PMD 门禁要求。 |
| **9** | `com.acme.intelligentqa.application.service.QuestionAnswerService` | 应用服务层 | **修改** | **前端 SSE 流式通知与协同演进**。<br>在回答完成与元数据推送时携带推荐问题与耗时，协同前端 `AI-empowerment-web` 契约演进。 |
| **10** | `CompanyModelEventDecoderTest` / `CompanyModelApiClientTest` | 测试工程 | **新增/修改** | **补充单元测试与回归门禁**。<br>验证带有 `rec_questions`、`time_cost` 及任意额外字段的 SSE 帧解析、时间统计与异常兜底，确保测试覆盖率与代码门禁 100% 通过。 |

---

## 四、可观测性、安全性与非功能设计

1. **敏感信息与日志脱敏（严格延续 ADR-024 约束）**：
   - 严禁在日志中输出完整问题、答案、Query 提示词正文以及 `UserID`；
   - 打印请求时仅记录字符总数与摘要参数，平台会话与消息 ID 统一遮蔽后输出；
   - 动态扩展字段在打印时同样需做非空与键名审计，不输出过大或敏感的扩展字段内容。
2. **容错与前向兼容性**：
   - 若模型某次回答未输出 `rec_questions`，默认返回空列表 `Collections.emptyList()`，不产生空指针；
   - 若模型未返回 `time_cost`，保持为 `null`，端到端耗时仅记录于可观测性日志中；
   - 若模型返回了未知格式的新字段，由 `@JsonAnySetter` 统一捕获为 `Map<String, Object>`，保证系统不会因字段不匹配而反序列化失败。
3. **前端契约协同**：
   - 推荐追问列表（`rec_questions`）将在前端 `AI-empowerment-web` 扩展 `StreamEventType`（或通过 `metadata` 载荷扩展）后对齐渲染，后端保证在契约未冻结前不推送非法未知事件破坏前端状态机。

---

## 五、验证与交付门禁

按照项目 `AGENTS.md` 规范，所有改动必须通过以下严格质量门禁：
1. **中文注释与文档**：所有新类、方法、字段必须具备规范的中文 TSDoc/Javadoc 注释，满足 `ChineseDocumentationArchitectureTest` 门禁；
2. **代码风格与静态分析**：通过 Checkstyle、PMD/CPD（圈复杂度 $\le 10$）、SpotBugs 检查；
3. **架构与分层门禁**：通过 ArchUnit 与 `HexagonalArchitectureTest` 检查，禁止领域层反向依赖外部库；
4. **全量构建验证**：
   ```bash
   mvn -B -ntp clean verify
   ```
   只有全部门禁全部通过，才视为实施完成。
