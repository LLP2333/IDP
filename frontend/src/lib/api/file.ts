import { http } from "./http";
import type {
  FileCreateDirReq,
  FileDirCalcSizeResp,
  FilePageQuery,
  FileResp,
  FileStatisticsResp,
  FileUpdateReq,
  FileUploadResp,
  PageResp,
} from "./types";

/** 文件管理后端前缀。 */
const BASE_URL = "/system/file";

/**
 * 文件分页（不含回收站）。
 *
 * @param query 分页 + 过滤
 */
export function pageFile(query: FilePageQuery = {}) {
  return http.get<PageResp<FileResp>>(BASE_URL, {
    originalName: query.originalName,
    parentPath: query.parentPath,
    type: query.type,
    page: query.page ?? 1,
    size: query.size ?? 10,
  });
}

/**
 * 普通上传。
 *
 * 适合中小文件；超大文件请走 {@link import("./multipart-upload").initMultipartUpload}。
 *
 * @param file       文件
 * @param parentPath 上级目录（默认 /）
 * @param onProgress 进度回调
 */
export function uploadFile(file: File, parentPath = "/", onProgress?: (p: number) => void) {
  const fd = new FormData();
  fd.append("file", file);
  fd.append("parentPath", parentPath);
  return http.upload<FileUploadResp>(BASE_URL, fd, { onProgress });
}

/**
 * 重命名。
 */
export function renameFile(id: number, req: FileUpdateReq) {
  return http.put<void>(`${BASE_URL}/${id}`, req);
}

/**
 * 批量删除（按存储的 recycleBin 设置自动回收/物理删除）。
 */
export function deleteFile(ids: number[]) {
  return http.del<void>(BASE_URL, { ids });
}

/**
 * 资源统计。
 */
export function getFileStatistics() {
  return http.get<FileStatisticsResp>(`${BASE_URL}/statistics`);
}

/**
 * 按 SHA256 校验秒传。
 *
 * @param fileHash SHA256
 * @returns 命中已有文件返回 DTO，否则 null
 */
export function checkFile(fileHash: string) {
  return http.get<FileResp | null>(`${BASE_URL}/check`, { fileHash });
}

/**
 * 创建文件夹。
 */
export function createDir(req: FileCreateDirReq) {
  return http.post<number>(`${BASE_URL}/dir`, req);
}

/**
 * 计算文件夹大小。
 */
export function calcDirSize(id: number) {
  return http.get<FileDirCalcSizeResp>(`${BASE_URL}/${id}/size`);
}

// ============== recycle bin ==============

const RECYCLE_URL = "/system/file/recycle";

/**
 * 回收站分页。
 */
export function pageRecycle(query: FilePageQuery = {}) {
  return http.get<PageResp<FileResp>>(RECYCLE_URL, {
    originalName: query.originalName,
    type: query.type,
    page: query.page ?? 1,
    size: query.size ?? 10,
  });
}

/**
 * 还原。
 */
export function restoreFile(id: number) {
  return http.put<void>(`${RECYCLE_URL}/${id}`);
}

/**
 * 物理删除。
 */
export function permanentDelete(id: number) {
  return http.del<void>(`${RECYCLE_URL}/${id}`);
}

/**
 * 清空回收站。
 */
export function cleanRecycle() {
  return http.del<void>(RECYCLE_URL);
}
