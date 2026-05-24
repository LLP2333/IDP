import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import {
  assignRolePermission,
  createPermission,
  deletePermission,
  getPermissionTree,
  getRolePermission,
  listPermission,
  updatePermission,
} from "./permission";

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

describe("permission api", () => {
  it("listPermission 透传 keyword / type", async () => {
    mockOk([]);
    await listPermission({ keyword: "user", type: 2 });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("keyword=user");
    expect(url).toContain("type=2");
  });

  it("getPermissionTree -> GET /system/permission/tree", async () => {
    mockOk([]);
    await getPermissionTree();
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/permission/tree");
  });

  it("createPermission / updatePermission / deletePermission 使用对应动词", async () => {
    mockOk(1);
    await createPermission({ code: "x:y", name: "x", type: 2 });
    expect(((global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit).method).toBe("POST");

    mockOk(null);
    await updatePermission(1, { code: "x:y", name: "x", type: 2 });
    expect(((global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit).method).toBe("PUT");

    mockOk(null);
    await deletePermission([1, 2]);
    const del = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(del.method).toBe("DELETE");
    expect(del.body).toContain("[1,2]");
  });

  it("getRolePermission / assignRolePermission 走 /system/role/{id}/permission", async () => {
    mockOk([1, 2, 3]);
    await getRolePermission(7);
    expect((global.fetch as unknown as Mock).mock.calls[0]![0]).toContain("/system/role/7/permission");

    mockOk(null);
    await assignRolePermission(7, [4, 5]);
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(init.body).toContain('"permissionIds":[4,5]');
  });
});
