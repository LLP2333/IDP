"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useAuthStore } from "~/lib/store/auth-store";

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
