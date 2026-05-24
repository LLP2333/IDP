package com.qvqw.idp.user.model.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户分页查询条件。
 */
@Schema(description = "用户分页查询条件")
public class UserQuery {

    @Schema(description = "按用户名模糊匹配", example = "zhang")
    private String username;

    @Schema(description = "状态过滤：1=启用，0=禁用", example = "1")
    private Integer status;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
