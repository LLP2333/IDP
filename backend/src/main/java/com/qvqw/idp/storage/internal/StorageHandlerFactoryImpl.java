package com.qvqw.idp.storage.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StorageHandlerFactory;
import com.qvqw.idp.storage.StorageType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link StorageHandlerFactory} 默认实现。
 *
 * <p>按 {@link Storage#getId()} 缓存 {@link StorageHandler}，避免每次上传都新建底层 S3Client / 校验本地目录。
 * 通过监听 {@link StorageServiceImpl.StorageChangedEvent} 在存储配置变更时失效缓存。</p>
 */
@Component
public class StorageHandlerFactoryImpl implements StorageHandlerFactory {

    private final StorageSecretCipher cipher;
    private final ConcurrentHashMap<Long, StorageHandler> cache = new ConcurrentHashMap<>();

    public StorageHandlerFactoryImpl(StorageSecretCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public StorageHandler get(Storage storage) {
        if (storage == null || storage.getId() == null) {
            throw new BusinessException("存储记录无效");
        }
        return cache.computeIfAbsent(storage.getId(), id -> build(storage));
    }

    @Override
    public void evict(Long storageId) {
        StorageHandler handler = cache.remove(storageId);
        if (handler != null) {
            try {
                handler.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 监听存储配置变更事件，自动失效缓存。
     */
    @EventListener
    public void onStorageChanged(StorageServiceImpl.StorageChangedEvent event) {
        evict(event.storageId());
    }

    private StorageHandler build(Storage storage) {
        StorageType type = StorageType.ofValue(storage.getType());
        if (type == null) {
            throw new BusinessException("存储类型异常: " + storage.getType());
        }
        return switch (type) {
            case LOCAL -> new LocalStorageHandler(storage);
            case S3 -> new S3StorageHandler(storage, cipher.decrypt(storage.getSecretKey()));
        };
    }
}
