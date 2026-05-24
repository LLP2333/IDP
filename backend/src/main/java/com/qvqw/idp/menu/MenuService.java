package com.qvqw.idp.menu;

import com.qvqw.idp.menu.model.query.MenuQuery;
import com.qvqw.idp.menu.model.req.MenuReq;
import com.qvqw.idp.menu.model.resp.MenuResp;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 菜单服务（对外暴露）。
 *
 * <p>菜单模块同时承担 “路由元数据 + 按钮权限” 两类职责：</p>
 * <ul>
 *   <li>{@code type=1/2} 节点对应前端侧边栏 / 路由；</li>
 *   <li>{@code type=3} 节点对应一个按钮权限码，写在 {@code permission} 字段，参与 {@code MenuAspect} 鉴权。</li>
 * </ul>
 *
 * <p>用户 → 角色 → 菜单的查询链由 role 模块负责，避免双向依赖。</p>
 */
public interface MenuService {

    /** 平铺列表查询（按 sort 升序），用于菜单管理表格。 */
    List<MenuResp> list(MenuQuery query);

    /**
     * 列出树形结构（按 sort 升序，全量返回不过滤状态，前端自行展示禁用样式）。
     *
     * @return 树（顶级节点列表）
     */
    List<MenuResp> tree(MenuQuery query);

    MenuResp get(Long id);

    Long create(MenuReq req);

    void update(Long id, MenuReq req);

    void delete(List<Long> ids);

    /**
     * 按菜单 ID 集合批量获取按钮权限码（去重，仅返回启用状态的 {@code type=3} 节点）。
     *
     * @param ids 菜单 ID 集合（可为 {@code null} / 空）
     * @return 权限码集合；输入为空时返回空集
     */
    Set<String> listCodesByIds(Collection<Long> ids);

    /**
     * 仅返回 “系统内置” 的菜单 ID（用于 admin 角色默认绑定全部菜单）。
     *
     * @return 内置菜单 ID 列表
     */
    List<Long> listSystemMenuIds();

    /**
     * 列出某些角色可见的菜单树（仅 {@code type=1/2}，按 sort 升序）。
     *
     * <p>用于 {@code /auth/user/route} 接口：根据用户的角色聚合可见菜单，过滤掉按钮节点。
     * 传入空集合返回空列表；传入 {@code admin} 角色的 ID 时由调用方自行决定是否走 admin 直通逻辑。</p>
     *
     * @param menuIds 角色已绑定的菜单 ID 集合
     * @return 菜单树（type=1/2，按 sort 升序）
     */
    List<MenuResp> treeByIds(Collection<Long> menuIds);

    /**
     * 列出全部启用的 {@code type=1/2} 菜单树（admin 直通时使用）。
     *
     * @return 完整菜单树
     */
    List<MenuResp> treeAllEnabledRoutes();
}
