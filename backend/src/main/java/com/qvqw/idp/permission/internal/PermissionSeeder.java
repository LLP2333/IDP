package com.qvqw.idp.permission.internal;

import com.qvqw.idp.permission.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统内置权限节点初始化。
 *
 * <p>启动时幂等地灌入所有 “按钮级” 权限码；菜单父节点（type=1）仅起分组作用。
 * 实际角色 - 权限绑定在 {@link com.qvqw.idp.role.internal.RolePermissionSeeder} 中完成。</p>
 */
@Component
@Order(15)
public class PermissionSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissionSeeder.class);

    private final PermissionRepository permissionRepository;

    public PermissionSeeder(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 顶级：系统管理
        Permission systemRoot = ensureMenu("system", "系统管理", 0L, 100, "系统管理顶级菜单");
        // 子菜单
        Permission userMenu = ensureMenu("system:user", "用户管理", systemRoot.getId(), 110, "用户 CRUD");
        Permission roleMenu = ensureMenu("system:role", "角色管理", systemRoot.getId(), 120, "角色 CRUD");
        Permission permMenu = ensureMenu("system:permission", "权限管理", systemRoot.getId(), 130, "权限 CRUD");
        Permission siteMenu = ensureMenu("system:siteConfig", "网站配置", systemRoot.getId(), 140, "网站基本信息");
        Permission securityMenu = ensureMenu("system:securityConfig", "安全配置", systemRoot.getId(), 150, "密码策略");
        Permission loginMenu = ensureMenu("system:loginConfig", "登录配置", systemRoot.getId(), 160, "登录相关开关");
        // 按钮
        Map<Long, List<ButtonDef>> buttons = Map.of(
                userMenu.getId(), List.of(
                        new ButtonDef("system:user:list", "查询用户"),
                        new ButtonDef("system:user:add", "新增用户"),
                        new ButtonDef("system:user:update", "修改用户"),
                        new ButtonDef("system:user:delete", "删除用户"),
                        new ButtonDef("system:user:resetPassword", "重置密码"),
                        new ButtonDef("system:user:updateRole", "分配角色")),
                roleMenu.getId(), List.of(
                        new ButtonDef("system:role:list", "查询角色"),
                        new ButtonDef("system:role:add", "新增角色"),
                        new ButtonDef("system:role:update", "修改角色"),
                        new ButtonDef("system:role:delete", "删除角色"),
                        new ButtonDef("system:role:assignPermission", "分配权限")),
                permMenu.getId(), List.of(
                        new ButtonDef("system:permission:list", "查询权限"),
                        new ButtonDef("system:permission:add", "新增权限"),
                        new ButtonDef("system:permission:update", "修改权限"),
                        new ButtonDef("system:permission:delete", "删除权限")),
                siteMenu.getId(), List.of(
                        new ButtonDef("system:siteConfig:get", "查询网站配置"),
                        new ButtonDef("system:siteConfig:update", "修改网站配置")),
                securityMenu.getId(), List.of(
                        new ButtonDef("system:securityConfig:get", "查询安全配置"),
                        new ButtonDef("system:securityConfig:update", "修改安全配置")),
                loginMenu.getId(), List.of(
                        new ButtonDef("system:loginConfig:get", "查询登录配置"),
                        new ButtonDef("system:loginConfig:update", "修改登录配置")));
        int created = 0;
        int sort = 1000;
        // 按照 menu 顺序遍历，保证排序稳定
        List<Permission> orderedMenus = List.of(userMenu, roleMenu, permMenu, siteMenu, securityMenu, loginMenu);
        for (Permission menu : orderedMenus) {
            List<ButtonDef> defs = buttons.getOrDefault(menu.getId(), List.of());
            for (ButtonDef def : defs) {
                if (ensureButton(def.code, def.name, menu.getId(), sort++)) {
                    created++;
                }
            }
        }
        if (created > 0) {
            log.info("[初始化] 已新增 {} 条系统内置权限", created);
        }
    }

    /** 保证某菜单节点存在，已存在返回原记录。 */
    private Permission ensureMenu(String code, String name, Long parentId, int sort, String description) {
        return permissionRepository.findByCode(code).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(code);
            p.setName(name);
            p.setParentId(parentId);
            p.setType(1);
            p.setSort(sort);
            p.setStatus(1);
            p.setIsSystem(true);
            p.setDescription(description);
            return permissionRepository.save(p);
        });
    }

    /** 保证某按钮节点存在；不存在则插入并返回 true（用于统计新增数）。 */
    private boolean ensureButton(String code, String name, Long parentId, int sort) {
        Optional<Permission> existing = permissionRepository.findByCode(code);
        if (existing.isPresent()) {
            return false;
        }
        Permission p = new Permission();
        p.setCode(code);
        p.setName(name);
        p.setParentId(parentId);
        p.setType(2);
        p.setSort(sort);
        p.setStatus(1);
        p.setIsSystem(true);
        p.setDescription(name);
        permissionRepository.save(p);
        return true;
    }

    private record ButtonDef(String code, String name) {
    }

    /** 仅供集成测试用：清空内置数据用例时复用。 */
    @SuppressWarnings("unused")
    static List<String> buttonCodes() {
        return new ArrayList<>(List.of(
                "system:user:list", "system:user:add", "system:user:update", "system:user:delete",
                "system:user:resetPassword", "system:user:updateRole",
                "system:role:list", "system:role:add", "system:role:update", "system:role:delete",
                "system:role:assignPermission",
                "system:permission:list", "system:permission:add", "system:permission:update", "system:permission:delete",
                "system:siteConfig:get", "system:siteConfig:update",
                "system:securityConfig:get", "system:securityConfig:update",
                "system:loginConfig:get", "system:loginConfig:update"));
    }
}
