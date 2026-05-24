package com.qvqw.idp.menu.internal;

import com.qvqw.idp.menu.Menu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 系统内置菜单 / 按钮节点初始化。
 *
 * <p>启动时幂等地灌入三层结构：</p>
 * <ol>
 *   <li>顶级目录 {@code 系统管理}（{@code type=1}），用于侧边栏一级菜单分组；</li>
 *   <li>子菜单（{@code type=2}）：用户管理 / 角色管理 / 菜单管理 / 系统配置；</li>
 *   <li>每个子菜单下的按钮（{@code type=3}），{@code permission} 字段保存具体权限码。</li>
 * </ol>
 *
 * <p>菜单的 {@code path} / {@code name} / {@code component} 取与前端 Next.js 文件路由对齐的相对路径，
 * 仅作为元数据保存；前端侧边栏按 {@code path} 跳转。实际角色 - 菜单绑定在
 * {@link com.qvqw.idp.role.internal.RoleMenuSeeder} 中完成。</p>
 */
@Component
@Order(15)
public class MenuSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MenuSeeder.class);

    private static final int TYPE_DIR = 1;
    private static final int TYPE_MENU = 2;
    private static final int TYPE_BUTTON = 3;

    private final MenuRepository menuRepository;

    public MenuSeeder(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Menu systemDir = ensureDir("系统管理", "/system", "system", "Layout", "settings",
                100, "系统管理一级目录");
        Menu userMenu = ensureMenu("用户管理", systemDir.getId(),
                "/admin/system/user", "User", "system/user/index", "users",
                110, "用户管理菜单");
        Menu roleMenu = ensureMenu("角色管理", systemDir.getId(),
                "/admin/system/role", "Role", "system/role/index", "shield-check",
                120, "角色管理菜单");
        Menu menuMenu = ensureMenu("菜单管理", systemDir.getId(),
                "/admin/system/menu", "Menu", "system/menu/index", "menu",
                130, "菜单管理菜单");
        Menu siteMenu = ensureMenu("网站配置", systemDir.getId(),
                "/admin/system/config", "SiteConfig", "system/config/index", "settings",
                140, "网站基本信息");
        Menu securityMenu = ensureMenu("安全配置", systemDir.getId(),
                "/admin/system/config?tab=password", "SecurityConfig",
                "system/config/index", "shield", 150, "密码策略");
        Menu loginMenu = ensureMenu("登录配置", systemDir.getId(),
                "/admin/system/config?tab=login", "LoginConfig",
                "system/config/index", "log-in", 160, "登录相关开关");

        record ButtonDef(String permission, String title) {
        }
        record MenuButtons(Menu parent, List<ButtonDef> buttons) {
        }
        List<MenuButtons> buttonGroups = List.of(
                new MenuButtons(userMenu, List.of(
                        new ButtonDef("system:user:list", "查询用户"),
                        new ButtonDef("system:user:add", "新增用户"),
                        new ButtonDef("system:user:update", "修改用户"),
                        new ButtonDef("system:user:delete", "删除用户"),
                        new ButtonDef("system:user:resetPassword", "重置密码"),
                        new ButtonDef("system:user:updateRole", "分配角色"))),
                new MenuButtons(roleMenu, List.of(
                        new ButtonDef("system:role:list", "查询角色"),
                        new ButtonDef("system:role:add", "新增角色"),
                        new ButtonDef("system:role:update", "修改角色"),
                        new ButtonDef("system:role:delete", "删除角色"),
                        new ButtonDef("system:role:assignPermission", "分配菜单"))),
                new MenuButtons(menuMenu, List.of(
                        new ButtonDef("system:menu:list", "查询菜单"),
                        new ButtonDef("system:menu:add", "新增菜单"),
                        new ButtonDef("system:menu:update", "修改菜单"),
                        new ButtonDef("system:menu:delete", "删除菜单"))),
                new MenuButtons(siteMenu, List.of(
                        new ButtonDef("system:siteConfig:get", "查询网站配置"),
                        new ButtonDef("system:siteConfig:update", "修改网站配置"))),
                new MenuButtons(securityMenu, List.of(
                        new ButtonDef("system:securityConfig:get", "查询安全配置"),
                        new ButtonDef("system:securityConfig:update", "修改安全配置"))),
                new MenuButtons(loginMenu, List.of(
                        new ButtonDef("system:loginConfig:get", "查询登录配置"),
                        new ButtonDef("system:loginConfig:update", "修改登录配置"))));

        int sort = 1000;
        int created = 0;
        for (MenuButtons group : buttonGroups) {
            for (ButtonDef def : group.buttons()) {
                if (ensureButton(def.permission(), def.title(), group.parent().getId(), sort++)) {
                    created++;
                }
            }
        }
        if (created > 0) {
            log.info("[初始化] 已新增 {} 条系统内置按钮权限", created);
        }
    }

    /** 保证某目录节点存在（按 path + parent 唯一）；已存在直接返回。 */
    private Menu ensureDir(String title, String path, String name, String component, String icon,
                           int sort, String description) {
        return menuRepository.findAll().stream()
                .filter(m -> Long.valueOf(0L).equals(m.getParentId())
                        && Integer.valueOf(TYPE_DIR).equals(m.getType())
                        && title.equals(m.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    Menu m = new Menu();
                    m.setTitle(title);
                    m.setParentId(0L);
                    m.setType(TYPE_DIR);
                    m.setPath(path);
                    m.setName(name);
                    m.setComponent(component);
                    m.setIcon(icon);
                    m.setSort(sort);
                    m.setStatus(1);
                    m.setIsSystem(true);
                    m.setIsExternal(false);
                    m.setIsCache(false);
                    m.setIsHidden(false);
                    m.setDescription(description);
                    return menuRepository.save(m);
                });
    }

    /** 保证某菜单节点存在（按 parent + title 唯一）；已存在直接返回。 */
    private Menu ensureMenu(String title, Long parentId, String path, String name, String component,
                            String icon, int sort, String description) {
        return menuRepository.findAll().stream()
                .filter(m -> parentId.equals(m.getParentId())
                        && Integer.valueOf(TYPE_MENU).equals(m.getType())
                        && title.equals(m.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    Menu m = new Menu();
                    m.setTitle(title);
                    m.setParentId(parentId);
                    m.setType(TYPE_MENU);
                    m.setPath(path);
                    m.setName(name);
                    m.setComponent(component);
                    m.setIcon(icon);
                    m.setSort(sort);
                    m.setStatus(1);
                    m.setIsSystem(true);
                    m.setIsExternal(false);
                    m.setIsCache(false);
                    m.setIsHidden(false);
                    m.setDescription(description);
                    return menuRepository.save(m);
                });
    }

    /** 保证某按钮节点存在；不存在则插入并返回 true（用于统计新增数）。 */
    private boolean ensureButton(String permission, String title, Long parentId, int sort) {
        Optional<Menu> existing = menuRepository.findByPermission(permission);
        if (existing.isPresent()) {
            return false;
        }
        Menu m = new Menu();
        m.setTitle(title);
        m.setParentId(parentId);
        m.setType(TYPE_BUTTON);
        m.setPermission(permission);
        m.setSort(sort);
        m.setStatus(1);
        m.setIsSystem(true);
        m.setIsExternal(false);
        m.setIsCache(false);
        m.setIsHidden(false);
        m.setDescription(title);
        menuRepository.save(m);
        return true;
    }

    /** 仅供集成测试用：清空内置数据用例时复用。 */
    @SuppressWarnings("unused")
    static List<String> buttonPermissions() {
        return new ArrayList<>(List.of(
                "system:user:list", "system:user:add", "system:user:update", "system:user:delete",
                "system:user:resetPassword", "system:user:updateRole",
                "system:role:list", "system:role:add", "system:role:update", "system:role:delete",
                "system:role:assignPermission",
                "system:menu:list", "system:menu:add", "system:menu:update", "system:menu:delete",
                "system:siteConfig:get", "system:siteConfig:update",
                "system:securityConfig:get", "system:securityConfig:update",
                "system:loginConfig:get", "system:loginConfig:update"));
    }
}
