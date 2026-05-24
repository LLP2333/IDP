"use client";

import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

import { setTokenProvider, setUnauthorizedHandler } from "~/lib/api/http";
import type { UserInfo } from "~/lib/api/types";

const STORAGE_KEY = "idp-auth";

interface AuthState {
  token: string | null;
  user: UserInfo | null;
  /** 持久化恢复完成后置 true，避免 SSR 与首屏闪烁 */
  hydrated: boolean;
  setToken: (token: string | null) => void;
  setUser: (user: UserInfo | null) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      hydrated: false,
      setToken: (token) => set({ token }),
      setUser: (user) => set({ user }),
      logout: () => set({ token: null, user: null }),
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

if (typeof window !== "undefined") {
  setTokenProvider(() => useAuthStore.getState().token);
  setUnauthorizedHandler(() => {
    useAuthStore.getState().logout();
    if (window.location.pathname !== "/login") {
      window.location.assign("/login");
    }
  });
}
