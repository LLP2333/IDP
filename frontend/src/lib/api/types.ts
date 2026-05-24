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
  /** 验证码 ID（启用验证码时必填）。 */
  captchaId?: string;
  /** 用户输入的验证码（启用验证码时必填）。 */
  captcha?: string;
}

/** 登录响应。 */
export interface LoginResp {
  /** JWT 字符串，需放入后续请求的 `Authorization` 头。 */
  token: string;
  /** Token 有效期（秒）。 */
  expires: number;
  /** 密码是否已过期（true 时需强制改密）。 */
  passwordExpired?: boolean;
  /** 是否处于密码到期预警期。 */
  passwordWarning?: boolean;
  /** 距密码到期剩余天数（仅 passwordWarning 为 true 时有意义）。 */
  passwordExpiresInDays?: number | null;
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
  /** 按钮级权限编码列表，如 `["system:user:add"]`。 */
  permissions: string[];
}

/** 验证码响应（后端返回 SVG 的 Data URL）。 */
export interface CaptchaResp {
  /** 验证码 ID，登录时回传给后端核对。 */
  captchaId: string;
  /** SVG Data URL，可直接作为 `<img src>`。 */
  image: string;
  /** 有效期（秒）。 */
  expiresIn: number;
}

/** 自助修改密码请求。 */
export interface UserPasswordChangeReq {
  oldPassword: string;
  newPassword: string;
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

// ============== option ==============

/** 系统参数类别。 */
export type OptionCategory = "SITE" | "PASSWORD" | "LOGIN";

/** 单条系统参数。 */
export interface OptionResp {
  id: number;
  category: OptionCategory;
  name: string;
  code: string;
  /**
   * 实际生效值。
   *
   * 后端已经做过 `value` 为空时回落到 `defaultValue` 的处理，
   * 前端拿到的就是最终展示值，可能为 `null`（如未配置的图片）。
   */
  value: string | null;
  description: string | null;
}

/** 系统参数查询条件。 */
export interface OptionQuery {
  category?: OptionCategory;
  codes?: string[];
}

/** 单条系统参数更新请求。 */
export interface OptionReq {
  id: number;
  /** 参数键。后端会校验 id 与 code 一一对应，防止误改其它配置。 */
  code: string;
  value: string | null;
}

/** 重置系统参数请求。 */
export interface OptionValueResetReq {
  category?: OptionCategory;
  codes?: string[];
}

/** 上传图片请求（base64 Data URL）。 */
export interface OptionImageUploadReq {
  code: string;
  dataUrl: string;
}

/** 上传图片响应。 */
export interface OptionImageUploadResp {
  code: string;
  dataUrl: string;
}

/** 公开网站配置（登录页可见）。 */
export interface SiteConfigResp {
  title: string | null;
  copyright: string | null;
  description: string | null;
  logo: string | null;
  favicon: string | null;
  /** 备案号（如 “粤 ICP 备 xxxxxxxx 号”）。 */
  beian: string | null;
}

/** 公开登录配置（登录页可见）。 */
export interface LoginConfigResp {
  captchaEnabled: boolean;
}

// ============== permission ==============

/** 权限类型：1=菜单，2=按钮。 */
export type PermissionType = 1 | 2;

/** 权限项。 */
export interface PermissionResp {
  id: number;
  code: string;
  name: string;
  type: PermissionType;
  parentId: number;
  sort: number;
  status: number;
  isSystem: boolean;
  description: string | null;
  children?: PermissionResp[];
}

/** 权限查询条件。 */
export interface PermissionQuery {
  keyword?: string;
  status?: number;
  type?: PermissionType;
}

/** 权限新增/修改请求。 */
export interface PermissionReq {
  code: string;
  name: string;
  type: PermissionType;
  parentId?: number;
  sort?: number;
  status?: number;
  description?: string;
}
