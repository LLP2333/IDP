package com.qvqw.idp.menu;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serial;

/**
 * 菜单实体（表 {@code idp_sys_menu}）。
 *
 * <p>一条记录可能代表三种角色：</p>
 * <ul>
 *   <li>{@code type=1} 目录：纯分组节点，没有真实页面（前端侧边栏一级菜单）；</li>
 *   <li>{@code type=2} 菜单：对应一个前端路由 / 页面；</li>
 *   <li>{@code type=3} 按钮：挂在菜单下面的细粒度权限点，{@link #permission} 字段保存对应的权限码（如
 *       {@code system:user:add}），实际鉴权由 {@code MenuAspect} 消费。</li>
 * </ul>
 *
 * <p>本表同时承担 “路由元数据 + 按钮权限” 双重职责，与 {@code idp_sys_role_menu} 组成
 * 角色 - 菜单 - 按钮的 RBAC 关系。</p>
 */
@Entity
@Table(name = "idp_sys_menu", indexes = {
        @Index(name = "idx_idp_sys_menu_parent", columnList = "parent_id"),
        @Index(name = "idx_idp_sys_menu_permission", columnList = "permission")
})
public class Menu extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 菜单标题（前端侧边栏 / 表格直接展示）。 */
    @Column(name = "title", nullable = false, length = 64)
    private String title;

    /** 父节点 ID；顶级节点固定为 0。 */
    @Column(name = "parent_id", nullable = false)
    private Long parentId = 0L;

    /** 类型：1=目录、2=菜单、3=按钮。 */
    @Column(name = "type", nullable = false)
    private Integer type = 2;

    /** 前端路由地址（按钮类型为空）。 */
    @Column(name = "path", length = 255)
    private String path;

    /** 组件名称（用于 keep-alive / 路由命名，按钮类型为空）。 */
    @Column(name = "name", length = 64)
    private String name;

    /** 组件路径（前端按约定加载组件，按钮类型为空）。 */
    @Column(name = "component", length = 255)
    private String component;

    /** 重定向地址（仅目录 / 非外链菜单）。 */
    @Column(name = "redirect", length = 255)
    private String redirect;

    /** 图标标识（前端按约定映射为图标组件）。 */
    @Column(name = "icon", length = 50)
    private String icon;

    /** 是否外链；true 时 {@link #path} 必须是 http(s) URL。 */
    @Column(name = "is_external", nullable = false)
    private Boolean isExternal = false;

    /** 是否启用 keep-alive 缓存（仅对菜单类型有意义）。 */
    @Column(name = "is_cache", nullable = false)
    private Boolean isCache = false;

    /** 是否在侧边栏隐藏（仍可访问，仅不显示）。 */
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    /**
     * 按钮权限码：如 {@code system:user:add}；仅 {@code type=3} 节点使用，
     * 其他类型为空。{@code MenuAspect} 在 AOP 校验时按该字段判断。
     */
    @Column(name = "permission", length = 100)
    private String permission;

    /** 排序值，越小越靠前。 */
    @Column(name = "sort", nullable = false)
    private Integer sort = 999;

    /** 状态：1=启用，0=禁用（禁用的菜单不会下发到前端，按钮不会参与鉴权）。 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 是否系统内置（true 时不允许通过接口删除 / 改 permission 等关键字段）。 */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    /** 描述。 */
    @Column(name = "description", length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getIsExternal() {
        return isExternal;
    }

    public void setIsExternal(Boolean isExternal) {
        this.isExternal = isExternal;
    }

    public Boolean getIsCache() {
        return isCache;
    }

    public void setIsCache(Boolean isCache) {
        this.isCache = isCache;
    }

    public Boolean getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
