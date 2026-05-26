import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import {
  cancelMultipartUpload,
  completeMultipartUpload,
  initMultipartUpload,
  uploadPart,
} from "./multipart-upload";

const ORIGINAL = process.env.NEXT_PUBLIC_API_BASE_URL;

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  vi.restoreAllMocks();
});

afterEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = ORIGINAL;
});

function mockOk(body: unknown) {
  global.fetch = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    headers: new Headers({ "content-type": "application/json" }),
    json: async () => ({ code: 0, msg: "ok", data: body, timestamp: 0 }),
  });
}

describe("multipart upload api", () => {
  it("init POST 并返回 uploadId", async () => {
    mockOk({ uploadId: "u1", existing: null });
    const resp = await initMultipartUpload({
      fileName: "a.zip",
      fileSize: 5,
      chunkSize: 5,
      sha256: "h",
      parentPath: "/",
    });
    expect(resp.uploadId).toBe("u1");
  });

  it("init 命中秒传时返回 existing 字段", async () => {
    mockOk({ uploadId: null, existing: { id: 9, name: "a.zip" } });
    const resp = await initMultipartUpload({ fileName: "a.zip", fileSize: 1, chunkSize: 1, sha256: "h" });
    expect(resp.existing?.id).toBe(9);
  });

  it("uploadPart 使用 PUT 并携带 partNumber", async () => {
    const xhr = {
      open: vi.fn(),
      send: vi.fn(),
      setRequestHeader: vi.fn(),
      upload: {} as { onprogress?: (e: ProgressEvent) => void },
      onload: null as null | (() => void),
      onerror: null as null | (() => void),
      status: 200,
      responseText: JSON.stringify({ code: 0, msg: "ok", data: { partNumber: 1, etag: "e1" }, timestamp: 0 }),
      getResponseHeader: () => "application/json",
    };
    vi.spyOn(globalThis, "XMLHttpRequest").mockImplementation(() => xhr as unknown as XMLHttpRequest);
    const p = uploadPart("u1", 1, new Blob(["abc"]));
    setTimeout(() => xhr.onload?.(), 0);
    const r = await p;
    expect(r.etag).toBe("e1");
    expect(xhr.open).toHaveBeenCalledWith("PUT", expect.stringContaining("/system/multipart-upload/u1/part"), true);
  });

  it("complete POST，cancel DELETE", async () => {
    mockOk({ id: 1 });
    await completeMultipartUpload("u1");
    const completeInit = (global.fetch as unknown as Mock).mock.calls.at(-1)![1] as RequestInit;
    expect(completeInit.method).toBe("POST");

    mockOk(null);
    await cancelMultipartUpload("u1");
    const cancelInit = (global.fetch as unknown as Mock).mock.calls.at(-1)![1] as RequestInit;
    expect(cancelInit.method).toBe("DELETE");
  });
});
