package com.qvqw.idp.storage.internal;

import com.qvqw.idp.storage.StorageReferenceChecker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import java.util.Collection;

/**
 * 兜底的 {@link StorageReferenceChecker} 实现：在 {@code file} 模块缺席时返回 0，
 * 让 {@code storage} 模块的删除 / 状态检查逻辑仍可独立工作（主要用于单元测试场景）。
 *
 * <p>当 {@code file} 模块提供更具体的 Bean 时，@ConditionalOnMissingBean 会自动让位。</p>
 */
@Configuration
class DefaultStorageReferenceChecker {

    @Bean
    @ConditionalOnMissingBean(StorageReferenceChecker.class)
    StorageReferenceChecker noopStorageReferenceChecker() {
        return new StorageReferenceChecker() {
            @Override
            public long countFilesByStorageIds(Collection<Long> storageIds) {
                return 0L;
            }
        };
    }
}
