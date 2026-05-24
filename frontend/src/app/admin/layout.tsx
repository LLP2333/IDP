"use client";

import { useQuery } from "@tanstack/react-query";
import { KeyRound, LogOut, Settings, ShieldCheck, ShieldHalf, Users } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { toast } from "sonner";

import { SiteFooter } from "~/components/site-footer";
import { Button } from "~/components/ui/button";
import { getUserInfo, logout } from "~/lib/api/auth";
import { usePermission } from "~/lib/hooks/use-permission";
import { useSiteConfig } from "~/lib/hooks/use-site-config";
import { useAuthStore } from "~/lib/store/auth-store";
import { cn } from "~/lib/utils";

/**
 * 侧边栏导航项定义。
 *
 * 每个 item 可声明所需权限码集合：admin 直通，普通用户需拥有任一权限码才能看见。
 * 空数组表示无需权限（如 “概览”）。
 */
const NAV_ITEMS: Array<{
  href: string;
  label: string;
  icon: typeof Users | null;
  requires: string[];
}> = [
  { href: "/admin", label: "概览", icon: null, requires: [] },
  { href: "/admin/system/user", label: "用户管理", icon: Users, requires: ["system:user:list"] },
  { href: "/admin/system/role", label: "角色管理", icon: ShieldCheck, requires: ["system:role:list"] },
  {
    href: "/admin/system/permission",
    label: "权限管理",
    icon: ShieldHalf,
    requires: ["system:permission:list"],
  },
  {
    href: "/admin/system/config",
    label: "系统配置",
    icon: Settings,
    requires: ["system:siteConfig:get", "system:securityConfig:get", "system:loginConfig:get"],
  },
];

/**
 * `/admin/**` 路由下的整体 Shell：侧边栏 + 顶栏 + 主内容区。
 *
 * 同时负责：
 * - 路由级登录守卫：未登录则跳转 `/login`；
 * - 首次进入时调用 `/auth/user/info` 拉取并缓存当前用户信息；
 * - 顶栏提供登出按钮，登出后清空登录态并跳转登录页。
 */
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

  const { hasAnyPermission } = usePermission();
  const { data: site } = useSiteConfig();
  const siteTitle = site?.title?.trim() ? site.title : "IDP 管理系统";
  const siteSubtitle = site?.description?.trim() ? site.description : "企业级后台";
  const siteLogo = site?.logo ?? null;

  if (!hydrated || !token) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-zinc-500">
        正在加载…
      </div>
    );
  }

  /**
   * 登出处理：尝试调用后端登出，再清理本地登录态并跳回登录页。
   * 后端调用失败（如已离线）时也要保证前端能正常退出。
   */
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
          <div className="flex items-center gap-2">
            {siteLogo ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={siteLogo}
                alt={siteTitle}
                className="h-6 w-6 rounded object-contain"
              />
            ) : null}
            <h1 className="truncate text-base font-bold text-zinc-900" title={siteTitle}>
              {siteTitle}
            </h1>
          </div>
          <p className="mt-0.5 truncate text-xs text-zinc-400" title={siteSubtitle}>
            {siteSubtitle}
          </p>
        </div>
        <nav className="flex-1 px-2 py-3 text-sm">
          {NAV_ITEMS.filter((item) => hasAnyPermission(item.requires)).map((item) => {
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
          <Link
            href="/admin/profile/password"
            className={cn(
              "mb-1 flex items-center gap-2 rounded-md px-3 py-2 transition-colors",
              pathname.startsWith("/admin/profile/password")
                ? "bg-blue-50 text-blue-700"
                : "text-zinc-600 hover:bg-zinc-100",
            )}
          >
            <KeyRound size={16} />
            修改密码
          </Link>
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
        <SiteFooter className="border-t border-zinc-200 bg-white" />
      </div>
    </div>
  );
}
