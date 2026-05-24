package com.qvqw.idp.role.model.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色查询条件。
 */
@Schema(description = "角色查询条件")
public class RoleQuery {

    @Schema(description = "关键字（同时匹配 name 和 code，忽略大小写）", example = "admin")
    private String keyword;

    @Schema(description = "状态：1=启用, 0=禁用", example = "1")
    private Integer status;

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
}
