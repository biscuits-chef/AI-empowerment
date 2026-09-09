package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.QueryIntent;
import java.util.List;

/**
 * 通过已审核查询目录读取业务事实的出站端口，禁止执行模型生成的任意 SQL。
 */
public interface BusinessDataQueryPort {

    /**
     * 查询模型查询文本。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param question 用户问题。
     *
     * @param intent 查询意图。
     *
     * @param limit 数量上限。
     *
     * @return 模型查询文本。
     */
    List<BusinessFact> query(String ownerId, String question, QueryIntent intent, int limit);
}
