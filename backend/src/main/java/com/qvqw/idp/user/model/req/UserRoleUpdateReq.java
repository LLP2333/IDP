package com.qvqw.idp.user.model.req;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 分配角色请求。
 */
public class UserRoleUpdateReq {

    @NotNull(message = "角色 ID 列表不能为空")
    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
