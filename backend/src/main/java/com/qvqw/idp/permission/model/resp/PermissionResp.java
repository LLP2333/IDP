package com.qvqw.idp.permission.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限响应 DTO（同时承担树节点角色）。
 */
@Schema(description = "权限")
public class PermissionResp {

    @Schema(description = "权限 ID", example = "1")
    private Long id;

    @Schema(description = "权限编码", example = "system:user:list")
    private String code;

    @Schema(description = "权限名称", example = "用户列表")
    private String name;

    @Schema(description = "父节点 ID", example = "0")
    private Long parentId;

    @Schema(description = "类型：1=菜单, 2=按钮", example = "2")
    private Integer type;

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
    private List<PermissionResp> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<PermissionResp> getChildren() {
        return children;
    }

    public void setChildren(List<PermissionResp> children) {
        this.children = children;
    }
}
