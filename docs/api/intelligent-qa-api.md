# 智能问答 API 契约

基础路径：`/api/v1`。所有接口都要求认证，用户身份来自服务端 `Principal`。

接口中的会话、消息、回答和文件 `id` 仍是不可枚举的 UUID，并映射到数据库 `public_id` 唯一列。数据库统一的 `id BIGINT AUTO_INCREMENT` 仅为内部物理主键，不通过 API、SSE 或游标暴露。

接口名、Header、Path/Query/Body 入参、成功出参、错误码及第三方调用契约的评审基线见[一期技术方案 V2.2 第 17 节](../technical-solution-design-v2.md#17-api-详细契约)。本文保留面向开发联调的精简说明，两处不一致时必须先发起契约评审，不能自行选择。

## Agent 约定

系统不提供 `/agents` 或 `/api/v1/agents`。Agent 的名称、排序、图标和前端开放状态由前端本地配置；选择器只在新建会话首次提问前展示。首次提问必须发送稳定的 `agentType`，后端独立执行白名单、开放状态和权限校验并把类型固化到会话。后续提问省略该字段，由后端从会话读取。第一阶段唯一可执行类型为 `SMART_DATA`。

## 会话

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/chats?cursor=...&limit=50` | 按稳定游标查询当前用户会话；首页不传 `cursor`，`limit` 范围为 1～100 |
| `GET` | `/chats/{chatId}` | 查询会话详情 |
| `POST` | `/chats/{chatId}/rename` | 修改名称 |
| `POST` | `/chats/{chatId}/deletion` | 逻辑删除会话；存在活动回答时返回 `409` |
| `GET` | `/chats/{chatId}/messages?limit=100` | 查询历史消息 |

会话列表响应为 `{"items":[],"nextCursor":"...","hasMore":true}`。`cursor` 是服务端不透明值，客户端只能原样回传；不得解析、修改或替换为页码。前端以该接口分批取数，并用虚拟滚动限制长列表 DOM 节点。

点击“新建聊天”只建立前端空白草稿。第一次有效提问时，前端调用统一接口 `POST /questions/submission` 并传 `chatId=null`，后端在同一事务中创建会话、问题和回答；用户未提问即退出时不创建空会话。后续追问仍调用同一接口，但携带已有 `chatId`。

助手历史消息包含 `answerStatus`、`executionEvents` 和 `artifacts`。前端刷新后必须恢复真实状态、已完成执行阶段、人工复核提示和来源/产物；追问回答的状态为 `NEEDS_CLARIFICATION`，不能被当作可赞踩或可重新生成的正常答案。历史 `attachments` 字段只用于兼容开发期旧数据，一期不会产生新的非空附件列表。

当会话中存在 `PENDING`、`RETRIEVING`、`QUERYING`、`GENERATING` 或 `CANCEL_REQUESTED` 回答时，删除返回：

```json
{
  "status": 409,
  "title": "Conversation is active",
  "detail": "会话正在执行，请停止后删除"
}
```

客户端必须展示该固定提示，不能自动停止回答；回答到达明确终态后由用户重新发起删除。

历史用户消息附件兼容示例：

```json
{
  "id": "问题 UUID",
  "answerId": null,
  "answerStatus": null,
  "role": "USER",
  "content": "查询附件中的产品信息",
  "createdAt": "2026-09-02T00:00:00Z",
  "attachments": [
    {
      "fileId": "文件 UUID",
      "name": "产品编号.xlsx",
      "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "sizeBytes": 2048,
      "usage": "QUERY_INPUT",
      "status": "READY"
    }
  ]
}
```

该兼容响应不会包含 `objectKey`、OBS 签名 URL、本地存储路径或文件内容。第一阶段不提供创建新附件的接口。

## 提交问题

```http
POST /api/v1/questions/submission
Idempotency-Key: <客户端生成的唯一键>
Content-Type: application/json

{"chatId":null,"agentType":"SMART_DATA","question":"用户问题"}
```

首次提问中 `agentType` 为必填字段；缺失、未知或当前未开放的类型返回 `400 Invalid request`，且不得创建回答、查询知识库或业务数据库、调用 HiAgent。已有会话的后续请求示例为 `{"chatId":"会话 UUID","question":"用户后续问题"}`，不发送 `agentType`；后端以会话中保存的类型路由，携带不同类型试图切换时返回 `400`。第一阶段请求携带非空 `files` 时同样返回 `400`。

返回 `202 Accepted`：

```json
{
  "conversation": {
    "id": "UUID",
    "title": "用户问题的前 100 个 Unicode 字符",
    "createdAt": "2026-09-14T08:00:00Z",
    "updatedAt": "2026-09-14T08:00:00Z"
  },
  "conversationCreated": true,
  "answer": {
    "answerId": "UUID",
    "questionId": "UUID",
    "traceId": "UUID",
    "status": "PENDING",
    "content": "",
    "streamPath": "/api/v1/answers/{answerId}/events"
  }
}
```

`chatId=null` 表示首次提问；非空表示向当前用户已有会话追问。两种形态不会影响后续问答流程，区别仅在于是否先创建会话。相同用户、幂等键和请求指纹必须返回同一会话与回答；同一幂等键改换问题、Agent、会话 ID 或首次/非首次形态时返回 `409 IDEMPOTENCY_CONFLICT`。旧的独立创建会话和按路径提交问题接口不再注册。

## 临时附件（后续阶段）

第一阶段不注册临时附件上传、列表或删除接口。相关 URL 和数据契约在后续阶段重新评审。

## 流式回答

```http
GET /api/v1/answers/{answerId}/events
Accept: text/event-stream
Last-Event-ID: 0
```

事件类型为 `metadata`、`workflow_plan_selected`、`intent_recognition_started`、`intent_recognized`、`clarification_required`、`retrieval_started`、`citation`、`knowledge_retrieval_completed`、`business_query_started`、`business_query_completed`、`evidence_reconciliation_started`、`evidence_assessed`、`manual_review_required`、`generation_started`、`generation_completed`、`delta`、`cancellation_requested`、`cancelled`、`cancellation_failed`、`completed` 和 `error`。每个事件具有单调递增 ID。

白盒事件值采用不包含业务正文的稳定摘要：`workflow_plan_selected=场景|版本`，`intent_recognized=意图|置信度百分数|查询条件数量`，知识库与业务数据库完成事件为结果数量，`evidence_assessed=核验状态|冲突字段数量`，`generation_completed=READY_TO_PERSIST`。严禁在事件中放入公司大模型提示词、思维链、原始 SQL、服务地址、密钥或证据正文。

当意图低置信度、指代无法证明、缺少必需参数或存在多个授权候选时，事件顺序为 `metadata → workflow_plan_selected → intent_recognition_started → clarification_required → delta → completed`，最后一个事件的值为 `clarification_required`，回答快照状态为 `NEEDS_CLARIFICATION`。该分支不得出现检索、业务查询、证据对账或模型生成事件。用户把补充内容作为下一条普通问题提交；服务端从持久化待追问上下文恢复原意图。

当 `evidence_assessed=CONFLICT` 时，随后必须出现 `manual_review_required`，回答文本先展示知识库与数据库两个通道的数据及冲突字段，再展示模型整理结果。`INSUFFICIENT` 返回确定性拒答，模型不得补充事实。

事件在发布前写入 GoldenDB `qa_answer_event`，同一回答可从持久事件 ID 重放。历史消息中的 `executionEvents` 恢复非 `delta` 执行阶段，`artifacts` 恢复可展示引用产物。当前实时通知仍局限于本实例，生产 Redis 事件流接入完成前，不得承诺跨实例即时续流。

如果 `Last-Event-ID` 早于当前仍保留的最早事件，接口返回 `409 Event replay gap`。客户端应调用 `GET /answers/{answerId}` 重新加载完整持久化快照；不得继续拼接不完整的增量文本。

## 回答操作

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/answers/{answerId}` | 查询最终或当前回答快照 |
| `POST` | `/answers/{answerId}/regenerations` | 把原问题重新发起为新问答轮次，要求 `Idempotency-Key` |
| `POST` | `/answers/{answerId}/cancellation` | 停止生成，要求 `Idempotency-Key`，Body：`{"reason":"USER_REQUESTED"}` |
| `POST` | `/answers/{answerId}/feedback` | Body：`{"feedback":"LIKE"}` 或 `DISLIKE` |

复制回答由前端完成，不需要后端 API。

重新生成成功返回新的 `questionId`、`answerId` 和 `traceId`，并通过 `regeneratedFromAnswerId` 指向原回答。服务端会新增一条原问题内容相同的用户消息，但不会复制历史附件，再从意图识别开始执行完整问答链路；不会覆盖旧消息，也不会直接复用旧证据或旧模型答案。

`NEEDS_CLARIFICATION` 回答不能重新生成，调用重新生成接口返回 `409`；用户应提交下一条问题补充查询对象或候选序号。

### 停止生成

首次有效停止返回 `202 Accepted`，回答状态为 `CANCEL_REQUESTED`。客户端继续监听原 SSE 地址，直至收到：

- `cancelled`：停止成功，快照状态为 `CANCELLED`；
- `cancellation_failed`：远程停止最终失败或未取得模型消息 ID，快照状态为 `CANCEL_FAILED`。

重复提交同一回答的停止请求返回当前停止状态，不重复调用。已完成、失败或不完整的回答返回 `409 Answer already terminal`。SSE 断开不会自动停止回答。

回答快照可能包含 `cancelReason`、`cancelledStage`、`cancelErrorCode`、`cancelRequestedAt` 和 `cancelledAt`。已经发送给客户端的 `delta` 会先写入持久化快照，停止后可通过 `GET /answers/{answerId}` 读取部分内容。

## 错误

同步 API 使用兼容 RFC 7807 的 `application/problem+json`。流建立后的失败使用 `error` 事件，事件值为稳定错误码，不暴露内部异常或依赖地址。

当前 `Idempotency-Key` 最大 128 个字符。正式实现还需保存请求指纹；同一键对应不同聊天、问题或操作时应返回 `409`，不得静默复用旧结果。
