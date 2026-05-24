import { http } from "./http";
import type {
  LoginConfigResp,
  OptionImageUploadReq,
  OptionImageUploadResp,
  OptionQuery,
  OptionReq,
  OptionResp,
  OptionValueResetReq,
  SiteConfigResp,
} from "./types";

/** 系统参数模块在后端的统一前缀。 */
const BASE_URL = "/system/option";

/**
 * 查询系统参数列表。
 *
 * @param query 类别 / 编码数组过滤条件
 * @returns 命中条件的全部参数（按 category, code 排序）
 */
export function listOption(query: OptionQuery = {}) {
  return http.get<OptionResp[]>(BASE_URL, {
    category: query.category,
    codes: query.codes,
  });
}

/**
 * 批量更新系统参数。
 *
 * 后端会先校验密码策略字段、图片字段格式，再写库并清空 Redis 缓存。
 *
 * @param reqs 待更新的参数列表
 */
export function updateOption(reqs: OptionReq[]) {
  return http.put<void>(BASE_URL, reqs);
}

/**
 * 重置系统参数为默认值。
 *
 * 至少指定 {@code category} 或 {@code codes} 之一。
 *
 * @param req 重置条件
 */
export function resetOption(req: OptionValueResetReq) {
  return http.patch<void>(`${BASE_URL}/value`, req);
}

/**
 * 上传 base64 编码图片（logo / favicon）。
 *
 * 后端会校验 mime 与体积，校验通过后直接把 Data URL 写入对应 code 的 option。
 *
 * @param req 上传请求
 * @returns 实际写入的 Data URL
 */
export function uploadOptionImage(req: OptionImageUploadReq) {
  return http.post<OptionImageUploadResp>(`${BASE_URL}/image`, req);
}

/**
 * 获取登录页公开网站配置（不需要登录）。
 *
 * @returns 站点标题 / Logo / Favicon 等
 */
export function getSiteConfigPublic() {
  return http.get<SiteConfigResp>(`${BASE_URL}/site`, undefined, {
    skipUnauthorizedHandler: true,
  });
}

/**
 * 获取登录页公开登录配置（不需要登录）。
 *
 * @returns 是否启用验证码等
 */
export function getLoginConfigPublic() {
  return http.get<LoginConfigResp>(`${BASE_URL}/login`, undefined, {
    skipUnauthorizedHandler: true,
  });
}
