import { http } from "./http";
import type {
  LogDetailResp,
  LogPageQuery,
  LogResp,
  OnlineUserPageQuery,
  OnlineUserResp,
  PageResp,
} from "./types";

/** 在线用户监控后端统一前缀。 */
const ONLINE_BASE_URL = "/monitor/online";
/** 系统日志后端统一前缀。 */
const LOG_BASE_URL = "/system/log";

/** 查询在线用户列表。 */
export function listOnlineUser(query: OnlineUserPageQuery) {
  return http.get<PageResp<OnlineUserResp>>(
    ONLINE_BASE_URL,
    query as Record<string, unknown>,
  );
}

/** 强退在线用户。 */
export function kickoutOnlineUser(token: string) {
  return http.del<void>(`${ONLINE_BASE_URL}/${encodeURIComponent(token)}`);
}

/** 查询系统日志列表。 */
export function listLog(query: LogPageQuery) {
  return http.get<PageResp<LogResp>>(LOG_BASE_URL, query as Record<string, unknown>);
}

/** 查询系统日志详情。 */
export function getLog(id: string) {
  return http.get<LogDetailResp>(`${LOG_BASE_URL}/${id}`);
}

/** 导出登录日志。 */
export function exportLoginLog(query: LogPageQuery) {
  return http.download(`${LOG_BASE_URL}/export/login`, {
    query: query as Record<string, unknown>,
    filename: "登录日志.xlsx",
  });
}

/** 导出操作日志。 */
export function exportOperationLog(query: LogPageQuery) {
  return http.download(`${LOG_BASE_URL}/export/operation`, {
    query: query as Record<string, unknown>,
    filename: "操作日志.xlsx",
  });
}
