package com.qvqw.idp.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 角色-权限关联表（表 {@code idp_sys_role_permission}）。
 *
 * <p>联合主键 {@code (role_id, permission_id)}。{@code permission_id} 仅作为外部引用使用，
 * 不通过 JPA 外键级联，避免与 permission 模块强耦合。</p>
 */
@Entity
@IdClass(RolePermission.RolePermissionId.class)
@Table(name = "idp_sys_role_permission", indexes = {
        @Index(name = "idx_idp_sys_role_perm_role", columnList = "role_id"),
        @Index(name = "idx_idp_sys_role_perm_perm", columnList = "permission_id")
})
public class RolePermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Id
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    public RolePermission() {
    }

    public RolePermission(Long roleId, Long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    /** 联合主键值对象。 */
    public static class RolePermissionId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long roleId;
        private Long permissionId;

        public RolePermissionId() {
        }

        public RolePermissionId(Long roleId, Long permissionId) {
            this.roleId = roleId;
            this.permissionId = permissionId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public Long getPermissionId() {
            return permissionId;
        }

        public void setPermissionId(Long permissionId) {
            this.permissionId = permissionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RolePermissionId that)) {
                return false;
            }
            return Objects.equals(roleId, that.roleId) && Objects.equals(permissionId, that.permissionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, permissionId);
        }
    }
}
