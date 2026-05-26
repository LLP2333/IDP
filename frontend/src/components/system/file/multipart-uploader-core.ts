/**
 * 分片上传核心算法（与 UI 解耦），方便在 vitest 中单测。
 *
 * 流程：
 * 1. 客户端用 Web Crypto SHA256 计算整文件哈希；
 * 2. POST init —— 命中秒传则提前返回 `existing`；否则后端返回 `uploadId`；
 * 3. 按 chunkSize 切片，最多并发 `concurrency` 个分片同时上传；
 * 4. 已成功的 partNumber 写入 sessionStorage，便于刷新后断点续传；
 * 5. 全部完成后 POST `/{uploadId}` 触发服务端合并 + 入库。
 */

import {
  cancelMultipartUpload,
  completeMultipartUpload,
  initMultipartUpload,
  uploadPart,
} from "~/lib/api/multipart-upload";
import type { FileResp } from "~/lib/api/types";

/** 默认 5MB 一片。 */
export const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;
/** 默认并发数。 */
export const DEFAULT_CONCURRENCY = 3;

/**
 * 计算 File / Blob 的 SHA256，返回 64 位 hex。
 *
 * 在不支持 SubtleCrypto 的旧浏览器中会抛错；调用方应捕获并降级提示。
 */
export async function calcSha256(blob: Blob): Promise<string> {
  const buf = await blob.arrayBuffer();
  const digest = await crypto.subtle.digest("SHA-256", buf);
  const bytes = new Uint8Array(digest);
  let hex = "";
  for (const b of bytes) hex += b.toString(16).padStart(2, "0");
  return hex;
}

/**
 * 单分片上传进度回调。
 */
export interface PartProgress {
  /** 分片编号，从 1 开始。 */
  partNumber: number;
  /** 0-100。 */
  percent: number;
}

/**
 * `uploadMultipart` 选项。
 */
export interface UploadMultipartOptions {
  /** 要上传的文件。 */
  file: File;
  /** 父目录路径，默认 /。 */
  parentPath?: string;
  /** 单片字节数，默认 5MB。 */
  chunkSize?: number;
  /** 并发分片数，默认 3。 */
  concurrency?: number;
  /** 整体进度（0-100）回调。 */
  onProgress?: (percent: number) => void;
  /** 每个分片单独进度回调。 */
  onPartProgress?: (p: PartProgress) => void;
  /** 取消信号（兼容浏览器 `AbortController`）。 */
  signal?: AbortSignal;
  /** 用于断点续传的会话 key 前缀；默认按 sha256 派生。 */
  sessionKeyPrefix?: string;
}

/** 上传完成后返回的文件信息。 */
export type UploadMultipartResult = FileResp;

/**
 * sessionStorage key 生成。
 */
function buildSessionKey(prefix: string | undefined, sha256: string) {
  return `${prefix ?? "idp:mu"}:${sha256}`;
}

/**
 * 从 sessionStorage 读取已上传的 partNumber 列表。
 */
function readDoneParts(sessionKey: string): Set<number> {
  try {
    const raw = sessionStorage.getItem(sessionKey);
    if (!raw) return new Set();
    const arr = JSON.parse(raw) as number[];
    return new Set(arr);
  } catch {
    return new Set();
  }
}

/** 写回已上传的 partNumber 列表。 */
function writeDoneParts(sessionKey: string, set: Set<number>) {
  try {
    sessionStorage.setItem(sessionKey, JSON.stringify(Array.from(set)));
  } catch {
    // sessionStorage 写入失败（隐身模式 / 磁盘满）允许继续上传。
  }
}

/**
 * 切片 + 并发上传 + 合并的完整流程。
 */
export async function uploadMultipart(opts: UploadMultipartOptions): Promise<UploadMultipartResult> {
  const {
    file,
    parentPath = "/",
    chunkSize = DEFAULT_CHUNK_SIZE,
    concurrency = DEFAULT_CONCURRENCY,
    onProgress,
    onPartProgress,
    signal,
    sessionKeyPrefix,
  } = opts;

  const sha256 = await calcSha256(file);
  if (signal?.aborted) throw new DOMException("已取消", "AbortError");

  const init = await initMultipartUpload({
    fileName: file.name,
    fileSize: file.size,
    chunkSize,
    sha256,
    parentPath,
  });
  if (init.existing) {
    onProgress?.(100);
    return init.existing;
  }
  if (!init.uploadId) {
    throw new Error("分片上传初始化失败");
  }
  const uploadId = init.uploadId;
  const sessionKey = buildSessionKey(sessionKeyPrefix, sha256);
  const done = readDoneParts(sessionKey);

  const totalParts = Math.max(1, Math.ceil(file.size / chunkSize));
  const allParts = Array.from({ length: totalParts }, (_, i) => i + 1);
  const pending = allParts.filter((p) => !done.has(p));
  const partProgress: Record<number, number> = {};

  const updateOverall = () => {
    if (!onProgress) return;
    const total = totalParts * 100;
    let sum = 0;
    for (const p of allParts) {
      sum += done.has(p) ? 100 : (partProgress[p] ?? 0);
    }
    onProgress(Math.min(100, Math.round((sum / total) * 100) / 100));
  };

  try {
    for (let i = 0; i < pending.length; i += concurrency) {
      if (signal?.aborted) throw new DOMException("已取消", "AbortError");
      const batch = pending.slice(i, i + concurrency);
      await Promise.all(
        batch.map(async (partNumber) => {
          const start = (partNumber - 1) * chunkSize;
          const end = Math.min(file.size, start + chunkSize);
          const blob = file.slice(start, end);
          await uploadPart(uploadId, partNumber, blob, (p) => {
            partProgress[partNumber] = p;
            onPartProgress?.({ partNumber, percent: p });
            updateOverall();
          });
          done.add(partNumber);
          writeDoneParts(sessionKey, done);
          updateOverall();
        }),
      );
    }
    const result = await completeMultipartUpload(uploadId);
    try {
      sessionStorage.removeItem(sessionKey);
    } catch {
      // ignore
    }
    onProgress?.(100);
    return result;
  } catch (err) {
    if ((err as DOMException)?.name === "AbortError") {
      try {
        await cancelMultipartUpload(uploadId);
      } catch {
        // 忽略 cancel 失败
      }
    }
    throw err;
  }
}
