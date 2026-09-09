package com.acme.intelligentqa.common.error;

/**
 * 表示上传文件的扩展名、内容类型或文件头不受支持。
 */
public class UnsupportedFileTypeException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 创建不支持文件类型异常。
     */
    public UnsupportedFileTypeException() {
        super("文件类型不受支持或文件内容与扩展名不一致");
    }
}
