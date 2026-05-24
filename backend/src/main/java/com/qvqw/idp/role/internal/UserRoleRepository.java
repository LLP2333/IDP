package com.qvqw.idp.role.internal;

import com.qvqw.idp.role.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 用户-角色关联表的 Repository。
 */
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

    /**
     * 查询某用户的全部关联记录。
     *
     * @param userId 用户 ID
     * @return 关联记录列表
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * 查询某角色的全部关联记录。
     *
     * @param roleId 角色 ID
     * @return 关联记录列表
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * 仅返回用户的角色 ID 集合。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    @Query("select ur.roleId from UserRole ur where ur.userId = :userId")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 仅返回某角色下的用户 ID 集合。
     *
     * @param roleId 角色 ID
     * @return 用户 ID 列表
     */
    @Query("select ur.userId from UserRole ur where ur.roleId = :roleId")
    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 计算某角色下的用户数量（删除角色前的引用计数校验）。
     *
     * @param roleId 角色 ID
     * @return 用户数
     */
    long countByRoleId(Long roleId);

    /**
     * 清空某用户的所有角色关联。
     *
     * @param userId 用户 ID
     */
    @Modifying
    @Query("delete from UserRole ur where ur.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 清空某角色的所有用户关联。
     *
     * @param roleId 角色 ID
     */
    @Modifying
    @Query("delete from UserRole ur where ur.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
