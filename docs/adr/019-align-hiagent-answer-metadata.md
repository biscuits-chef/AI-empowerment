# ADR-019：回答元数据字段与 HiAgent 接口语义对齐

## 状态

已接受；流式事件中的扩展元数据仍待真实样例确认。

## 背景

智能体创设平台是公司内部统一的大模型底层服务。系统原有 `qa_answer.provider_message_id` 表达的实际内容就是 HiAgent `MessageID`，但列名仍使用了泛化供应商命名；同时，`create_conversation` 返回的 `AppConversationID` 没有持久化，不利于故障排查、停止调用审计和后续消息详情对账。

平台消息详情模型还定义了 `QueryID`、`TaskID`、`TotalTokens`、`Latency`、`TracingJsonStr`、`IntentionJsonStr` 和 `RetrieverResource`。这些字段语义明确，但当前 `chat_query` 流事件完整结构尚未获得真实脱敏样例。

## 决策

1. 使用 Flyway V19 把 `qa_answer.provider_message_id` 前向改名为 `message_id`。
2. 在 `qa_answer` 增加 `app_conversation_id`、`query_id`、`task_id`、`total_tokens`、`latency`、`tracing_json_str`、`intention_json_str` 和 `retriever_resource`。
3. 数据库使用 snake_case；持久化 Record 以数据库字段为准转换为 Java 小驼峰，例如 `app_conversation_id` 对应 `appConversationId`、`message_id` 对应 `messageId`。通过显式 MyBatis 映射与 HiAgent 原始字段保持一一对应，不再使用 `providerMessageId` 等偏离物理列的别名。
4. `create_conversation` 成功后、发起 `chat_query` 前立即把 `AppConversationID` 写入当前回答；流事件取得 `MessageID` 后继续沿用持久停止任务机制写入 `message_id`。
5. 尚未在当前流契约中确认的字段保持 `NULL`。禁止从未知 JSON 层级递归猜测，也禁止用本系统 ID 或估算值填充。
6. 本系统 `conversation_id`、`question_id`、`content`、`created_at` 和反馈表字段不改名。它们描述本系统领域对象，与平台 `AppConversationID`、`QueryID`、`Answer`、`CreatedTime` 和 `Like` 不是同一数据。
7. 平台嵌套集合 `OtherAnswers`、`QueryExtends.Files`、`QAInfo` 和 `Inputs` 不直接压平进 `qa_answer`；只有业务启用且关系、生命周期、权限边界明确后，才通过独立表或 JSON 快照设计落地。

## 后果

- 生产升级必须先备份并在代表性 GoldenDB 环境验证 `CHANGE COLUMN` 的方言、锁时长和存量值保持不变。
- 停止逻辑读取的物理列从 `provider_message_id` 切换为 `message_id`，对外 REST API 不受影响。
- 表结构已为平台详情元数据预留位置，但这不代表真实 SSE 契约已经完成联调。
