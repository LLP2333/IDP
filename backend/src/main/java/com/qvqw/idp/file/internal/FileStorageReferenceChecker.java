package com.qvqw.idp.file.internal;

import com.qvqw.idp.storage.StorageReferenceChecker;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * {@link StorageReferenceChecker} 在 {@code file} 模块中的实现：
 * 通过 {@link FileRepository#countByStorageIdIn} 提供存储 → 文件数的反查。
 *
 * <p>之所以在 file 模块这里实现，是因为 storage 模块不能依赖 file 模块。</p>
 */
@Component
class FileStorageReferenceChecker implements StorageReferenceChecker {

    private final FileRepository repository;

    FileStorageReferenceChecker(FileRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countFilesByStorageIds(Collection<Long> storageIds) {
        if (storageIds == null || storageIds.isEmpty()) {
            return 0;
        }
        return repository.countByStorageIdIn(storageIds);
    }
}
