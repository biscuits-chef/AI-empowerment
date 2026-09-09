package com.acme.intelligentqa.common.error;

/**
 * 表示上传文件超过服务端配置的字节上限。
 */
public class FileTooLargeException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 创建文件过大异常。
     *
     * @param maximumBytes 允许的最大字节数。
     */
    public FileTooLargeException(final long maximumBytes) {
        super("文件不能超过 " + maximumBytes + " 字节");
    }
}
