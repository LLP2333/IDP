import { http } from "./http";
import type { PageResp, RolePageQuery, RoleReq, RoleResp } from "./types";

/** 角色管理在后端的统一前缀。 */
const BASE_URL = "/system/role";

/**
 * 角色分页查询。
 *
 * @param query 关键字 + 状态 + 分页参数
 * @returns 分页结果
 */
export function listRole(query: RolePageQuery) {
  return http.get<PageResp<RoleResp>>(BASE_URL, query as Record<string, unknown>);
}

/**
 * 不分页查询所有角色，用于表单中的角色选择器等场景。
 *
 * @returns 角色列表（按 sort 升序）
 */
export function listAllRole() {
  return http.get<RoleResp[]>(`${BASE_URL}/list`);
}

/**
 * 查询单个角色详情。
 *
 * @param id 角色 ID
 * @returns 角色信息
 */
export function getRole(id: number | string) {
  return http.get<RoleResp>(`${BASE_URL}/${id}`);
}

/**
 * 新增角色，code 必须唯一。
 *
 * @param data 角色请求体
 * @returns 新建角色 ID
 */
export function addRole(data: RoleReq) {
  return http.post<number>(BASE_URL, data);
}

/**
 * 修改角色信息。
 *
 * @param id   角色 ID
 * @param data 角色请求体（系统内置角色不允许改 code）
 */
export function updateRole(id: number | string, data: RoleReq) {
  return http.put<void>(`${BASE_URL}/${id}`, data);
}

/**
 * 批量删除角色；系统内置或仍被引用的角色不允许删除。
 *
 * @param ids 角色 ID 列表
 */
export function deleteRole(ids: Array<number | string>) {
  return http.del<void>(BASE_URL, { ids });
}

/**
 * 列出某角色下的所有用户 ID。
 *
 * @param id 角色 ID
 * @returns 用户 ID 列表
 */
export function listRoleUserId(id: number | string) {
  return http.get<number[]>(`${BASE_URL}/${id}/user/id`);
}
