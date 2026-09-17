package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * 与 {@code qa_answer_event} 表对应的持久化记录。
 */
@TableName("qa_answer_event")
public class AnswerEventPersistenceRecord {

    /** 全局递增事件序号。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 回答 ID。 */
    @TableField("answer_id")
    private String answerId;
    /** 事件类型。 */
    @TableField("event_type")
    private String eventType;
    /** 事件数据。 */
    @TableField("event_data")
    private String eventData;
    /** 事件发生时间。 */
    @TableField("occurred_at")
    private Timestamp occurredAt;

    /** @return 全局递增事件序号。 */
    public Long getId() { return id; }

    /** @param value 全局递增事件序号。 */
    public void setId(final Long value) { this.id = value; }

    /** @return 回答 ID。 */
    public String getAnswerId() { return answerId; }

    /** @param value 回答 ID。 */
    public void setAnswerId(final String value) { this.answerId = value; }

    /** @return 事件类型。 */
    public String getEventType() { return eventType; }

    /** @param value 事件类型。 */
    public void setEventType(final String value) { this.eventType = value; }

    /** @return 事件数据。 */
    public String getEventData() { return eventData; }

    /** @param value 事件数据。 */
    public void setEventData(final String value) { this.eventData = value; }

    /** @return 事件发生时间。 */
    public Timestamp getOccurredAt() {
        return occurredAt == null ? null : new Timestamp(occurredAt.getTime());
    }

    /** @param value 事件发生时间。 */
    public void setOccurredAt(final Timestamp value) {
        this.occurredAt = value == null ? null : new Timestamp(value.getTime());
    }

    /**
     * 将数据库时间转换为领域时间。
     *
     * @return 事件发生时间。
     */
    public Instant occurredAtInstant() {
        if (occurredAt == null) {
            throw new IllegalStateException("answer event occurredAt was not mapped");
        }
        return occurredAt.toInstant();
    }
}
