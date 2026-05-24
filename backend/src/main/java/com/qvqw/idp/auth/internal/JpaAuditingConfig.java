package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.UserContextHolder;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 启用 JPA 审计。
 *
 * <p>当前线程绑定的用户 ID 作为 createdBy / updatedBy。</p>
 *
 * <p>放置在 {@code auth.internal} 以避免 {@code common} 模块反向依赖 {@code auth}。
 * Spring 的 {@code @EnableJpaAuditing} 是全局生效，配置在哪都可以。</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Component("auditorAware")
    static class CurrentUserAuditorAware implements AuditorAware<Long> {

        @Override
        public Optional<Long> getCurrentAuditor() {
            return Optional.ofNullable(UserContextHolder.getUserId());
        }
    }
}
