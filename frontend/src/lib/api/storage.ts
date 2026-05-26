import { http } from "./http";
import type {
  StorageCreateReq,
  StorageQuery,
  StorageResp,
  StorageStatusUpdateReq,
  StorageUpdateReq,
} from "./types";

/** 存储管理后端前缀。 */
const BASE_URL = "/system/storage";

/**
 * 查询存储列表。
 *
 * @param query 过滤条件
 * @returns 存储数组（按 sort 升序）
 */
export function listStorage(query: StorageQuery = {}) {
  return http.get<StorageResp[]>(`${BASE_URL}/list`, {
    type: query.type,
    keyword: query.keyword,
  });
}

/**
 * 存储详情。
 *
 * @param id 存储 ID
 */
export function getStorage(id: number) {
  return http.get<StorageResp>(`${BASE_URL}/${id}`);
}

/**
 * 新增存储。
 *
 * @param req 请求体
 * @returns 新增的存储 ID
 */
export function addStorage(req: StorageCreateReq) {
  return http.post<number>(BASE_URL, req);
}

/**
 * 修改存储。
 *
 * @param id  存储 ID
 * @param req 请求体（SecretKey 留空表示不修改）
 */
export function updateStorage(id: number, req: StorageUpdateReq) {
  return http.put<void>(`${BASE_URL}/${id}`, req);
}

/**
 * 批量删除存储。
 *
 * @param ids 存储 ID 列表
 */
export function deleteStorage(ids: number[]) {
  return http.del<void>(BASE_URL, { ids });
}

/**
 * 切换存储状态。
 *
 * @param id  存储 ID
 * @param req 状态值
 */
export function updateStorageStatus(id: number, req: StorageStatusUpdateReq) {
  return http.put<void>(`${BASE_URL}/${id}/status`, req);
}

/**
 * 设为默认存储。
 *
 * @param id 存储 ID
 */
export function setDefaultStorage(id: number) {
  return http.put<void>(`${BASE_URL}/${id}/default`);
}
