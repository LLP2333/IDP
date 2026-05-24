import { http } from "./http";
import type {
  DashboardNoticeResp,
  NoticeDetailResp,
  NoticePageQuery,
  NoticeReq,
  NoticeResp,
  PageResp,
} from "./types";

/** 公告在后端的统一前缀。 */
const BASE_URL = "/system/notice";

/**
 * 公告分页查询（管理端）。
 */
export function listNotice(query: NoticePageQuery) {
  return http.get<PageResp<NoticeResp>>(BASE_URL, query as Record<string, unknown>);
}

/**
 * 公告详情。
 */
export function getNotice(id: number | string) {
  return http.get<NoticeDetailResp>(`${BASE_URL}/${id}`);
}

/**
 * 新增公告（草稿 / 立即发布 / 定时发布由 status + isTiming 推导）。
 */
export function addNotice(data: NoticeReq) {
  return http.post<number>(BASE_URL, data);
}

/**
 * 修改公告。已发布的公告不允许修改通知范围 / 方式 / 定时。
 */
export function updateNotice(id: number | string, data: NoticeReq) {
  return http.put<void>(`${BASE_URL}/${id}`, data);
}

/**
 * 批量删除公告（联动清空 notice_log）。
 */
export function deleteNotice(ids: Array<number | string>) {
  return http.del<void>(BASE_URL, { ids });
}

/**
 * 当前用户登录弹窗公告：未读 + method 含 POPUP 的已发布公告。
 *
 * @returns 公告详情列表（含正文）
 */
export function listPopupNotice() {
  return http.get<NoticeDetailResp[]>(`${BASE_URL}/popup`);
}

/**
 * 标记某条公告已读。
 */
export function readNotice(id: number | string) {
  return http.post<void>(`${BASE_URL}/${id}/read`);
}

/**
 * Dashboard 最新公告摘要。
 *
 * @param limit 摘要数量，默认 5，最大 50。
 */
export function listDashboardNotice(limit = 5) {
  return http.get<DashboardNoticeResp[]>(`${BASE_URL}/dashboard`, { limit });
}
