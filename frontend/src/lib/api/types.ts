/**
 * 与后端 DTO 对齐的类型定义。
 */

export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
  timestamp: number;
}

export interface PageResp<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

// ============== auth ==============

export interface LoginReq {
  username: string;
  password: string;
}

export interface LoginResp {
  token: string;
  expires: number;
}

export interface UserInfo {
  id: number;
  username: string;
  nickname: string | null;
  avatar: string | null;
  email: string | null;
  phone: string | null;
  roles: string[];
  permissions: string[];
}

// ============== user ==============

export interface UserResp {
  id: number;
  username: string;
  nickname: string | null;
  email: string | null;
  phone: string | null;
  gender: number;
  avatar: string | null;
  status: number;
  isSystem: boolean;
  createdAt: string;
  updatedAt: string | null;
}

export interface UserDetailResp extends UserResp {
  description: string | null;
  roleIds: number[];
  roleCodes: string[];
  roleNames: string[];
  pwdResetAt: string | null;
}

export interface UserPageQuery {
  username?: string;
  status?: number;
  page?: number;
  size?: number;
}

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

export interface UserUpdateReq {
  nickname?: string;
  email?: string;
  phone?: string;
  gender?: number;
  description?: string;
  status?: number;
  roleIds?: number[];
}

export interface UserPasswordResetReq {
  newPassword: string;
}

export interface UserRoleUpdateReq {
  roleIds: number[];
}

// ============== role ==============

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

export interface RolePageQuery {
  keyword?: string;
  status?: number;
  page?: number;
  size?: number;
}

export interface RoleReq {
  name: string;
  code: string;
  description?: string;
  sort?: number;
  status?: number;
}
