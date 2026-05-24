import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import {
  assignRoleMenu,
  createMenu,
  deleteMenu,
  getMenu,
  getMenuTree,
  getRoleMenu,
  getUserRoute,
  listMenu,
  updateMenu,
} from "./menu";
import type { MenuReq } from "./types";

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

describe("menu api", () => {
  it("listMenu 透传查询条件", async () => {
    mockOk([]);
    await listMenu({ title: "用户", type: 2 });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/menu");
    expect(url).toContain("title=%E7%94%A8%E6%88%B7");
    expect(url).toContain("type=2");
  });

  it("getMenuTree 命中 /system/menu/tree", async () => {
    mockOk([]);
    await getMenuTree();
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/menu/tree");
  });

  it("getMenu 按 ID 走 GET", async () => {
    mockOk({});
    await getMenu(7);
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/menu/7");
  });

  it("createMenu / updateMenu / deleteMenu 走对应动词", async () => {
    mockOk(123);
    const req: MenuReq = {
      title: "新建",
      parentId: 0,
      type: 2,
      path: "/foo",
    };
    await createMenu(req);
    expect(((global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit).method).toBe(
      "POST",
    );

    mockOk(null);
    await updateMenu(2, req);
    expect(((global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit).method).toBe(
      "PUT",
    );

    mockOk(null);
    await deleteMenu([1, 2]);
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(init.body).toContain('"ids":[1,2]');
  });

  it("getRoleMenu / assignRoleMenu 命中 /system/role/{id}/menu", async () => {
    mockOk([10, 20]);
    await getRoleMenu(5);
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/role/5/menu");

    mockOk(null);
    await assignRoleMenu(5, [10, 20]);
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(init.body).toContain('"menuIds":[10,20]');
  });

  it("getUserRoute 走 /auth/user/route", async () => {
    mockOk([]);
    await getUserRoute();
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/auth/user/route");
  });
});
