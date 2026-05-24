package com.qvqw.idp.permission.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 权限新增 / 修改请求。
 */
@Schema(description = "权限请求")
public class PermissionReq {

    @Schema(description = "权限编码（小写 + 冒号分隔）", example = "system:user:list", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "权限编码不能为空")
    @Pattern(regexp = "^[a-z][a-zA-Z0-9_:]{1,99}$", message = "权限编码格式不合法（小写字母开头，仅含字母/数字/下划线/冒号）")
    private String code;

    @Schema(description = "权限名称", example = "用户列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64, message = "名称不能超过 64")
    private String name;

    @Schema(description = "父节点 ID，顶级为 0", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "父节点 ID 不能为空")
    private Long parentId;

    @Schema(description = "类型：1=菜单, 2=按钮", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "类型不能为空")
    private Integer type;

    @Schema(description = "排序值（越小越靠前）", example = "100")
    private Integer sort;

    @Schema(description = "状态：1=启用, 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "描述")
    @Size(max = 255)
    private String description;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
