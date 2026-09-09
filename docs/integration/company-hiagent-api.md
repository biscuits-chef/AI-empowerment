# 公司 HiAgent 运行态 API 接入说明

## 依据与边界

本适配器依据《HiAgent 运行态 API 文档》实现。目标地址由 `COMPANY_MODEL_BASE_URL` 配置，值应为目标智能体 Single Chat Server 的 `/api/v1` 前缀，例如：

```text
http://agent-chat-server:6789/api/v1
```

生产地址、TLS、网关鉴权和网络区域未在当前文档中给出，必须由公司平台负责人确认。任何凭据只能由密钥管理系统注入。

## 调用流程

### 1. 创建平台会话

```http
POST {baseUrl}/create_conversation
Content-Type: application/json

{
  "Inputs": {},
  "UserID": "user-1"
}
```

`Inputs` 为可选 `map<string,string>`，实际必填变量由所发布智能体配置决定。`UserID` 必填、应用内唯一，文档要求长度 1～20。

必须从响应中取得：

```text
Conversation.AppConversationID
```

未返回该字段视为依赖失败，不能继续聊天请求。

### 2. 流式聊天

```http
POST {baseUrl}/chat_query
Content-Type: application/json
Accept: text/event-stream

{
  "UserID": "user-1",
  "AppConversationID": "company-conversation-id",
  "Query": "由本系统组装的历史、意图与双通道证据",
  "ResponseMode": "streaming",
  "PubAgentJump": false
}
```

`QueryExtends.Files` 仅在需要向模型平台传文件时使用，第一阶段问答当前不发送文件。`PubAgentJump=false` 避免输出内部 Agent 调用消息。

响应为 SSE `data: json`。平台文档未给出流事件 JSON 完整结构；消息详情接口中出现的 `AnswerInfo.Answer` 不能作为流事件字段路径的证据。当前代码不再递归猜测大小写变体或嵌套字段，生产和联调环境默认因 `stream-contract-verified=false` 拒绝启用模型适配器。只有取得真实脱敏样例、固化正常结束与错误帧契约测试并经责任人确认后，才可把对应环境变量设置为 `true`；届时解码器也必须同步收紧到获批的精确事件类型和字段路径。

为避免异常依赖响应造成无界内存占用，SSE 单行和单事件上限均使用 `COMPANY_MODEL_MAX_REQUEST_CHARACTERS`，单次响应累计字符上限为该值的四倍；任一上限触发时在 JSON 解析前以 `COMPANY_MODEL_RESPONSE_TOO_LARGE` 失败关闭。该安全上限不是平台容量承诺，真实联调后仍须由平台和 SRE 共同确认并通过配置评审。

### 3. 停止消息

当用户停止已进入模型生成的回答时，系统从流事件捕获精确 `MessageID` 并调用：

```http
POST {baseUrl}/stop_message
Content-Type: application/json

{
  "UserID": "user-1",
  "MessageID": "message-id"
}
```

字段名及大小写严格按公司文档发送。停止请求采用持久任务、有界重试和租约抢占；没有取得 `MessageID` 时不会猜测或使用本系统回答 ID 代替，而是进入明确的 `MODEL_MESSAGE_ID_UNAVAILABLE` 失败状态。

## 重新生成和上下文

本系统的重新生成是把原问题作为新的完整问答轮次重新发起：创建新的问题和回答记录，继承原问题的不可变附件引用，重新执行意图识别、知识库检索、数据库查询和证据对账，因此不直接调用平台 `/query_again`。每次模型生成创建独立平台会话，并在 `Query` 中携带由本系统裁剪和审核的上下文，以本系统 GoldenDB 记录作为事实源。

如果后续决定复用平台会话，必须先设计内部会话到 `AppConversationID` 的持久化映射、并发创建幂等、生命周期和删除对账，并通过新 ADR 批准。

## 配置

| 环境变量 | 默认值 | 说明 |
|---|---:|---|
| `DEV_COMPANY_MODEL_ENABLED` | `false` | 开发环境是否启用公司模型适配器 |
| `DEV_COMPANY_MODEL_STREAM_CONTRACT_VERIFIED` | `false` | 开发联调的真实 SSE 契约证据门禁 |
| `DEV_COMPANY_MODEL_BASE_URL` | 空 | 开发环境 `/api/v1` 前缀 |
| `TEST_COMPANY_MODEL_ENABLED` | `false` | 测试环境是否启用公司模型适配器 |
| `TEST_COMPANY_MODEL_STREAM_CONTRACT_VERIFIED` | `false` | 测试环境的真实 SSE 契约证据门禁 |
| `TEST_COMPANY_MODEL_BASE_URL` | 空 | 测试环境 `/api/v1` 前缀 |
| `PROD_COMPANY_MODEL_BASE_URL` | 无，必填 | 生产环境 `/api/v1` 前缀；生产固定启用真实模型且关闭演示模式 |
| `PROD_COMPANY_MODEL_STREAM_CONTRACT_VERIFIED` | `false` | 生产真实 SSE 契约证据门禁；未明确设为 `true` 时启动失败 |
| `COMPANY_MODEL_CONNECT_TIMEOUT_MS` | `3000` | 建连超时 |
| `COMPANY_MODEL_READ_TIMEOUT_MS` | `120000` | 流读取超时 |
| `COMPANY_MODEL_MAX_REQUEST_CHARACTERS` | `100000` | 发往平台的最大 Query 字符数，同时作为单个 SSE 行/事件的安全上限；累计响应上限为其四倍 |

## 联调前必须补齐

- 实际服务地址、环境和网络访问方式。
- 平台是否有文档外的鉴权 Header、签名、证书或网关要求。
- 目标智能体必填 `Inputs` 和发布版本。
- 真实脱敏 SSE 样例，包括增量/累计答案、正常结束、模型错误、平台错误、`MessageID`、`TaskID` 和用量。
- 将脱敏样例作为契约测试资源，证明精确事件类型、字段路径和结束条件后，方可批准对应环境的 `*_STREAM_CONTRACT_VERIFIED=true`。
- 确认 `MessageID` 在真实流事件中的准确字段路径，并验证 `/stop_message` 成功、重复停止、已结束消息、超时和非 2xx 语义。
- HTTP 状态码、业务错误码、限流行为、最大连接数和重试约束。
- 模型输入输出留存、训练使用、内容安全和审计要求。
