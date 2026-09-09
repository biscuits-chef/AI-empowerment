package com.acme.intelligentqa.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 使用稳定游标返回的一页会话摘要。
 */
public final class ConversationPage {

    /** 当前页会话列表。 */
    private final List<Conversation> items;
    /** 下一页不透明游标；没有更多数据时为空。 */
    private final String nextCursor;
    /** 是否还有下一页。 */
    private final boolean hasMore;

    /**
     * 创建会话游标页。
     *
     * @param items 当前页会话列表。
     * @param nextCursor 下一页不透明游标；没有更多数据时为空。
     * @param hasMore 是否还有下一页。
     */
    public ConversationPage(
            final List<Conversation> items,
            final String nextCursor,
            final boolean hasMore) {
        this.items = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(items, "items must not be null")));
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    /**
     * 返回当前页会话列表。
     *
     * @return 不可变会话列表。
     */
    public List<Conversation> items() { return items; }

    /**
     * 返回下一页游标。
     *
     * @return 下一页不透明游标；没有更多数据时为空。
     */
    public String nextCursor() { return nextCursor; }

    /**
     * 返回是否还有下一页。
     *
     * @return 有下一页时返回 true。
     */
    public boolean hasMore() { return hasMore; }
}
