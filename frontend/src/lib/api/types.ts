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
  /** 性别：0=未知, 1=男, 2=女。可能为 `null`（数据缺失时）。 */
  gender: number | null;
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

/**
 * 当前登录用户自助修改基本信息请求。
 *
 * 仅承载非敏感字段：昵称 / 邮箱 / 手机 / 性别。
 * - 字段为 `undefined` 时不修改；
 * - `email` / `phone` 为 `""` 时表示主动清空（后端会写为 `null`）。
 */
export interface UserBasicInfoUpdateReq {
  nickname?: string;
  email?: string;
  phone?: string;
  /** 性别：0=未知, 1=男, 2=女。 */
  gender?: number;
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

// ============== menu ==============

/**
 * 菜单类型。
 *
 * - `1` 目录：纯分组节点，没有真实页面，仅用于前端侧边栏一级菜单分组。
 * - `2` 菜单：对应前端一个路由 / 页面。
 * - `3` 按钮：挂在菜单下的细粒度权限点，`permission` 字段存按钮权限码（如 `system:user:add`）。
 */
export type MenuType = 1 | 2 | 3;

/** 单条菜单（同时承担 “路由元数据 + 按钮权限” 双重角色）。 */
export interface MenuResp {
  id: number;
  /** 标题（前端表格 / 侧边栏直接展示）。 */
  title: string;
  /** 父节点 ID，顶级为 0。 */
  parentId: number;
  type: MenuType;
  /** 路由地址（按钮类型为 `null`）。 */
  path: string | null;
  /** 组件名称（按钮类型为 `null`）。 */
  name: string | null;
  /** 组件路径（按钮类型为 `null`）。 */
  component: string | null;
  /** 重定向地址。 */
  redirect: string | null;
  /** 图标标识，前端按约定映射为 lucide 图标。 */
  icon: string | null;
  /** 是否外链；true 时 `path` 必须是 http(s) URL。 */
  isExternal: boolean;
  /** 是否启用 keep-alive 缓存（仅菜单类型有意义）。 */
  isCache: boolean;
  /** 是否在侧边栏隐藏（仍可访问，仅不显示）。 */
  isHidden: boolean;
  /** 按钮权限码，仅 type=3 节点；其他类型为 `null`。 */
  permission: string | null;
  sort: number;
  /** 状态：1=启用，0=禁用。 */
  status: number;
  isSystem: boolean;
  description: string | null;
  createdAt: string;
  updatedAt: string | null;
  /** 子节点；仅在 tree 接口 / 动态路由接口返回时填充，平铺接口为空数组。 */
  children?: MenuResp[];
}

/** 菜单查询条件。 */
export interface MenuQuery {
  title?: string;
  path?: string;
  permission?: string;
  status?: number;
  type?: MenuType;
}

/** 菜单新增 / 修改请求。 */
export interface MenuReq {
  title: string;
  parentId: number;
  type: MenuType;
  path?: string | null;
  name?: string | null;
  component?: string | null;
  redirect?: string | null;
  icon?: string | null;
  isExternal?: boolean;
  isCache?: boolean;
  isHidden?: boolean;
  permission?: string | null;
  sort?: number;
  status?: number;
  description?: string | null;
}

// ============== dict ==============

/** 字典信息。 */
export interface DictResp {
  id: number;
  name: string;
  code: string;
  description: string | null;
  isSystem: boolean;
  createdAt: string;
  updatedAt: string | null;
}

/** 字典新增 / 修改请求。 */
export interface DictReq {
  name: string;
  code: string;
  description?: string;
}

/** 字典明细。 */
export interface DictItemResp {
  id: number;
  dictId: number;
  label: string;
  value: string;
  color: string | null;
  sort: number;
  status: number;
  isSystem: boolean;
}

/** 字典明细新增 / 修改请求。 */
export interface DictItemReq {
  label: string;
  value: string;
  color?: string | null;
  sort?: number;
  status?: number;
}

// ============== notice ==============

/** 通知范围：1=所有人, 2=指定用户。 */
export type NoticeScope = 1 | 2;

/** 通知方式：1=系统消息, 2=登录弹窗。 */
export type NoticeMethod = 1 | 2;

/** 公告状态：1=草稿, 2=待发布, 3=已发布。 */
export type NoticeStatus = 1 | 2 | 3;

/** 公告列表项。 */
export interface NoticeResp {
  id: number;
  title: string;
  type: string;
  noticeScope: NoticeScope;
  noticeMethods: NoticeMethod[] | null;
  isTiming: boolean;
  publishTime: string | null;
  isTop: boolean;
  status: NoticeStatus;
  isRead: boolean | null;
  createUserString: string | null;
  createdBy: number | null;
  createdAt: string;
  updatedAt: string | null;
}

/** 公告详情：列表 + 正文 + noticeUsers。 */
export interface NoticeDetailResp extends NoticeResp {
  content: string;
  noticeUsers: number[] | null;
}

/** 公告分页查询条件。 */
export interface NoticePageQuery {
  title?: string;
  type?: string;
  status?: NoticeStatus;
  page?: number;
  size?: number;
}

/** 公告新增 / 修改请求。 */
export interface NoticeReq {
  title: string;
  content: string;
  type: string;
  noticeScope: NoticeScope;
  noticeUsers?: number[];
  noticeMethods?: NoticeMethod[];
  isTiming: boolean;
  /** ISO 字符串，如 `2026-06-01T10:00:00`。 */
  publishTime?: string | null;
  isTop?: boolean;
  /** 1=草稿，3=发布（后端按 isTiming 自动转 2）。 */
  status: NoticeStatus;
}

/** Dashboard 公告摘要。 */
export interface DashboardNoticeResp {
  id: number;
  title: string;
  type: string;
  isTop: boolean;
  publishTime: string | null;
  isRead: boolean;
}

// ============== message ==============

/** 站内消息。 */
export interface MessageResp {
  id: number;
  type: number;
  title: string;
  content: string;
  path: string | null;
  isRead: boolean;
  readTime: string | null;
  createdAt: string;
}

/** 消息收件箱查询条件。 */
export interface MessagePageQuery {
  title?: string;
  isRead?: boolean;
  page?: number;
  size?: number;
}

// ============== monitor ==============

/** 在线用户列表项。 */
export interface OnlineUserResp {
  token: string;
  username: string;
  nickname: string | null;
  ip: string | null;
  address: string | null;
  browser: string | null;
  os: string | null;
  loginTime: string;
  lastActiveTime: string | null;
}

/** 在线用户分页查询条件。 */
export interface OnlineUserPageQuery {
  nickname?: string;
  loginTime?: string[];
  sort?: string[];
  page?: number;
  size?: number;
}

/** 系统日志列表项。 */
export interface LogResp {
  id: string;
  description: string | null;
  module: string | null;
  timeTaken: number;
  ip: string | null;
  address: string | null;
  browser: string | null;
  os: string | null;
  status: number;
  errorMsg: string | null;
  createUserString: string | null;
  createTime: string;
}

/** 系统日志详情。 */
export interface LogDetailResp extends LogResp {
  traceId: string | null;
  requestUrl: string | null;
  requestMethod: string | null;
  requestHeaders: string | null;
  requestBody: string | null;
  statusCode: number | null;
  responseHeaders: string | null;
  responseBody: string | null;
}

/** 系统日志分页 / 导出查询条件。 */
export interface LogPageQuery {
  description?: string;
  module?: string;
  ip?: string;
  createUserString?: string;
  createTime?: string[];
  status?: number;
  sort?: string[];
  page?: number;
  size?: number;
}

// ============== storage ==============

/** 存储类型：1=本地，2=S3 协议（MinIO/AWS S3/OSS/COS）。 */
export type StorageType = 1 | 2;

/** 单个存储引擎信息。 */
export interface StorageResp {
  id: number;
  name: string;
  code: string;
  type: StorageType;
  accessKey: string | null;
  /** 密文永远脱敏为 '******'。 */
  secretKey: string | null;
  endpoint: string | null;
  bucketName: string | null;
  domain: string | null;
  recycleBinEnabled: boolean;
  recycleBinPath: string | null;
  description: string | null;
  isDefault: boolean;
  sort: number;
  /** 1=启用，2=禁用。 */
  status: number;
  createdAt: string;
  updatedAt: string | null;
}

/** 存储查询条件。 */
export interface StorageQuery {
  type?: StorageType;
  keyword?: string;
}

/** 存储新增请求。 */
export interface StorageCreateReq {
  name: string;
  code: string;
  type: StorageType;
  accessKey?: string;
  secretKey?: string;
  endpoint?: string;
  bucketName: string;
  domain?: string;
  recycleBinEnabled: boolean;
  recycleBinPath?: string;
  description?: string;
  sort: number;
  status: number;
}

/** 存储修改请求。 */
export interface StorageUpdateReq {
  name: string;
  accessKey?: string;
  /** 留空表示不修改原值。 */
  secretKey?: string;
  endpoint?: string;
  bucketName: string;
  domain?: string;
  description?: string;
  sort: number;
  status: number;
}

/** 切换存储状态请求。 */
export interface StorageStatusUpdateReq {
  status: number;
}

// ============== file ==============

/** 文件类型：0=目录,1=其他,2=图片,3=文档,4=视频,5=音频。 */
export type FileType = 0 | 1 | 2 | 3 | 4 | 5;

/** 文件 / 文件夹信息。 */
export interface FileResp {
  id: number;
  name: string;
  originalName: string;
  size: number;
  url: string | null;
  thumbnailUrl: string | null;
  parentPath: string;
  path: string;
  extension: string | null;
  contentType: string | null;
  type: FileType;
  sha256: string | null;
  metadata: string | null;
  storageId: number;
  storageName: string | null;
  createdAt: string;
  updatedAt: string | null;
  deletedAt?: string | null;
}

/** 文件分页查询条件。 */
export interface FilePageQuery {
  originalName?: string;
  parentPath?: string;
  type?: FileType;
  page?: number;
  size?: number;
}

/** 文件统计明细项。 */
export interface FileStatisticsDetail {
  type: FileType;
  name: string;
  size: number;
  number: number;
}

/** 文件统计响应。 */
export interface FileStatisticsResp {
  size: number;
  number: number;
  data: FileStatisticsDetail[];
}

/** 文件上传响应。 */
export interface FileUploadResp {
  id: number;
  url: string;
  thumbnailUrl: string | null;
  metadata: string | null;
}

/** 文件夹大小响应。 */
export interface FileDirCalcSizeResp {
  size: number;
}

/** 文件重命名请求。 */
export interface FileUpdateReq {
  originalName: string;
}

/** 创建文件夹请求。 */
export interface FileCreateDirReq {
  parentPath: string;
  originalName: string;
}

// ============== multipart-upload ==============

/** 分片上传初始化请求。 */
export interface MultipartUploadInitReq {
  fileName: string;
  fileSize: number;
  chunkSize: number;
  sha256: string;
  parentPath?: string;
}

/** 分片上传初始化响应。 */
export interface MultipartUploadInitResp {
  /** 命中秒传时为 null；否则非空。 */
  uploadId: string | null;
  /** 命中秒传时返回已有文件信息，否则 null。 */
  existing: FileResp | null;
}

/** 单分片上传响应。 */
export interface MultipartUploadPartResp {
  partNumber: number;
  etag: string;
}
