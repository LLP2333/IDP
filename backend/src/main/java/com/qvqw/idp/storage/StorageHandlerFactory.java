package com.qvqw.idp.storage;

/**
 * 存储句柄工厂。
 *
 * <p>跨模块入口：file 模块通过 {@link #get(Storage)} 拿到 {@link StorageHandler}，再调用接口进行 IO 操作。
 * 实现位于 internal 包，通过 Spring 自动装配；按 {@link Storage#getId()} 缓存 handler 避免重复构造
 * 底层 client。</p>
 */
public interface StorageHandlerFactory {

    /**
     * 获取或创建存储句柄；缓存。
     *
     * @param storage 存储实体
     * @return 存储句柄
     */
    StorageHandler get(Storage storage);

    /**
     * 失效指定存储的句柄缓存。
     *
     * @param storageId 存储 ID
     */
    void evict(Long storageId);
}
