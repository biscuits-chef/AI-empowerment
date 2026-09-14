# ADR-004：通过公司 HiAgent 运行态 API 调用大模型

## 状态

已接受，真实流事件契约待联调确认。

## 背景

公司提供《HiAgent 运行态 API 文档》。第一阶段必须通过该平台调用大模型，而不是假设 OpenAI 兼容接口。对话接口要求先创建平台会话，再用返回的 `AppConversationID` 调用 `chat_query`。

## 决策

1. 生产适配器调用 `{baseUrl}/create_conversation`，请求使用原始字段 `Inputs` 和 `UserID`。
2. 取得 `Conversation.AppConversationID` 后调用 `{baseUrl}/chat_query`。
3. 聊天请求使用原始字段 `UserID`、`AppConversationID`、`Query`、`ResponseMode=streaming` 和 `PubAgentJump=false`。
4. 本系统把已审核的历史、意图、知识证据和数据库证据组装到 `Query`；每次生成使用独立平台会话，不依赖平台隐藏上下文。
5. 不使用 `/query_again` 实现本系统重新生成。重新生成在本系统内创建新的问题与回答，只复制原问题文本、不复制历史附件，并重新取得最新双通道证据；新回答只通过追溯字段关联原回答，不覆盖旧问答。
6. 流响应按 SSE `data: json` 解析。当前仅接受 `Answer/answer`；未知结构和空答案失败关闭，真实样例确认后再扩展。
7. `create_conversation` 返回的 `AppConversationID` 在调用 `chat_query` 前保存到当前 `qa_answer.app_conversation_id`；流中确认的 `MessageID` 保存到 `qa_answer.message_id`。其他详情元数据遵循 ADR-019，未确认前保持为空。

## 后果

- 请求 DTO 使用精确 JSON 属性名，并由契约测试验证大小写。
- `UserID` 受 1～20 字符限制，SSO 标识映射需业务与安全确认。
- 每次创建平台会话会增加一次调用和平台会话数量，但避免双重上下文状态漂移。
- 平台流事件结构、错误码和鉴权缺失是生产联调阻塞项。
