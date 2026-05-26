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
 *   <li>子菜单（{@code type=2}）：用户管理 / 角色管理 / 菜单管理 / 网站配置；</li>
 *   <li>每个子菜单下的按钮（{@code type=3}），{@code permission} 字段保存具体权限码。</li>
 * </ol>
 *
 * <p>“网站配置” 页面通过 tab 同时承载站点 / 安全 / 登录 三组参数，因此
 * {@code system:siteConfig:* / system:securityConfig:* / system:loginConfig:*} 全部直接挂在
 * “网站配置” 菜单下，不再为安全 / 登录单独造 type=2 菜单。</p>
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
                140, "网站配置（含站点 / 安全 / 登录三组参数）");
        Menu dictMenu = ensureMenu("字典管理", systemDir.getId(),
                "/admin/system/dict", "Dict", "system/dict/index", "book",
                150, "字典管理菜单");
        Menu noticeMenu = ensureMenu("通知公告", systemDir.getId(),
                "/admin/system/notice", "Notice", "system/notice/index", "bell",
                160, "通知公告菜单");
        Menu fileMenu = ensureMenu("文件管理", systemDir.getId(),
                "/admin/system/file", "File", "system/file/index", "file",
                170, "文件管理菜单（含回收站、分片上传）");
        Menu monitorDir = ensureDir("系统监控", "/monitor", "Monitor", "Layout", "computer",
                200, "系统监控一级目录");
        Menu onlineMenu = ensureMenu("在线用户", monitorDir.getId(),
                "/admin/monitor/online", "MonitorOnline", "monitor/online/index", "user",
                210, "在线用户菜单");
        Menu logMenu = ensureMenu("系统日志", monitorDir.getId(),
                "/admin/monitor/log", "MonitorLog", "monitor/log/index", "history",
                220, "系统日志菜单");

        // 老库迁移：早期版本曾把 “安全配置 / 登录配置” 建成 type=2 子菜单，
        // 现在统一收编到 “网站配置” 下。把它们的子按钮 reparent 到 siteMenu，再删除多余菜单。
        migrateLegacySubMenu(systemDir.getId(), "安全配置", siteMenu.getId());
        migrateLegacySubMenu(systemDir.getId(), "登录配置", siteMenu.getId());

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
                // 网站 / 安全 / 登录三组按钮全部直接挂在 “网站配置” 下
                new MenuButtons(siteMenu, List.of(
                        new ButtonDef("system:siteConfig:get", "查询网站配置"),
                        new ButtonDef("system:siteConfig:update", "修改网站配置"),
                        new ButtonDef("system:securityConfig:get", "查询安全配置"),
                        new ButtonDef("system:securityConfig:update", "修改安全配置"),
                        new ButtonDef("system:loginConfig:get", "查询登录配置"),
                        new ButtonDef("system:loginConfig:update", "修改登录配置"),
                        new ButtonDef("system:storage:list", "查询存储"),
                        new ButtonDef("system:storage:get", "存储详情"),
                        new ButtonDef("system:storage:add", "新增存储"),
                        new ButtonDef("system:storage:update", "修改存储"),
                        new ButtonDef("system:storage:delete", "删除存储"),
                        new ButtonDef("system:storage:updateStatus", "切换存储状态"),
                        new ButtonDef("system:storage:setDefault", "设为默认存储"))),
                new MenuButtons(fileMenu, List.of(
                        new ButtonDef("system:file:list", "查询文件"),
                        new ButtonDef("system:file:upload", "上传文件"),
                        new ButtonDef("system:file:update", "重命名"),
                        new ButtonDef("system:file:delete", "删除文件"),
                        new ButtonDef("system:file:createDir", "新建文件夹"),
                        new ButtonDef("system:file:calcDirSize", "计算文件夹大小"),
                        new ButtonDef("system:fileRecycle:list", "查询回收站"),
                        new ButtonDef("system:fileRecycle:restore", "还原文件"),
                        new ButtonDef("system:fileRecycle:delete", "物理删除"),
                        new ButtonDef("system:fileRecycle:clean", "清空回收站"))),
                new MenuButtons(dictMenu, List.of(
                        new ButtonDef("system:dict:list", "查询字典"),
                        new ButtonDef("system:dict:add", "新增字典"),
                        new ButtonDef("system:dict:update", "修改字典"),
                        new ButtonDef("system:dict:delete", "删除字典"))),
                new MenuButtons(noticeMenu, List.of(
                        new ButtonDef("system:notice:list", "查询公告"),
                        new ButtonDef("system:notice:add", "新增公告"),
                        new ButtonDef("system:notice:update", "修改公告"),
                        new ButtonDef("system:notice:delete", "删除公告"))),
                new MenuButtons(onlineMenu, List.of(
                        new ButtonDef("monitor:online:list", "查询在线用户"),
                        new ButtonDef("monitor:online:kickout", "强退在线用户"))),
                new MenuButtons(logMenu, List.of(
                        new ButtonDef("monitor:log:list", "查询系统日志"),
                        new ButtonDef("monitor:log:get", "日志详情"),
                        new ButtonDef("monitor:log:export", "导出系统日志"))));

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

    /**
     * 老库迁移：把 {@code (parentId, type=2, title)} 命中的旧菜单的所有子节点
     * reparent 到 {@code newParentId}，再删除旧菜单本身。
     *
     * <p>纯增量场景（新装库）下不会命中任何记录，直接 noop。</p>
     *
     * @param parentId    旧菜单的父节点（一般为 “系统管理” 目录）
     * @param title       旧菜单标题（“安全配置” / “登录配置”）
     * @param newParentId 子节点要被迁到的新父节点（一般为 “网站配置” 菜单）
     */
    private void migrateLegacySubMenu(Long parentId, String title, Long newParentId) {
        Optional<Menu> legacy = menuRepository.findAll().stream()
                .filter(m -> parentId.equals(m.getParentId())
                        && Integer.valueOf(TYPE_MENU).equals(m.getType())
                        && title.equals(m.getTitle()))
                .findFirst();
        if (legacy.isEmpty()) {
            return;
        }
        Menu legacyMenu = legacy.get();
        List<Menu> children = menuRepository.findAll().stream()
                .filter(m -> legacyMenu.getId().equals(m.getParentId()))
                .toList();
        for (Menu child : children) {
            child.setParentId(newParentId);
            menuRepository.save(child);
        }
        menuRepository.deleteById(legacyMenu.getId());
        log.info("[迁移] 已移除遗留菜单「{}」(id={}), {} 个子节点已转移到 parentId={}",
                title, legacyMenu.getId(), children.size(), newParentId);
    }

    /** 保证某按钮节点存在；不存在则插入并返回 true（用于统计新增数）。 */
    private boolean ensureButton(String permission, String title, Long parentId, int sort) {
        Optional<Menu> existing = menuRepository.findByPermission(permission);
        if (existing.isPresent()) {
            // 老库迁移兜底：按钮的 parentId 可能仍指向已删除的旧菜单，矫正一次。
            Menu m = existing.get();
            if (!parentId.equals(m.getParentId())) {
                m.setParentId(parentId);
                menuRepository.save(m);
            }
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
                "system:loginConfig:get", "system:loginConfig:update",
                "system:storage:list", "system:storage:get", "system:storage:add", "system:storage:update",
                "system:storage:delete", "system:storage:updateStatus", "system:storage:setDefault",
                "system:file:list", "system:file:upload", "system:file:update", "system:file:delete",
                "system:file:createDir", "system:file:calcDirSize",
                "system:fileRecycle:list", "system:fileRecycle:restore",
                "system:fileRecycle:delete", "system:fileRecycle:clean",
                "system:dict:list", "system:dict:add", "system:dict:update", "system:dict:delete",
                "system:notice:list", "system:notice:add", "system:notice:update", "system:notice:delete",
                "monitor:online:list", "monitor:online:kickout",
                "monitor:log:list", "monitor:log:get", "monitor:log:export"));
    }
}
