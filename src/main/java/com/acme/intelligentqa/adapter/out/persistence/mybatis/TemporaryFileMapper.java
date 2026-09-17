package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 临时文件表的 MyBatis-Plus 映射器。
 */
@Mapper
public interface TemporaryFileMapper extends BaseMapper<TemporaryFilePersistenceRecord> {

    /**
     * 按用户和会话读取有界活动文件。
     *
     * @param ownerId 用户所有者 ID。
     * @param conversationId 会话 ID。
     * @param limit 数量上限。
     * @return 临时文件记录列表。
     */
    List<TemporaryFilePersistenceRecord> selectActive(
            @Param("ownerId") String ownerId,
            @Param("conversationId") String conversationId,
            @Param("limit") int limit);
}

