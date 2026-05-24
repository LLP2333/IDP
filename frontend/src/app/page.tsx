"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useAuthStore } from "~/lib/store/auth-store";

/**
 * 应用根入口：根据登录态自动跳转。
 *
 * - 已登录 → `/admin`
 * - 未登录 → `/login`
 *
 * 在持久化恢复完成（`hydrated === true`）前展示 “正在跳转…” 占位，避免闪烁。
 */
export default function Home() {
  const router = useRouter();
  const token = useAuthStore((s) => s.token);
  const hydrated = useAuthStore((s) => s.hydrated);

  useEffect(() => {
    if (!hydrated) return;
    if (token) {
      router.replace("/admin");
    } else {
      router.replace("/login");
    }
  }, [hydrated, token, router]);

  return (
    <main className="flex min-h-screen items-center justify-center text-sm text-zinc-500">
      正在跳转…
    </main>
  );
}
