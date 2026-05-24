package com.qvqw.idp.menu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 菜单新增 / 修改请求。
 *
 * <p>type 决定哪些字段必填：</p>
 * <ul>
 *   <li>type=1 目录：title + path + name；</li>
 *   <li>type=2 菜单：title + path + name + component；</li>
 *   <li>type=3 按钮：title + permission（{@link #permission} 必填，{@link #path} 等留空）。</li>
 * </ul>
 *
 * <p>具体业务校验由 {@code MenuServiceImpl} 在保存前再做一次。</p>
 */
@Schema(description = "菜单请求")
public class MenuReq {

    @Schema(description = "标题", example = "用户管理", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 64, message = "标题长度不能超过 64 个字符")
    private String title;

    @Schema(description = "父节点 ID，顶级为 0", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "父节点 ID 不能为空")
    private Long parentId;

    @Schema(description = "类型：1=目录, 2=菜单, 3=按钮", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "类型不能为空")
    private Integer type;

    @Schema(description = "路由地址（按钮类型留空）", example = "/system/user")
    @Size(max = 255, message = "路由地址长度不能超过 255 个字符")
    private String path;

    @Schema(description = "组件名称（按钮类型留空）", example = "User")
    @Size(max = 64, message = "组件名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "组件路径（仅菜单类型必填）", example = "system/user/index")
    @Size(max = 255, message = "组件路径长度不能超过 255 个字符")
    private String component;

    @Schema(description = "重定向地址", example = "/system/user")
    @Size(max = 255, message = "重定向地址长度不能超过 255 个字符")
    private String redirect;

    @Schema(description = "图标标识", example = "users")
    @Size(max = 50, message = "图标长度不能超过 50 个字符")
    private String icon;

    @Schema(description = "是否外链；true 时 path 必须以 http(s) 开头", example = "false")
    private Boolean isExternal;

    @Schema(description = "是否启用 keep-alive 缓存", example = "false")
    private Boolean isCache;

    @Schema(description = "是否在侧边栏隐藏", example = "false")
    private Boolean isHidden;

    @Schema(description = "按钮权限码（仅 type=3 必填）", example = "system:user:add")
    @Pattern(regexp = "^$|^[a-z][a-zA-Z0-9_:]{1,99}$",
            message = "权限标识格式不合法（小写字母开头，仅含字母/数字/下划线/冒号）")
    private String permission;

    @Schema(description = "排序值（越小越靠前）", example = "100")
    @Min(value = 1, message = "排序最小值为 1")
    private Integer sort;

    @Schema(description = "状态：1=启用, 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "描述")
    @Size(max = 255, message = "描述长度不能超过 255 个字符")
    private String description;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
