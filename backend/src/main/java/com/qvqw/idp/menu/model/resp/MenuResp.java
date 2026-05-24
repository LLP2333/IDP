package com.qvqw.idp.menu.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单响应 DTO（同时承担树节点角色）。
 *
 * <p>{@link #children} 仅在 tree 接口中填充；平铺 list 接口固定为空列表，方便前端按需消费。</p>
 */
@Schema(description = "菜单")
public class MenuResp {

    @Schema(description = "菜单 ID", example = "1")
    private Long id;

    @Schema(description = "标题", example = "用户管理")
    private String title;

    @Schema(description = "父节点 ID", example = "0")
    private Long parentId;

    @Schema(description = "类型：1=目录, 2=菜单, 3=按钮", example = "2")
    private Integer type;

    @Schema(description = "路由地址", example = "/system/user")
    private String path;

    @Schema(description = "组件名称", example = "User")
    private String name;

    @Schema(description = "组件路径", example = "system/user/index")
    private String component;

    @Schema(description = "重定向地址")
    private String redirect;

    @Schema(description = "图标", example = "users")
    private String icon;

    @Schema(description = "是否外链", example = "false")
    private Boolean isExternal;

    @Schema(description = "是否启用 keep-alive 缓存", example = "false")
    private Boolean isCache;

    @Schema(description = "是否在侧边栏隐藏", example = "false")
    private Boolean isHidden;

    @Schema(description = "按钮权限码（仅 type=3）", example = "system:user:add")
    private String permission;

    @Schema(description = "排序值", example = "100")
    private Integer sort;

    @Schema(description = "状态：1=启用, 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "是否系统内置", example = "true")
    private Boolean isSystem;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "子节点（仅在树结构接口返回）")
    private List<MenuResp> children = new ArrayList<>();

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MenuResp> getChildren() {
        return children;
    }

    public void setChildren(List<MenuResp> children) {
        this.children = children;
    }
}
