package com.qvqw.idp.storage.internal;

import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地存储的静态资源映射。
 *
 * <p>启动时扫描所有 LOCAL 类型存储，根据 {@code storage.domain} 解析出 URL 路径前缀，
 * 把它映射到磁盘目录 {@code storage.bucketName} 上，让浏览器可以直接通过 URL 拉取文件。</p>
 *
 * <p>由于 Spring MVC 的 {@code addResourceHandlers} 仅在启动时调用一次，运行时新增的存储
 * 暂不支持动态注册（与参考项目策略一致：要么重启，要么通过外部反向代理处理）。</p>
 */
@Configuration
public class LocalStorageResourceConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageResourceConfig.class);

    private final StorageRepository repository;
    private final String defaultBasePath;
    private final String defaultDomain;

    private final Map<String, String> registered = new HashMap<>();

    public LocalStorageResourceConfig(StorageRepository repository,
                                      @Value("${idp.file.local.base-path}") String defaultBasePath,
                                      @Value("${idp.file.local.domain}") String defaultDomain) {
        this.repository = repository;
        this.defaultBasePath = defaultBasePath;
        this.defaultDomain = defaultDomain;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 默认始终注册一条，覆盖未初始化数据库的场景
        register(registry, defaultDomain, defaultBasePath);
        repository.findAll().forEach(storage -> {
            if (storage.getType() == StorageType.LOCAL.getValue()
                    && storage.getDomain() != null && storage.getBucketName() != null) {
                register(registry, storage.getDomain(), storage.getBucketName());
            }
        });
    }

    /**
     * 监听运行时新增的 LOCAL 存储，提示用户需要重启才能生效。
     */
    @EventListener
    public void onStorageChanged(StorageServiceImpl.StorageChangedEvent event) {
        repository.findById(event.storageId()).ifPresent(storage -> {
            if (storage.getType() == StorageType.LOCAL.getValue()
                    && storage.getDomain() != null
                    && !registered.containsKey(extractPath(storage.getDomain()))) {
                log.warn("[存储] 新增的本地存储 [{}] 需要重启服务才能让 URL 路径 {} 生效",
                        storage.getCode(), storage.getDomain());
            }
        });
    }

    private void register(ResourceHandlerRegistry registry, String domain, String basePath) {
        String path = extractPath(domain);
        if (path == null || path.isBlank()) {
            return;
        }
        if (registered.containsKey(path)) {
            return;
        }
        registered.put(path, basePath);
        String pattern = path.endsWith("/") ? path + "**" : path + "/**";
        String location = "file:" + (basePath.endsWith("/") ? basePath : basePath + "/");
        registry.addResourceHandler(pattern).addResourceLocations(location);
        log.info("[存储] 注册本地资源映射 {} -> {}", pattern, location);
    }

    private static String extractPath(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(domain);
            String p = uri.getPath();
            return (p == null || p.isBlank()) ? "/" : p;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
