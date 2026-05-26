import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import {
  addStorage,
  deleteStorage,
  getStorage,
  listStorage,
  setDefaultStorage,
  updateStorage,
  updateStorageStatus,
} from "./storage";

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

describe("storage api", () => {
  it("listStorage 拼接 type / keyword", async () => {
    mockOk([]);
    await listStorage({ type: 2, keyword: "minio" });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/storage/list");
    expect(url).toContain("type=2");
    expect(url).toContain("keyword=minio");
  });

  it("getStorage 走 GET 详情接口", async () => {
    mockOk({});
    await getStorage(7);
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(init.method).toBe("GET");
    expect(url).toContain("/system/storage/7");
  });

  it("addStorage 使用 POST", async () => {
    mockOk(1);
    const id = await addStorage({
      name: "本地",
      code: "local",
      type: 1,
      bucketName: "/tmp",
      domain: "http://localhost:8080/file/local/",
      recycleBinEnabled: false,
      sort: 1,
      status: 1,
    });
    expect(id).toBe(1);
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("POST");
  });

  it("updateStorage 使用 PUT", async () => {
    mockOk(null);
    await updateStorage(2, {
      name: "新名",
      bucketName: "/tmp",
      sort: 1,
      status: 1,
    });
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("PUT");
  });

  it("deleteStorage 使用 DELETE + ids body", async () => {
    mockOk(null);
    await deleteStorage([1, 2, 3]);
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(init.body).toContain("[1,2,3]");
  });

  it("updateStorageStatus 使用 PUT /id/status", async () => {
    mockOk(null);
    await updateStorageStatus(2, { status: 2 });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/storage/2/status");
  });

  it("setDefaultStorage 使用 PUT /id/default", async () => {
    mockOk(null);
    await setDefaultStorage(3);
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/storage/3/default");
  });
});
