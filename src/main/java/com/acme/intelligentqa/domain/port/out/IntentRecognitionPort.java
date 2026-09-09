package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.ChatMessage;
import com.acme.intelligentqa.domain.model.QueryIntent;
import java.util.List;

/**
 * 识别受控意图、实体、缺失参数和候选项的出站端口。
 */
public interface IntentRecognitionPort {

    /**
     * 结合问题和历史识别受控意图。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param question 用户问题。
     *
     * @param history 最近对话历史。
     *
     * @return 结合问题和历史识别受控意图。
     */
    QueryIntent recognize(String ownerId, String question, List<ChatMessage> history);
}
