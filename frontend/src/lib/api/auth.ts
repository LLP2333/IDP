import { http } from "./http";
import type { LoginReq, LoginResp, UserInfo } from "./types";

const BASE_URL = "/auth";

export function login(req: LoginReq) {
  return http.post<LoginResp>(`${BASE_URL}/login`, req, {
    skipUnauthorizedHandler: true,
  });
}

export function logout() {
  return http.post<void>(`${BASE_URL}/logout`);
}

export function getUserInfo() {
  return http.get<UserInfo>(`${BASE_URL}/user/info`);
}
