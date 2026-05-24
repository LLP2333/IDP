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

/** 用户管理在后端的统一前缀。 */
const BASE_URL = "/system/user";

/**
 * 用户分页查询。
 *
 * @param query 模糊匹配条件 + 分页参数
 * @returns 分页结果
 */
export function listUser(query: UserPageQuery) {
  return http.get<PageResp<UserResp>>(BASE_URL, query as Record<string, unknown>);
}

/**
 * 查询单个用户详情（含其角色 ID / code / name）。
 *
 * @param id 用户 ID
 * @returns 用户详情
 */
export function getUser(id: number | string) {
  return http.get<UserDetailResp>(`${BASE_URL}/${id}`);
}

/**
 * 新增用户。
 *
 * @param data 用户创建请求
 * @returns 新建用户 ID
 */
export function addUser(data: UserCreateReq) {
  return http.post<number>(BASE_URL, data);
}

/**
 * 修改用户基本信息；用户名不可改。
 *
 * @param id   用户 ID
 * @param data 待更新字段（`undefined` 字段不会覆盖）
 */
export function updateUser(id: number | string, data: UserUpdateReq) {
  return http.put<void>(`${BASE_URL}/${id}`, data);
}

/**
 * 批量删除用户；系统内置用户不允许删除。
 *
 * @param ids 用户 ID 列表
 */
export function deleteUser(ids: Array<number | string>) {
  return http.del<void>(BASE_URL, { ids });
}

/**
 * 管理员重置用户密码。
 *
 * @param id   用户 ID
 * @param data 新密码
 */
export function resetUserPassword(id: number | string, data: UserPasswordResetReq) {
  return http.patch<void>(`${BASE_URL}/${id}/password`, data);
}

/**
 * 重新分配用户角色（全量覆盖）。
 *
 * @param id   用户 ID
 * @param data 角色 ID 列表
 */
export function updateUserRole(id: number | string, data: UserRoleUpdateReq) {
  return http.patch<void>(`${BASE_URL}/${id}/role`, data);
}
