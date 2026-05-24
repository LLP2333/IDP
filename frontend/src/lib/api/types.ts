/**
 * 与后端 DTO 对齐的类型定义。
 *
 * 命名约定（与 `backend/src/main/java/com/qvqw/idp/<module>/model/` 下保持一致）：
 * - `XxxReq`：请求 DTO
 * - `XxxResp`：响应 DTO
 * - `XxxQuery`：分页 / 列表查询条件
 *
 * 后端字段为 `null` 时这里也用 `T | null`，避免业务层把 `null` 视为 `undefined`。
 */

/** 后端统一响应结构 `R<T>`。 */
export interface ApiResponse<T> {
  /** 业务码：0=成功，其余表示业务错误。 */
  code: number;
  /** 业务描述，前端通常直接展示。 */
  msg: string;
  /** 业务数据。 */
  data: T;
  /** 服务端响应时间戳（毫秒）。 */
  timestamp: number;
}

/** 后端通用分页响应。 */
export interface PageResp<T> {
  /** 当前页数据列表。 */
  list: T[];
  /** 总记录数。 */
  total: number;
  /** 当前页码（从 1 开始）。 */
  page: number;
  /** 每页数量。 */
  size: number;
}

// ============== auth ==============

/** 登录请求。 */
export interface LoginReq {
  username: string;
  password: string;
}

/** 登录响应。 */
export interface LoginResp {
  /** JWT 字符串，需放入后续请求的 `Authorization` 头。 */
  token: string;
  /** Token 有效期（秒）。 */
  expires: number;
}

/** 当前登录用户信息。 */
export interface UserInfo {
  id: number;
  username: string;
  nickname: string | null;
  avatar: string | null;
  email: string | null;
  phone: string | null;
  /** 角色编码列表，如 `["admin"]`。 */
  roles: string[];
  /** 权限标识列表（预留，当前固定为空数组）。 */
  permissions: string[];
}

// ============== user ==============

/** 用户列表项。 */
export interface UserResp {
  id: number;
  username: string;
  nickname: string | null;
  email: string | null;
  phone: string | null;
  /** 性别：0=未知, 1=男, 2=女。 */
  gender: number;
  avatar: string | null;
  /** 状态：1=启用，0=禁用。 */
  status: number;
  /** 是否系统内置（系统内置不可删除 / 禁用）。 */
  isSystem: boolean;
  createdAt: string;
  updatedAt: string | null;
}

/** 用户详情（包含角色信息与扩展字段）。 */
export interface UserDetailResp extends UserResp {
  description: string | null;
  roleIds: number[];
  roleCodes: string[];
  roleNames: string[];
  /** 最后一次重置密码时间。 */
  pwdResetAt: string | null;
}

/** 用户分页查询条件（含分页参数）。 */
export interface UserPageQuery {
  username?: string;
  status?: number;
  page?: number;
  size?: number;
}

/** 新增用户请求。 */
export interface UserCreateReq {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  phone?: string;
  gender?: number;
  description?: string;
  status?: number;
  roleIds?: number[];
}

/** 修改用户请求（用户名不可改）。 */
export interface UserUpdateReq {
  nickname?: string;
  email?: string;
  phone?: string;
  gender?: number;
  description?: string;
  status?: number;
  roleIds?: number[];
}

/** 重置密码请求。 */
export interface UserPasswordResetReq {
  newPassword: string;
}

/** 分配角色请求（全量覆盖）。 */
export interface UserRoleUpdateReq {
  roleIds: number[];
}

// ============== role ==============

/** 角色信息。 */
export interface RoleResp {
  id: number;
  name: string;
  code: string;
  description: string | null;
  sort: number;
  status: number;
  isSystem: boolean;
  createdAt: string;
  updatedAt: string | null;
}

/** 角色分页查询条件。 */
export interface RolePageQuery {
  keyword?: string;
  status?: number;
  page?: number;
  size?: number;
}

/** 角色新增/修改请求。 */
export interface RoleReq {
  name: string;
  code: string;
  description?: string;
  sort?: number;
  status?: number;
}
