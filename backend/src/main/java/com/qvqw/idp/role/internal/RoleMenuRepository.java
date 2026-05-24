package com.qvqw.idp.role.internal;

import com.qvqw.idp.role.RoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 角色-菜单关联表的 Repository。
 */
public interface RoleMenuRepository extends JpaRepository<RoleMenu, RoleMenu.RoleMenuId> {

    @Query("select rm.menuId from RoleMenu rm where rm.roleId = :roleId")
    List<Long> findMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Query("select distinct rm.menuId from RoleMenu rm where rm.roleId in :roleIds")
    List<Long> findMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    long countByMenuId(Long menuId);

    @Modifying
    @Query("delete from RoleMenu rm where rm.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("delete from RoleMenu rm where rm.menuId = :menuId")
    void deleteByMenuId(@Param("menuId") Long menuId);
}
