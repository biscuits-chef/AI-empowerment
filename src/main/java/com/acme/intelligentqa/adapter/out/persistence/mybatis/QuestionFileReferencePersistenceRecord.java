package com.acme.intelligentqa.adapter.out.persistence.mybatis;

/**
 * 承载问题与临时文件关联查询结果的持久化投影。
 */
public class QuestionFileReferencePersistenceRecord {

    /** 文件 ID。 */
    private String fileId;
    /** 文件在该次问题中的用途。 */
    private String usageType;

    /**
     * 返回文件 ID。
     *
     * @return 文件 ID。
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * 设置文件 ID。
     *
     * @param value 文件 ID。
     */
    public void setFileId(final String value) {
        this.fileId = value;
    }

    /**
     * 返回文件在该次问题中的用途。
     *
     * @return 文件用途。
     */
    public String getUsageType() {
        return usageType;
    }

    /**
     * 设置文件在该次问题中的用途。
     *
     * @param value 文件用途。
     */
    public void setUsageType(final String value) {
        this.usageType = value;
    }
}
