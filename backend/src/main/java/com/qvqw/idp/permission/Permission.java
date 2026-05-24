package com.qvqw.idp.permission;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serial;

/**
 * 权限实体（表 {@code idp_sys_permission}）。
 *
 * <p>每条记录对应一个 “权限节点”：可能是菜单（{@code type=1}），也可能是按钮操作
 * （{@code type=2}）。{@link #code} 在全表唯一，业务侧通过 {@code @HasPermission("system:user:list")}
 * 这样的字符串来匹配。</p>
 */
@Entity
@Table(name = "idp_sys_permission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idp_sys_permission_code", columnNames = "code")
})
public class Permission extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 权限编码，业务唯一键，如 {@code system:user:list}。 */
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    /** 权限名称（前端权限树展示）。 */
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    /** 父节点 ID；顶级节点为 0。 */
    @Column(name = "parent_id", nullable = false)
    private Long parentId = 0L;

    /** 类型：1=菜单（用于分组）、2=按钮（用于鉴权）。 */
    @Column(name = "type", nullable = false)
    private Integer type = 2;

    /** 排序值，越小越靠前。 */
    @Column(name = "sort", nullable = false)
    private Integer sort = 999;

    /** 状态：1=启用，0=禁用（禁用的权限不参与鉴权）。 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 是否系统内置（true 时不允许通过接口删除 / 改 code）。 */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    /** 描述。 */
    @Column(name = "description", length = 255)
    private String description;

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
}
