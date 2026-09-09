package com.acme.intelligentqa.domain.port.out;

import com.acme.intelligentqa.domain.model.EntityCandidate;
import java.util.List;

/**
 * 使用已批准产品主数据把用户输入的代码、名称、简称或全称解析为稳定产品候选。
 */
public interface ProductEntityResolutionPort {

    /**
     * 在最新有效快照中解析当前用户可以查询的产品候选。
     *
     * @param ownerId 用户所有者 ID。
     * @param productReference 用户输入的产品代码或名称引用。
     * @return 有界且去重的受权产品候选列表。
     */
    List<EntityCandidate> resolve(String ownerId, String productReference);
}
