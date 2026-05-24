import { http } from "./http";
import type { DictItemReq, DictItemResp, DictReq, DictResp } from "./types";

/** 字典管理在后端的统一前缀。 */
const BASE_URL = "/system/dict";

/**
 * 查询全部字典（不分页）。
 *
 * @returns 字典列表（按 ID 升序）
 */
export function listDict() {
  return http.get<DictResp[]>(`${BASE_URL}/list`);
}

/**
 * 查询单个字典。
 */
export function getDict(id: number | string) {
  return http.get<DictResp>(`${BASE_URL}/${id}`);
}

/**
 * 新增字典。
 *
 * @returns 新建字典 ID
 */
export function addDict(data: DictReq) {
  return http.post<number>(BASE_URL, data);
}

/**
 * 修改字典。
 */
export function updateDict(id: number | string, data: DictReq) {
  return http.put<void>(`${BASE_URL}/${id}`, data);
}

/**
 * 批量删除字典；系统内置字典不可删。
 */
export function deleteDict(ids: Array<number | string>) {
  return http.del<void>(BASE_URL, { ids });
}

/**
 * 按 code 查询字典明细（仅启用项）。
 *
 * <p>各业务下拉的统一数据源：登录用户即可访问，无需 {@code system:dict:list} 权限。</p>
 */
export function listDictItemByCode(code: string) {
  return http.get<DictItemResp[]>(`${BASE_URL}/${code}/item`);
}

/**
 * 列出某字典下的全部明细（含禁用）。
 */
export function listDictItem(dictId: number | string) {
  return http.get<DictItemResp[]>(`${BASE_URL}/${dictId}/item/all`);
}

/**
 * 新增字典明细。
 */
export function addDictItem(dictId: number | string, data: DictItemReq) {
  return http.post<number>(`${BASE_URL}/${dictId}/item`, data);
}

/**
 * 修改字典明细。
 */
export function updateDictItem(itemId: number | string, data: DictItemReq) {
  return http.put<void>(`${BASE_URL}/item/${itemId}`, data);
}

/**
 * 批量删除字典明细。
 */
export function deleteDictItem(ids: Array<number | string>) {
  return http.del<void>(`${BASE_URL}/item`, { ids });
}
