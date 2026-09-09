package com.acme.intelligentqa.domain.port.out;

/**
 * 隔离本地开发存储与生产 OBS SDK 的对象存储端口。
 */
public interface ObjectStoragePort {

    /**
     * 保存一个已经过大小限制的文件对象。
     *
     * @param objectKey 服务端生成的对象键。
     * @param content 文件字节内容。
     * @param contentType 服务端识别的内容类型。
     */
    void store(String objectKey, byte[] content, String contentType);

    /**
     * 删除指定对象；对象不存在时也应视为成功。
     *
     * @param objectKey 服务端生成的对象键。
     */
    void delete(String objectKey);
}
