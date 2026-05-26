package com.qvqw.idp.storage;

import java.util.Collection;

/**
 * 存储引用关系检查接口。
 *
 * <p>由 {@code file} 模块在 internal 中实现并注入到 storage 模块，用于在删除 / 禁用存储前
 * 判断是否仍有文件关联。如此设计避免 {@code storage} 模块反向依赖 {@code file} 模块，
 * 保持模块边界清晰。</p>
 *
 * <p>该接口注册为 Spring Bean；当 {@code file} 模块没有装载时，由
 * {@link com.qvqw.idp.storage.internal.StorageServiceImpl} 自身的兜底实现处理。</p>
 */
public interface StorageReferenceChecker {

    /**
     * 统计指定存储 ID 下尚存的文件数量。
     *
     * @param storageIds 存储 ID 列表
     * @return 关联文件数（含回收站）
     */
    long countFilesByStorageIds(Collection<Long> storageIds);
}
