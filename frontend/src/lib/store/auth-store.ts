"use client";

import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

import { setTokenProvider, setUnauthorizedHandler } from "~/lib/api/http";
import type { MenuResp, UserInfo } from "~/lib/api/types";

/** localStorage 持久化 key。 */
const STORAGE_KEY = "idp-auth";

/**
 * 登录态 Store 的 Shape。
 */
interface AuthState {
  /** 当前 JWT；未登录时为 `null`。 */
  token: string | null;
  /** 当前登录用户信息；首次进入页面后异步加载。 */
  user: UserInfo | null;
  /**
   * 当前用户的动态菜单树（{@code GET /auth/user/route} 的结果，type=1/2）。
   *
   * 仅供侧边栏渲染使用，不做持久化（每次进入后台时按 token 重新拉取，保证数据新鲜）。
   */
  menuTree: MenuResp[] | null;
  /** 持久化恢复完成后置 true，避免 SSR 与首屏闪烁。 */
  hydrated: boolean;
  /** 设置 / 清空当前 JWT。 */
  setToken: (token: string | null) => void;
  /** 设置 / 清空当前登录用户信息。 */
  setUser: (user: UserInfo | null) => void;
  /** 设置 / 清空当前用户的菜单树。 */
  setMenuTree: (tree: MenuResp[] | null) => void;
  /** 清空登录态（不会主动调用后端 `/auth/logout`）。 */
  logout: () => void;
}

/**
 * 登录态全局 Store。
 *
 * - 仅持久化 `token` 与 `user` 字段，`hydrated` 在 rehydrate 完成后由中间件填回；
 * - 在 SSR 环境下使用 noop Storage 占位，避免访问 `window` 抛错；
 * - 模块加载时把 token / 401 处理回调注入到 HTTP 层，避免上层手动绑定。
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      menuTree: null,
      hydrated: false,
      setToken: (token) => set({ token }),
      setUser: (user) => set({ user }),
      setMenuTree: (menuTree) => set({ menuTree }),
      logout: () => set({ token: null, user: null, menuTree: null }),
    }),
    {
      name: STORAGE_KEY,
      storage: createJSONStorage(() => {
        if (typeof window === "undefined") {
          const noop: Storage = {
            length: 0,
            clear: () => undefined,
            getItem: () => null,
            key: () => null,
            removeItem: () => undefined,
            setItem: () => undefined,
          };
          return noop;
        }
        return window.localStorage;
      }),
      partialize: (state) => ({ token: state.token, user: state.user }),
      onRehydrateStorage: () => (state) => {
        if (state) state.hydrated = true;
      },
    },
  ),
);

// 浏览器侧把 HTTP 层的 token 提供者与 401 回调与 store 绑定。
// 之所以放在模块顶部，是为了第一次发起请求前就完成注入。
if (typeof window !== "undefined") {
  setTokenProvider(() => useAuthStore.getState().token);
  setUnauthorizedHandler(() => {
    useAuthStore.getState().logout();
    if (window.location.pathname !== "/login") {
      window.location.assign("/login");
    }
  });
}
