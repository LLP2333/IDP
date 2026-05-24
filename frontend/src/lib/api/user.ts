import { http } from "./http";
import type {
  PageResp,
  UserCreateReq,
  UserDetailResp,
  UserPageQuery,
  UserPasswordResetReq,
  UserResp,
  UserRoleUpdateReq,
  UserUpdateReq,
} from "./types";

const BASE_URL = "/system/user";

export function listUser(query: UserPageQuery) {
  return http.get<PageResp<UserResp>>(BASE_URL, query as Record<string, unknown>);
}

export function getUser(id: number | string) {
  return http.get<UserDetailResp>(`${BASE_URL}/${id}`);
}

export function addUser(data: UserCreateReq) {
  return http.post<number>(BASE_URL, data);
}

export function updateUser(id: number | string, data: UserUpdateReq) {
  return http.put<void>(`${BASE_URL}/${id}`, data);
}

export function deleteUser(ids: Array<number | string>) {
  return http.del<void>(BASE_URL, { ids });
}

export function resetUserPassword(id: number | string, data: UserPasswordResetReq) {
  return http.patch<void>(`${BASE_URL}/${id}/password`, data);
}

export function updateUserRole(id: number | string, data: UserRoleUpdateReq) {
  return http.patch<void>(`${BASE_URL}/${id}/role`, data);
}
