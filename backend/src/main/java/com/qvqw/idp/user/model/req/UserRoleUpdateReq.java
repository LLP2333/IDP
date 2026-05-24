package com.qvqw.idp.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 分配角色请求。
 */
@Schema(description = "分配角色请求")
public class UserRoleUpdateReq {

    @Schema(description = "角色 ID 列表（全量覆盖，传空数组表示清空角色）",
            example = "[1,2]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色 ID 列表不能为空")
    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
