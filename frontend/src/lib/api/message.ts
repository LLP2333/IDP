import { http } from "./http";
import type { MessagePageQuery, MessageResp, PageResp } from "./types";

/** 消息中心后端统一前缀。 */
const BASE_URL = "/system/message";

/**
 * 当前用户消息分页查询。
 */
export function listMessage(query: MessagePageQuery) {
  return http.get<PageResp<MessageResp>>(BASE_URL, query as Record<string, unknown>);
}

/**
 * 未读消息计数（顶栏 bell 角标用）。
 */
export function getUnreadCount() {
  return http.get<{ count: number }>(`${BASE_URL}/unread-count`);
}

/**
 * 标记某条消息已读。
 */
export function readMessage(id: number | string) {
  return http.post<void>(`${BASE_URL}/${id}/read`);
}

/**
 * 一键标记全部已读。
 */
export function readAllMessage() {
  return http.post<void>(`${BASE_URL}/read-all`);
}
