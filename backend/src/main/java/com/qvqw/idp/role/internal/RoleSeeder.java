package com.qvqw.idp.role.internal;

import com.qvqw.idp.role.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 启动时初始化默认角色。
 */
@Component
@Order(10)
public class RoleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureRole("admin", "超级管理员", 1);
        ensureRole("user", "普通用户", 2);
    }

    private void ensureRole(String code, String name, int sort) {
        roleRepository.findByCode(code).orElseGet(() -> {
            Role r = new Role();
            r.setCode(code);
            r.setName(name);
            r.setSort(sort);
            r.setStatus(1);
            r.setIsSystem(true);
            r.setDescription(name);
            log.info("[初始化] 创建系统内置角色: code={}, name={}", code, name);
            return roleRepository.save(r);
        });
    }
}
