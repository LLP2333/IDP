import { http } from "./http";
import type {
  CaptchaResp,
  LoginReq,
  LoginResp,
  UserBasicInfoUpdateReq,
  UserInfo,
  UserPasswordChangeReq,
} from "./types";

/** 认证模块在后端的统一前缀。 */
const BASE_URL = "/auth";

/**
 * 账号密码登录。
 *
 * 成功后后端返回 JWT 与有效期，调用方需要把 token 写入 `auth-store`。
 * 由于本接口本身就是 “未登录” 状态发起，因此显式 `skipUnauthorizedHandler`
 * 避免触发 401 自动跳转。
 *
 * @param req 登录请求体
 * @returns JWT 与过期秒数
 */
export function login(req: LoginReq) {
  return http.post<LoginResp>(`${BASE_URL}/login`, req, {
    skipUnauthorizedHandler: true,
  });
}

/**
 * 登出。
 *
 * 通知后端使当前 JWT 立即失效；前端仍需自行清理 `auth-store`。
 */
export function logout() {
  return http.post<void>(`${BASE_URL}/logout`);
}

/**
 * 获取当前登录用户的完整信息（含角色编码、邮箱、手机号等）。
 *
 * 通常在登录成功后或刷新页面 hydrate 后调用。
 *
 * @returns 用户信息
 */
export function getUserInfo() {
  return http.get<UserInfo>(`${BASE_URL}/user/info`);
}

/**
 * 获取登录验证码（SVG）。
 *
 * 后端会把对应答案写入 Redis（或本地 fallback store），TTL 通常 2 分钟。
 * 浏览器拿到 `image` 后直接放到 `<img src>` 渲染即可。
 *
 * @returns 验证码 ID 与 SVG Data URL
 */
export function getCaptcha() {
  return http.get<CaptchaResp>(`${BASE_URL}/captcha`, undefined, {
    skipUnauthorizedHandler: true,
  });
}

/**
 * 当前登录用户修改自己的密码。
 *
 * 需要在请求头携带 token；修改成功后建议主动登出，让用户使用新密码重新登录。
 *
 * @param req 旧密码 + 新密码
 */
export function changeCurrentPassword(req: UserPasswordChangeReq) {
  return http.post<void>("/system/user/password", req);
}

/**
 * 当前登录用户自助修改基本信息（个人中心使用）。
 *
 * 对应后端 `PUT /system/user/profile`。任意登录用户都可以调用，无需
 * `system:user:*` 权限；只允许更新昵称 / 邮箱 / 手机 / 性别。
 * 状态、角色、备注等需通过用户管理接口由管理员调整。
 *
 * @param req 待修改字段；`undefined` 字段不会被覆盖
 */
export function updateCurrentUserBasicInfo(req: UserBasicInfoUpdateReq) {
  return http.put<void>("/system/user/profile", req);
}
