package com.qvqw.idp.permission;

import com.qvqw.idp.permission.model.query.PermissionQuery;
import com.qvqw.idp.permission.model.req.PermissionReq;
import com.qvqw.idp.permission.model.resp.PermissionResp;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 权限服务（对外暴露）。
 *
 * <p>权限模块只关心 {@code idp_sys_permission} 表自身的 CRUD；用户 → 角色 → 权限的查询链
 * 由 role 模块负责，避免与 role 形成循环依赖。</p>
 */
public interface PermissionService {

    List<PermissionResp> list(PermissionQuery query);

    /**
     * 列出树形结构（按 sort 升序，全量返回不过滤状态，前端自行展示禁用样式）。
     *
     * @return 树（顶级节点列表）
     */
    List<PermissionResp> tree();

    PermissionResp get(Long id);

    Long create(PermissionReq req);

    void update(Long id, PermissionReq req);

    void delete(List<Long> ids);

    /**
     * 按权限 ID 集合批量获取权限码（去重，仅返回启用状态）。
     *
     * @param ids 权限 ID 集合（可为 {@code null} / 空）
     * @return code 集合；输入为空时返回空集
     */
    Set<String> listCodesByIds(Collection<Long> ids);

    /**
     * 仅返回 “系统内置” 的权限 ID（用于 admin 角色默认绑定全部权限）。
     *
     * @return 内置权限 ID 列表
     */
    List<Long> listSystemPermissionIds();
}
