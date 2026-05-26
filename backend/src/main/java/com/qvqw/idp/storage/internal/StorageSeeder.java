package com.qvqw.idp.storage.internal;

import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 默认存储初始化。
 *
 * <p>启动时若不存在任何存储记录，则插入一条 LOCAL 类型的默认存储 {@code code=local}：</p>
 * <ul>
 *   <li>{@code bucketName}：本地绝对路径，来源于配置 {@code idp.file.local.base-path}；</li>
 *   <li>{@code domain}：公开访问 URL 前缀，来源于配置 {@code idp.file.local.domain}；</li>
 *   <li>回收站开启，路径 {@code .RECYCLE.BIN/}。</li>
 * </ul>
 *
 * <p>{@code @Order(50)} 保证晚于菜单 / 角色 Seeder 执行；演示用 MinIO 存储不在此自动创建，
 * 详见 {@code docs/file-storage.md}。</p>
 */
@Component
@Order(50)
public class StorageSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageSeeder.class);

    private static final String DEFAULT_CODE = "local";

    private final StorageRepository repository;
    private final String basePath;
    private final String domain;

    public StorageSeeder(StorageRepository repository,
                         @Value("${idp.file.local.base-path}") String basePath,
                         @Value("${idp.file.local.domain}") String domain) {
        this.repository = repository;
        this.basePath = basePath;
        this.domain = domain;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.existsByCode(DEFAULT_CODE)) {
            return;
        }
        Storage storage = new Storage();
        storage.setName("本地存储");
        storage.setCode(DEFAULT_CODE);
        storage.setType(StorageType.LOCAL.getValue());
        storage.setBucketName(ensureTrailing(basePath));
        storage.setDomain(ensureTrailing(domain));
        storage.setRecycleBinEnabled(Boolean.TRUE);
        storage.setRecycleBinPath(".RECYCLE.BIN/");
        storage.setDescription("内置本地文件系统存储");
        storage.setIsDefault(Boolean.TRUE);
        storage.setSort(1);
        storage.setStatus(1);
        repository.save(storage);
        log.info("[初始化] 默认本地存储已创建 code={} basePath={}", DEFAULT_CODE, basePath);
    }

    private static String ensureTrailing(String s) {
        if (s == null || s.isEmpty()) {
            return "/";
        }
        return s.endsWith("/") ? s : s + "/";
    }
}
