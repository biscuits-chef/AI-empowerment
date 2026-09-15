package com.acme.intelligentqa.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerFeedbackMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.AnswerPersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ChatMessageMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ChatMessagePersistenceRecord;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ConversationMapper;
import com.acme.intelligentqa.common.error.PersistenceOperationException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;

/**
 * 验证回答原生 MyBatis 持久化适配器的异常边界。
 */
class MybatisAnswerRepositoryTest {

    /**
     * 验证会话时间更新的数据访问异常被转换为稳定的持久化异常。
     */
    @Test
    void translatesConversationTouchFailure() {
        final AnswerMapper answerMapper = mock(AnswerMapper.class);
        final ChatMessageMapper messageMapper = mock(ChatMessageMapper.class);
        final ConversationMapper conversationMapper = mock(ConversationMapper.class);
        final AnswerFeedbackMapper feedbackMapper = mock(AnswerFeedbackMapper.class);
        final MybatisAnswerRepository repository = new MybatisAnswerRepository(
                answerMapper, messageMapper, conversationMapper, feedbackMapper);

        when(answerMapper.insert(any(AnswerPersistenceRecord.class))).thenReturn(1);
        when(messageMapper.insert(any(ChatMessagePersistenceRecord.class))).thenReturn(1);
        when(conversationMapper.touch(anyString(), any(Timestamp.class)))
                .thenThrow(new QueryTimeoutException("database detail must remain internal"));

        final PersistenceOperationException exception = assertThrows(
                PersistenceOperationException.class,
                () -> repository.create(
                        "owner-1",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        "问题",
                        "idempotency-key",
                        Instant.parse("2026-09-14T00:00:00Z")));

        assertEquals("failed to update conversation timestamp", exception.getMessage());
        assertEquals(QueryTimeoutException.class, exception.getCause().getClass());
    }
}
