import { http } from "./http";
import type {
  FileResp,
  MultipartUploadInitReq,
  MultipartUploadInitResp,
  MultipartUploadPartResp,
} from "./types";

/** 分片上传后端前缀。 */
const BASE_URL = "/system/multipart-upload";

/**
 * 初始化分片上传。
 *
 * 命中 SHA256 时直接返回已有文件（秒传），此时 {@code uploadId} 为 null。
 */
export function initMultipartUpload(req: MultipartUploadInitReq) {
  return http.post<MultipartUploadInitResp>(BASE_URL, req);
}

/**
 * 上传单个分片。
 *
 * @param uploadId   uploadId
 * @param partNumber 分片编号（从 1 开始）
 * @param part       分片 Blob / File
 * @param onProgress 进度回调
 */
export function uploadPart(uploadId: string, partNumber: number, part: Blob, onProgress?: (p: number) => void) {
  const fd = new FormData();
  fd.append("partNumber", String(partNumber));
  fd.append("file", part);
  return http.upload<MultipartUploadPartResp>(`${BASE_URL}/${uploadId}/part`, fd, {
    method: "PUT",
    onProgress,
  });
}

/**
 * 完成分片上传：合并 + 入库。
 */
export function completeMultipartUpload(uploadId: string) {
  return http.post<FileResp>(`${BASE_URL}/${uploadId}`);
}

/**
 * 取消分片上传。
 */
export function cancelMultipartUpload(uploadId: string) {
  return http.del<void>(`${BASE_URL}/${uploadId}`);
}
