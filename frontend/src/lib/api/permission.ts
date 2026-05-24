import { http } from "./http";
import type { PermissionQuery, PermissionReq, PermissionResp } from "./types";

/** 权限模块在后端的统一前缀。 */
const BASE_URL = "/system/permission";

/** 查询权限平铺列表。 */
export function listPermission(query: PermissionQuery = {}) {
  return http.get<PermissionResp[]>(BASE_URL, {
    keyword: query.keyword,
    status: query.status,
    type: query.type,
  });
}

/** 查询权限树（含父子结构，用于角色分配 / 树形管理）。 */
export function getPermissionTree() {
  return http.get<PermissionResp[]>(`${BASE_URL}/tree`);
}

/** 按 ID 获取单个权限详情。 */
export function getPermission(id: number) {
  return http.get<PermissionResp>(`${BASE_URL}/${id}`);
}

/** 新增权限。 */
export function createPermission(req: PermissionReq) {
  return http.post<number>(BASE_URL, req);
}

/** 更新权限。 */
export function updatePermission(id: number, req: PermissionReq) {
  return http.put<void>(`${BASE_URL}/${id}`, req);
}

/** 批量删除权限（仅非内置可删）。 */
export function deletePermission(ids: number[]) {
  return http.del<void>(BASE_URL, { ids });
}

/** 获取指定角色拥有的权限 ID 列表。 */
export function getRolePermission(roleId: number) {
  return http.get<number[]>(`/system/role/${roleId}/permission`);
}

/** 设置指定角色的权限（全量覆盖）。 */
export function assignRolePermission(roleId: number, permissionIds: number[]) {
  return http.put<void>(`/system/role/${roleId}/permission`, { permissionIds });
}
