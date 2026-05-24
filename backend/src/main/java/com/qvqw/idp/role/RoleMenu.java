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
 * 角色-菜单关联表（表 {@code idp_sys_role_menu}）。
 *
 * <p>联合主键 {@code (role_id, menu_id)}。{@code menu_id} 仅作为外部引用使用，
 * 不通过 JPA 外键级联，避免与 menu 模块强耦合。</p>
 *
 * <p>语义：角色拥有该 menu 节点的访问权（type=1/2 控制侧边栏可见，type=3 控制按钮鉴权码）。</p>
 */
@Entity
@IdClass(RoleMenu.RoleMenuId.class)
@Table(name = "idp_sys_role_menu", indexes = {
        @Index(name = "idx_idp_sys_role_menu_role", columnList = "role_id"),
        @Index(name = "idx_idp_sys_role_menu_menu", columnList = "menu_id")
})
public class RoleMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Id
    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    public RoleMenu() {
    }

    public RoleMenu(Long roleId, Long menuId) {
        this.roleId = roleId;
        this.menuId = menuId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    /** 联合主键值对象。 */
    public static class RoleMenuId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long roleId;
        private Long menuId;

        public RoleMenuId() {
        }

        public RoleMenuId(Long roleId, Long menuId) {
            this.roleId = roleId;
            this.menuId = menuId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public Long getMenuId() {
            return menuId;
        }

        public void setMenuId(Long menuId) {
            this.menuId = menuId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RoleMenuId that)) {
                return false;
            }
            return Objects.equals(roleId, that.roleId) && Objects.equals(menuId, that.menuId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, menuId);
        }
    }
}
