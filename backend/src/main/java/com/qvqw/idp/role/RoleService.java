package com.qvqw.idp.role;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.role.model.query.RoleQuery;
import com.qvqw.idp.role.model.req.RoleReq;
import com.qvqw.idp.role.model.resp.RoleResp;

import java.util.List;
import java.util.Set;

/**
 * 角色服务（对外暴露）。
 */
public interface RoleService {

    PageResp<RoleResp> page(RoleQuery query, int page, int size);

    List<RoleResp> list(RoleQuery query);

    RoleResp get(Long id);

    java.util.Optional<RoleResp> findByCode(String code);

    Long create(RoleReq req);

    void update(Long id, RoleReq req);

    void delete(List<Long> ids);

    /** 根据用户 ID 查询其所有角色编码（auth 模块使用）。 */
    Set<String> listCodesByUserId(Long userId);

    /** 角色 -> 用户 IDs。 */
    List<Long> listUserIdsByRoleId(Long roleId);

    /** 用户 -> 角色 IDs。 */
    List<Long> listRoleIdsByUserId(Long userId);

    /** 重新分配某用户的角色集合。 */
    void assignRoles(Long userId, List<Long> roleIds);

    /** 校验所有角色 ID 是否合法存在，存在不合法则抛业务异常。 */
    void ensureRolesExist(List<Long> roleIds);

    /**
     * 列出某角色绑定的全部权限 ID。
     *
     * @param roleId 角色 ID
     * @return 权限 ID 列表
     */
    List<Long> listPermissionIdsByRoleId(Long roleId);

    /**
     * 重新分配某角色的权限（全量覆盖）；admin 角色的权限不可改。
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 按用户 ID 聚合其全部角色对应的权限码（去重）。
     *
     * @param userId 用户 ID（可为 {@code null}）
     * @return 权限码集合（不可为 {@code null}）
     */
    Set<String> listPermissionCodesByUserId(Long userId);
}
