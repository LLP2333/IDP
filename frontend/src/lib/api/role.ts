import { http } from "./http";
import type { PageResp, RolePageQuery, RoleReq, RoleResp } from "./types";

const BASE_URL = "/system/role";

export function listRole(query: RolePageQuery) {
  return http.get<PageResp<RoleResp>>(BASE_URL, query as Record<string, unknown>);
}

export function listAllRole() {
  return http.get<RoleResp[]>(`${BASE_URL}/list`);
}

export function getRole(id: number | string) {
  return http.get<RoleResp>(`${BASE_URL}/${id}`);
}

export function addRole(data: RoleReq) {
  return http.post<number>(BASE_URL, data);
}

export function updateRole(id: number | string, data: RoleReq) {
  return http.put<void>(`${BASE_URL}/${id}`, data);
}

export function deleteRole(ids: Array<number | string>) {
  return http.del<void>(BASE_URL, { ids });
}

export function listRoleUserId(id: number | string) {
  return http.get<number[]>(`${BASE_URL}/${id}/user/id`);
}
