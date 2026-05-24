import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import {
  exportLoginLog,
  exportOperationLog,
  getLog,
  kickoutOnlineUser,
  listLog,
  listOnlineUser,
} from "./monitor";

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

function mockDownload() {
  global.fetch = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    headers: new Headers({
      "content-disposition": "attachment; filename*=UTF-8''server-log.csv",
    }),
    blob: async () => new Blob(["id,createTime"], { type: "text/csv" }),
  });
  const revokeObjectURLMock = vi.fn();
  window.URL.createObjectURL = vi.fn(() => "blob:test");
  window.URL.revokeObjectURL = revokeObjectURLMock;
  HTMLAnchorElement.prototype.click = vi.fn();
  return { revokeObjectURLMock };
}

describe("monitor api", () => {
  it("listOnlineUser 透传在线用户查询条件", async () => {
    mockOk({ list: [], total: 0, page: 1, size: 10 });
    await listOnlineUser({
      page: 1,
      size: 10,
      nickname: "admin",
      loginTime: ["2026-05-24 00:00:00", "2026-05-24 23:59:00"],
    });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/monitor/online");
    expect(url).toContain("nickname=admin");
    expect(url).toContain("loginTime=2026-05-24+00%3A00%3A00");
    expect(url).toContain("loginTime=2026-05-24+23%3A59%3A00");
  });

  it("kickoutOnlineUser 对 token 编码后删除", async () => {
    mockOk(null);
    await kickoutOnlineUser("a/b+c");
    const call = (global.fetch as unknown as Mock).mock.calls[0]!;
    expect(call[0]).toContain("/monitor/online/a%2Fb%2Bc");
    expect((call[1] as RequestInit).method).toBe("DELETE");
  });

  it("listLog 与 getLog 命中系统日志接口", async () => {
    mockOk({ list: [], total: 0, page: 1, size: 10 });
    await listLog({ page: 1, size: 10, module: "登录", status: 1 });
    let url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/log");
    expect(url).toContain("module=%E7%99%BB%E5%BD%95");
    expect(url).toContain("status=1");

    mockOk({ id: "7" });
    await getLog("7");
    url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/log/7");
  });

  it("导出日志走下载接口并使用服务端文件名", async () => {
    const { revokeObjectURLMock } = mockDownload();
    await exportLoginLog({ module: "登录" });
    let url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/log/export/login");
    expect(url).toContain("module=%E7%99%BB%E5%BD%95");
    expect(document.querySelector("a")).toBeNull();
    expect(revokeObjectURLMock).toHaveBeenCalledWith("blob:test");

    mockDownload();
    await exportOperationLog({ description: "修改" });
    url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/log/export/operation");
    expect(url).toContain("description=%E4%BF%AE%E6%94%B9");
  });
});
