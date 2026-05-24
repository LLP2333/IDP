import { http } from "./http";
import type { LoginReq, LoginResp, UserInfo } from "./types";

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
