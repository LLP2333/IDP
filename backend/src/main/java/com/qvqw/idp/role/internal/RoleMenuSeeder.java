package com.qvqw.idp.role.internal;

import com.qvqw.idp.menu.MenuService;
import com.qvqw.idp.role.Role;
import com.qvqw.idp.role.RoleMenu;
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
 * 启动时把所有 “系统内置菜单” 绑定到 admin 角色，幂等。
 *
 * <p>执行顺序：</p>
 * <ol>
 *   <li>{@link com.qvqw.idp.role.internal.RoleSeeder}（{@code @Order(10)}）— 建 admin / user 角色</li>
 *   <li>{@link com.qvqw.idp.menu.internal.MenuSeeder}（{@code @Order(15)}）— 建菜单节点</li>
 *   <li>本 Seeder（{@code @Order(17)}）— 绑定 admin → 所有系统菜单</li>
 *   <li>{@link com.qvqw.idp.option.internal.OptionSeeder}（{@code @Order(30)}）— 建系统参数</li>
 * </ol>
 *
 * <p>注意：虽然运行期 {@link com.qvqw.idp.menu.internal.MenuAspect} 已经特判
 * admin 直通，但 admin 角色仍然显式绑定所有菜单，便于前端 “角色 - 菜单” 页面可视化展示。</p>
 */
@Component
@Order(17)
public class RoleMenuSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleMenuSeeder.class);

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuService menuService;

    public RoleMenuSeeder(RoleRepository roleRepository,
                          RoleMenuRepository roleMenuRepository,
                          MenuService menuService) {
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuService = menuService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Optional<Role> adminOpt = roleRepository.findByCode("admin");
        if (adminOpt.isEmpty()) {
            return;
        }
        Long adminId = adminOpt.get().getId();
        List<Long> systemMenuIds = menuService.listSystemMenuIds();
        if (systemMenuIds.isEmpty()) {
            return;
        }
        Set<Long> already = new HashSet<>(roleMenuRepository.findMenuIdsByRoleId(adminId));
        int created = 0;
        for (Long mid : systemMenuIds) {
            if (!already.contains(mid)) {
                roleMenuRepository.save(new RoleMenu(adminId, mid));
                created++;
            }
        }
        if (created > 0) {
            log.info("[初始化] admin 角色新增 {} 条菜单绑定", created);
        }
    }
}
