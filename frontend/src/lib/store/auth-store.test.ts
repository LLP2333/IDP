import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { useAuthStore } from "./auth-store";

describe("useAuthStore", () => {
  beforeEach(() => {
    window.localStorage.clear();
    useAuthStore.setState({ token: null, user: null, hydrated: true });
  });

  afterEach(() => {
    window.localStorage.clear();
  });

  it("setToken 后能持久化到 localStorage", async () => {
    useAuthStore.getState().setToken("abc");
    expect(useAuthStore.getState().token).toBe("abc");
    const raw = window.localStorage.getItem("idp-auth");
    expect(raw).toBeTruthy();
    expect(raw!).toContain("abc");
  });

  it("logout 同时清空 token 与 user", () => {
    useAuthStore.setState({
      token: "x",
      user: {
        id: 1,
        username: "admin",
        nickname: "管理员",
        avatar: null,
        email: null,
        phone: null,
        gender: 0,
        roles: ["admin"],
        permissions: [],
      },
    });
    useAuthStore.getState().logout();
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
  });
});
