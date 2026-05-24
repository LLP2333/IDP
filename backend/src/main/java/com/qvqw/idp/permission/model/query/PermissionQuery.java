package com.qvqw.idp.permission.model.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 权限列表查询条件。
 */
@Schema(description = "权限查询条件")
public class PermissionQuery {

    @Schema(description = "名称 / 编码模糊关键字", example = "user")
    private String keyword;

    @Schema(description = "状态：1=启用, 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "类型：1=菜单, 2=按钮", example = "2")
    private Integer type;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
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
