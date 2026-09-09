package com.acme.intelligentqa.common.error;

/**
 * 表示问题引用的临时文件尚未完成上传、解析或安全检查。
 */
public class FileNotReadyException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 创建文件未就绪异常。
     */
    public FileNotReadyException() {
        super("附件尚未处理完成，请等待文件就绪后再发送问题");
    }
}
