package com.acme.intelligentqa.adapter.out.storage;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.TemporaryFileProperties;
import com.acme.intelligentqa.domain.port.out.ObjectStoragePort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 仅供开发环境使用的受限本地对象存储适配器。
 */
@Component
@ConditionalOnProperty(name = "app.file.storage-mode", havingValue = "local")
@Primary
public final class LocalObjectStorageAdapter implements ObjectStoragePort {

    /** 已规范化的本地对象根目录。 */
    private final Path root;

    /**
     * 创建本地开发对象存储适配器。
     *
     * @param properties 临时文件配置。
     */
    public LocalObjectStorageAdapter(final TemporaryFileProperties properties) {
        this.root = Paths.get(properties.localRoot()).toAbsolutePath().normalize();
    }

    /**
     * 保存一个已经过大小限制的文件对象。
     *
     * @param objectKey 服务端生成的对象键。
     * @param content 文件字节内容。
     * @param contentType 服务端识别的内容类型。
     */
    @Override
    public void store(final String objectKey, final byte[] content, final String contentType) {
        final Path target = resolve(objectKey);
        final Path parent = target.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("objectKey must identify a file inside local storage root");
        }
        try {
            Files.createDirectories(parent);
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (final IOException exception) {
            throw new DependencyUnavailableException(
                    "LOCAL_FILE_STORAGE_FAILED", "local development file could not be stored", exception);
        }
    }

    /**
     * 删除指定对象；对象不存在时也应视为成功。
     *
     * @param objectKey 服务端生成的对象键。
     */
    @Override
    public void delete(final String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (final IOException exception) {
            throw new DependencyUnavailableException(
                    "LOCAL_FILE_DELETE_FAILED", "local development file could not be deleted", exception);
        }
    }

    /**
     * 在根目录内安全解析服务端生成的对象键。
     *
     * @param objectKey 服务端对象键。
     * @return 根目录内的规范化目标路径。
     */
    private Path resolve(final String objectKey) {
        final Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("objectKey must stay inside local storage root");
        }
        return target;
    }
}
