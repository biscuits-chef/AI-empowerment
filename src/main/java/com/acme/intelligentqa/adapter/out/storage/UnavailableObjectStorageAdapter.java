package com.acme.intelligentqa.adapter.out.storage;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.domain.port.out.ObjectStoragePort;
import org.springframework.stereotype.Component;

/**
 * 在未配置开发存储或真实 OBS 时阻止文件请求静默降级。
 */
@Component
public final class UnavailableObjectStorageAdapter implements ObjectStoragePort {

    /**
     * 拒绝未配置对象存储时的写入。
     *
     * @param objectKey 服务端生成的对象键。
     * @param content 文件字节内容。
     * @param contentType 服务端识别的内容类型。
     */
    @Override
    public void store(final String objectKey, final byte[] content, final String contentType) {
        throw unavailable();
    }

    /**
     * 拒绝未配置对象存储时的删除。
     *
     * @param objectKey 服务端生成的对象键。
     */
    @Override
    public void delete(final String objectKey) {
        throw unavailable();
    }

    /**
     * 创建不泄露配置细节的依赖不可用异常。
     *
     * @return 对象存储未配置异常。
     */
    private DependencyUnavailableException unavailable() {
        return new DependencyUnavailableException(
                "OBJECT_STORAGE_UNCONFIGURED", "object storage adapter is not configured");
    }
}
