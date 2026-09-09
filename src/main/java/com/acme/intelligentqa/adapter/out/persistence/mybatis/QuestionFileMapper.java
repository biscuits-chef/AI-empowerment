package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import java.sql.Timestamp;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 问题与临时文件关联表的 MyBatis-Plus 映射器。
 */
@Mapper
public interface QuestionFileMapper {

    /**
     * 查询原问题已经固化的文件引用。
     *
     * @param questionId 原问题 ID。
     * @return 按关联创建顺序排列的文件引用。
     */
    @Select("SELECT file_id AS fileId, usage_type AS usageType "
            + "FROM qa_question_file WHERE question_id = #{questionId} ORDER BY created_at, file_id")
    List<QuestionFileReferencePersistenceRecord> selectByQuestionId(
            @Param("questionId") String questionId);

    /**
     * 保存一条不可变的问题文件引用。
     *
     * @param questionId 问题 ID。
     * @param fileId 文件 ID。
     * @param usageType 文件使用角色。
     * @param createdAt 关联创建时间。
     * @return 受影响行数。
     */
    @Insert("INSERT INTO qa_question_file "
            + "(question_id, file_id, usage_type, created_at) "
            + "VALUES (#{questionId}, #{fileId}, #{usageType}, #{createdAt})")
    int insertReference(
            @Param("questionId") String questionId,
            @Param("fileId") String fileId,
            @Param("usageType") String usageType,
            @Param("createdAt") Timestamp createdAt);

    /**
     * 统计指定文件的问题引用数量。
     *
     * @param fileId 文件 ID。
     * @return 问题引用数量。
     */
    @Select("SELECT COUNT(*) FROM qa_question_file WHERE file_id = #{fileId}")
    int countByFileId(@Param("fileId") String fileId);
}
