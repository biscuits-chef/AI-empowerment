package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 回答表的 MyBatis-Plus 映射器。 */
@Mapper
public interface AnswerMapper extends BaseMapper<AnswerPersistenceRecord> {

    /**
     * 按用户与幂等键读取已受理记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @return 按用户与幂等键读取已受理记录。
     */
    AnswerPersistenceRecord selectByIdempotencyKey(
            @Param("ownerId") String ownerId,
            @Param("idempotencyKey") String idempotencyKey);

    /**
     * 按用户与唯一标识读取记录。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param id 唯一标识。
     *
     * @return 按用户与唯一标识读取记录。
     */
    AnswerPersistenceRecord selectByOwnerAndId(
            @Param("ownerId") String ownerId,
            @Param("id") String id);

    /**
     * 读取原始用户问题。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param id 唯一标识。
     *
     * @return 读取原始用户问题。
     */
    String selectQuestion(@Param("ownerId") String ownerId, @Param("id") String id);

}
