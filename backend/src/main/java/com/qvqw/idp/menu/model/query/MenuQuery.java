package com.qvqw.idp.menu.model.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 菜单列表查询条件。
 */
@Schema(description = "菜单查询条件")
public class MenuQuery {

    @Schema(description = "标题模糊关键字", example = "用户")
    private String title;

    @Schema(description = "路由地址模糊关键字", example = "/system/user")
    private String path;

    @Schema(description = "权限标识模糊关键字", example = "system:user:add")
    private String permission;

    @Schema(description = "状态：1=启用, 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "类型：1=目录, 2=菜单, 3=按钮", example = "2")
    private Integer type;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
