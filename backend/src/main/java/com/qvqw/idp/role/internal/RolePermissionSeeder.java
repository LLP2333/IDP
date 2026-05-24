package com.qvqw.idp.role.internal;

import com.qvqw.idp.permission.PermissionService;
import com.qvqw.idp.role.Role;
import com.qvqw.idp.role.RolePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 启动时把所有 “系统内置权限” 绑定到 admin 角色，幂等。
 *
 * <p>执行顺序：</p>
 * <ol>
 *   <li>{@link com.qvqw.idp.role.internal.RoleSeeder}（{@code @Order(10)}）— 建 admin / user 角色</li>
 *   <li>{@link com.qvqw.idp.permission.internal.PermissionSeeder}（{@code @Order(15)}）— 建权限节点</li>
 *   <li>本 Seeder（{@code @Order(17)}）— 绑定 admin → 所有系统权限</li>
 *   <li>{@link com.qvqw.idp.option.internal.OptionSeeder}（{@code @Order(30)}）— 建系统参数</li>
 * </ol>
 *
 * <p>注意：虽然运行期 {@link com.qvqw.idp.permission.internal.PermissionAspect} 已经特判
 * admin 直通，但 admin 角色仍然显式绑定所有权限，便于前端 “角色 - 权限” 页面可视化展示。</p>
 */
@Component
@Order(17)
public class RolePermissionSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionSeeder.class);

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionService permissionService;

    public RolePermissionSeeder(RoleRepository roleRepository,
                                RolePermissionRepository rolePermissionRepository,
                                PermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Optional<Role> adminOpt = roleRepository.findByCode("admin");
        if (adminOpt.isEmpty()) {
            return;
        }
        Long adminId = adminOpt.get().getId();
        List<Long> systemPermIds = permissionService.listSystemPermissionIds();
        if (systemPermIds.isEmpty()) {
            return;
        }
        Set<Long> already = new HashSet<>(rolePermissionRepository.findPermissionIdsByRoleId(adminId));
        int created = 0;
        for (Long pid : systemPermIds) {
            if (!already.contains(pid)) {
                rolePermissionRepository.save(new RolePermission(adminId, pid));
                created++;
            }
        }
        if (created > 0) {
            log.info("[初始化] admin 角色新增 {} 条权限绑定", created);
        }
    }
}
