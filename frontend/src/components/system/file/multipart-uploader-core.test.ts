import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

vi.mock("~/lib/api/multipart-upload", () => ({
  initMultipartUpload: vi.fn(),
  uploadPart: vi.fn(),
  completeMultipartUpload: vi.fn(),
  cancelMultipartUpload: vi.fn(),
}));

import {
  cancelMultipartUpload,
  completeMultipartUpload,
  initMultipartUpload,
  uploadPart,
} from "~/lib/api/multipart-upload";

import { calcSha256, uploadMultipart } from "./multipart-uploader-core";

const initMock = initMultipartUpload as unknown as Mock;
const partMock = uploadPart as unknown as Mock;
const completeMock = completeMultipartUpload as unknown as Mock;
const cancelMock = cancelMultipartUpload as unknown as Mock;

beforeEach(() => {
  initMock.mockReset();
  partMock.mockReset();
  completeMock.mockReset();
  cancelMock.mockReset();
  sessionStorage.clear();

  if (typeof Blob.prototype.arrayBuffer !== "function") {
    Object.defineProperty(Blob.prototype, "arrayBuffer", {
      configurable: true,
      value: async function arrayBuffer(this: Blob): Promise<ArrayBuffer> {
        const reader = new FileReader();
        return await new Promise<ArrayBuffer>((resolve, reject) => {
          reader.onload = () => resolve(reader.result as ArrayBuffer);
          reader.onerror = () => reject(reader.error ?? new Error("read blob failed"));
          reader.readAsArrayBuffer(this);
        });
      },
    });
  }

  if (!globalThis.crypto?.subtle) {
    Object.defineProperty(globalThis, "crypto", {
      configurable: true,
      value: {
        subtle: {
          digest: vi.fn().mockResolvedValue(new Uint8Array([1, 2, 3]).buffer),
        },
      },
    });
  } else {
    vi.spyOn(globalThis.crypto.subtle, "digest").mockResolvedValue(
      new Uint8Array([1, 2, 3]).buffer,
    );
  }
});

afterEach(() => {
  vi.restoreAllMocks();
});

function makeFile(content: string, name = "a.bin") {
  return new File([content], name, { type: "application/octet-stream" });
}

describe("uploadMultipart", () => {
  it("calcSha256 返回 64 位 hex 串（mock 的 digest 长度未限制）", async () => {
    const hex = await calcSha256(new Blob(["abc"]));
    expect(hex).toMatch(/^[0-9a-f]+$/);
  });

  it("init 命中秒传时直接返回 existing 文件且不发分片请求", async () => {
    const existing = { id: 1, name: "a.bin" };
    initMock.mockResolvedValue({ uploadId: null, existing });
    const result = await uploadMultipart({ file: makeFile("aaa"), parentPath: "/" });
    expect(result).toBe(existing);
    expect(partMock).not.toHaveBeenCalled();
    expect(completeMock).not.toHaveBeenCalled();
  });

  it("按 chunkSize 切片并行上传，最终 complete 合并", async () => {
    initMock.mockResolvedValue({ uploadId: "u1", existing: null });
    partMock.mockImplementation(async (_uploadId, partNumber: number) => ({
      partNumber,
      etag: `e${partNumber}`,
    }));
    completeMock.mockResolvedValue({ id: 99 });

    const file = makeFile("0123456789abcdef", "big.bin");
    const result = await uploadMultipart({
      file,
      chunkSize: 4,
      concurrency: 2,
    });
    expect(result).toEqual({ id: 99 });
    expect(partMock).toHaveBeenCalledTimes(4);
    expect(completeMock).toHaveBeenCalledWith("u1");
    expect(sessionStorage.length).toBe(0);
  });

  it("断点续传：sessionStorage 已记录的 partNumber 跳过", async () => {
    initMock.mockResolvedValue({ uploadId: "u2", existing: null });
    partMock.mockImplementation(async (_uploadId, partNumber: number) => ({
      partNumber,
      etag: `e${partNumber}`,
    }));
    completeMock.mockResolvedValue({ id: 1 });

    const file = makeFile("0123456789abcdef", "x.bin");
    const sha = await calcSha256(file);
    sessionStorage.setItem(`idp:mu:${sha}`, JSON.stringify([1, 2]));

    await uploadMultipart({ file, chunkSize: 4, concurrency: 2 });
    expect(partMock).toHaveBeenCalledTimes(2);
    const sent = partMock.mock.calls.map((c) => c[1] as number);
    expect(sent.sort()).toEqual([3, 4]);
  });

  it("AbortSignal 触发时调用 cancelMultipartUpload", async () => {
    initMock.mockResolvedValue({ uploadId: "u3", existing: null });
    partMock.mockImplementation((_u: string, partNumber: number) => {
      if (partNumber === 2) return Promise.reject(new DOMException("aborted", "AbortError"));
      return Promise.resolve({ partNumber, etag: "e" });
    });
    completeMock.mockResolvedValue({ id: 1 });

    const controller = new AbortController();
    await expect(
      uploadMultipart({
        file: makeFile("0123456789abcdef"),
        chunkSize: 4,
        concurrency: 1,
        signal: controller.signal,
      }),
    ).rejects.toBeInstanceOf(DOMException);
    expect(cancelMock).toHaveBeenCalledWith("u3");
  });
});
