"use client";

import { useQuery } from "@tanstack/react-query";
import { LogOut, ShieldCheck, Users } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { toast } from "sonner";

import { Button } from "~/components/ui/button";
import { getUserInfo, logout } from "~/lib/api/auth";
import { useAuthStore } from "~/lib/store/auth-store";
import { cn } from "~/lib/utils";

const NAV_ITEMS = [
  { href: "/admin", label: "概览", icon: null },
  { href: "/admin/system/user", label: "用户管理", icon: Users },
  { href: "/admin/system/role", label: "角色管理", icon: ShieldCheck },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const hydrated = useAuthStore((s) => s.hydrated);
  const clearAuth = useAuthStore((s) => s.logout);

  useEffect(() => {
    if (hydrated && !token) {
      router.replace("/login");
    }
  }, [hydrated, token, router]);

  const userQuery = useQuery({
    queryKey: ["auth", "user-info"],
    queryFn: getUserInfo,
    enabled: hydrated && !!token && !user,
  });

  useEffect(() => {
    if (userQuery.data) setUser(userQuery.data);
  }, [userQuery.data, setUser]);

  if (!hydrated || !token) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-zinc-500">
        正在加载…
      </div>
    );
  }

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 忽略登出失败
    }
    clearAuth();
    toast.success("已登出");
    router.replace("/login");
  };

  return (
    <div className="flex min-h-screen bg-zinc-50">
      <aside className="flex w-56 flex-col border-r border-zinc-200 bg-white">
        <div className="border-b border-zinc-200 px-5 py-4">
          <h1 className="text-base font-bold text-zinc-900">IDP 管理系统</h1>
          <p className="text-xs text-zinc-400">企业级后台</p>
        </div>
        <nav className="flex-1 px-2 py-3 text-sm">
          {NAV_ITEMS.map((item) => {
            const active =
              item.href === "/admin"
                ? pathname === "/admin"
                : pathname.startsWith(item.href);
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "mb-1 flex items-center gap-2 rounded-md px-3 py-2 transition-colors",
                  active
                    ? "bg-blue-50 text-blue-700"
                    : "text-zinc-600 hover:bg-zinc-100",
                )}
              >
                {Icon ? <Icon size={16} /> : <span className="inline-block w-4" />}
                {item.label}
              </Link>
            );
          })}
        </nav>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex h-14 items-center justify-end border-b border-zinc-200 bg-white px-6">
          <div className="flex items-center gap-3 text-sm">
            <div className="flex flex-col items-end leading-tight">
              <span className="font-medium text-zinc-800">
                {user?.nickname ?? user?.username ?? "未知用户"}
              </span>
              <span className="text-xs text-zinc-400">
                {user?.roles?.join(" / ") ?? "—"}
              </span>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleLogout}
              className="!text-zinc-500 hover:!text-zinc-800"
            >
              <LogOut size={14} />
              登出
            </Button>
          </div>
        </header>
        <main className="flex-1 overflow-auto p-6">{children}</main>
      </div>
    </div>
  );
}
