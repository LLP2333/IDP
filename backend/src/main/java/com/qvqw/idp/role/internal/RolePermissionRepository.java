package com.qvqw.idp.role.internal;

import com.qvqw.idp.role.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 角色-权限关联表的 Repository。
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {

    @Query("select rp.permissionId from RolePermission rp where rp.roleId = :roleId")
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

    @Query("select distinct rp.permissionId from RolePermission rp where rp.roleId in :roleIds")
    List<Long> findPermissionIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    long countByPermissionId(Long permissionId);

    @Modifying
    @Query("delete from RolePermission rp where rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("delete from RolePermission rp where rp.permissionId = :permissionId")
    void deleteByPermissionId(@Param("permissionId") Long permissionId);
}
