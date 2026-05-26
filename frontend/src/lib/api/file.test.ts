import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import {
  calcDirSize,
  checkFile,
  cleanRecycle,
  createDir,
  deleteFile,
  getFileStatistics,
  pageFile,
  pageRecycle,
  permanentDelete,
  renameFile,
  restoreFile,
  uploadFile,
} from "./file";

const ORIGINAL_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  vi.restoreAllMocks();
});

afterEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = ORIGINAL_BASE;
});

function mockOk(body: unknown) {
  global.fetch = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    headers: new Headers({ "content-type": "application/json" }),
    json: async () => ({ code: 0, msg: "ok", data: body, timestamp: 0 }),
  });
}

describe("file api", () => {
  it("pageFile 默认 page=1 size=10，并拼接 parentPath", async () => {
    mockOk({ list: [], total: 0, page: 1, size: 10 });
    await pageFile({ parentPath: "/foo", type: 2 });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/file");
    expect(url).toContain("page=1");
    expect(url).toContain("size=10");
    expect(url).toContain("parentPath=%2Ffoo");
    expect(url).toContain("type=2");
  });

  it("renameFile 使用 PUT", async () => {
    mockOk(null);
    await renameFile(1, { originalName: "abc.png" });
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("PUT");
  });

  it("deleteFile 使用 DELETE 并携带 ids", async () => {
    mockOk(null);
    await deleteFile([1, 2]);
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(init.body).toContain("[1,2]");
  });

  it("getFileStatistics 走 /statistics", async () => {
    mockOk({ size: 0, number: 0, data: [] });
    await getFileStatistics();
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/file/statistics");
  });

  it("checkFile 带 fileHash query", async () => {
    mockOk(null);
    await checkFile("abc");
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("fileHash=abc");
  });

  it("createDir 使用 POST /dir", async () => {
    mockOk(1);
    await createDir({ originalName: "n", parentPath: "/" });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/file/dir");
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("POST");
  });

  it("calcDirSize 走 /id/size", async () => {
    mockOk({ size: 0 });
    await calcDirSize(99);
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/file/99/size");
  });

  it("uploadFile 走 http.upload 并设置 FormData", async () => {
    const xhr = {
      open: vi.fn(),
      send: vi.fn(),
      setRequestHeader: vi.fn(),
      upload: {} as { onprogress?: (e: ProgressEvent) => void },
      onload: null as null | (() => void),
      onerror: null as null | (() => void),
      onabort: null as null | (() => void),
      status: 200,
      responseText: JSON.stringify({ code: 0, msg: "ok", data: { id: 1 }, timestamp: 0 }),
      getResponseHeader: () => "application/json",
    };
    vi.spyOn(globalThis, "XMLHttpRequest").mockImplementation(() => xhr as unknown as XMLHttpRequest);
    const onProgress = vi.fn();
    const p = uploadFile(new File(["abc"], "a.txt"), "/x", onProgress);
    setTimeout(() => xhr.onload?.(), 0);
    const data = await p;
    expect(data.id).toBe(1);
    expect(xhr.open).toHaveBeenCalledWith("POST", expect.stringContaining("/system/file"), true);
  });

  // recycle
  it("pageRecycle / restoreFile / permanentDelete / cleanRecycle", async () => {
    mockOk({ list: [], total: 0, page: 1, size: 10 });
    await pageRecycle({ type: 2 });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/file/recycle");
    expect(url).toContain("type=2");

    mockOk(null);
    await restoreFile(1);
    const restoreInit = (global.fetch as unknown as Mock).mock.calls.at(-1)![1] as RequestInit;
    expect(restoreInit.method).toBe("PUT");

    mockOk(null);
    await permanentDelete(2);
    const delInit = (global.fetch as unknown as Mock).mock.calls.at(-1)![1] as RequestInit;
    expect(delInit.method).toBe("DELETE");

    mockOk(null);
    await cleanRecycle();
    const cleanInit = (global.fetch as unknown as Mock).mock.calls.at(-1)![1] as RequestInit;
    expect(cleanInit.method).toBe("DELETE");
  });
});
