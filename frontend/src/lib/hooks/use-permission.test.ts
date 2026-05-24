import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { useAuthStore } from "~/lib/store/auth-store";

import { usePermission } from "./use-permission";

function login(roles: string[], permissions: string[]) {
  useAuthStore.setState({
    token: "tok",
    user: {
      id: 1,
      username: "u",
      nickname: null,
      avatar: null,
      email: null,
      phone: null,
      roles,
      permissions,
    },
    hydrated: true,
  });
}

describe("usePermission", () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, user: null, hydrated: true });
  });

  it("admin 角色直通所有权限校验", () => {
    login(["admin"], []);
    const { result } = renderHook(() => usePermission());
    expect(result.current.hasPermission("anything")).toBe(true);
    expect(result.current.hasAllPermissions(["a", "b"])).toBe(true);
    expect(result.current.hasAnyPermission(["a"])).toBe(true);
  });

  it("普通用户按权限码精确匹配", () => {
    login(["user"], ["system:user:list"]);
    const { result } = renderHook(() => usePermission());
    expect(result.current.hasPermission("system:user:list")).toBe(true);
    expect(result.current.hasPermission("system:role:add")).toBe(false);
    expect(result.current.hasAllPermissions(["system:user:list", "system:role:add"])).toBe(false);
    expect(result.current.hasAnyPermission(["system:role:add", "system:user:list"])).toBe(true);
  });

  it("空数组校验视为允许（避免误拦截 “无需权限” 的元素）", () => {
    login(["user"], []);
    const { result } = renderHook(() => usePermission());
    expect(result.current.hasAnyPermission([])).toBe(true);
    expect(result.current.hasAllPermissions([])).toBe(true);
  });

  it("未登录时 isAdmin=false 且没有任何权限", () => {
    const { result } = renderHook(() => usePermission());
    expect(result.current.isAdmin).toBe(false);
    expect(result.current.hasPermission("any")).toBe(false);
  });
});
