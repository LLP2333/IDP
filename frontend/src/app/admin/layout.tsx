"use client";

import { useQuery } from "@tanstack/react-query";
import {
  Banknote,
  BarChart3,
  Bell,
  Boxes,
  CircleUser,
  Database,
  ExternalLink,
  FileText,
  Folder,
  Globe,
  KeyRound,
  LayoutDashboard,
  type LucideIcon,
  LogIn,
  LogOut,
  Menu as MenuIcon,
  Settings,
  Shield,
  ShieldCheck,
  Users,
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";

import { SiteFooter } from "~/components/site-footer";
import { Button } from "~/components/ui/button";
import { getUserInfo, logout } from "~/lib/api/auth";
import { getUserRoute } from "~/lib/api/menu";
import type { MenuResp } from "~/lib/api/types";
import { useSiteConfig } from "~/lib/hooks/use-site-config";
import { useAuthStore } from "~/lib/store/auth-store";
import { cn } from "~/lib/utils";

/**
 * 把后端菜单的 `icon` 字符串映射为 lucide-react 图标组件。
 *
 * 未匹配到 / 字段为空时返回 `null`，渲染时占位一个等宽的空白。
 */
const ICON_MAP: Record<string, LucideIcon> = {
  users: Users,
  user: Users,
  "shield-check": ShieldCheck,
  shield: Shield,
  menu: MenuIcon,
  settings: Settings,
  setting: Settings,
  "key-round": KeyRound,
  key: KeyRound,
  "log-in": LogIn,
  login: LogIn,
  dashboard: LayoutDashboard,
  layout: LayoutDashboard,
  folder: Folder,
  boxes: Boxes,
  database: Database,
  globe: Globe,
  bell: Bell,
  file: FileText,
  banknote: Banknote,
  chart: BarChart3,
  external: ExternalLink,
};

function resolveIcon(icon: string | null | undefined): LucideIcon | null {
  if (!icon) return null;
  return ICON_MAP[icon.trim().toLowerCase()] ?? Folder;
}

/**
 * 过滤掉隐藏 / 禁用节点，并按 sort 升序排序；递归处理子节点。
 */
function normalizeTree(tree: MenuResp[]): MenuResp[] {
  return tree
    .filter((node) => !node.isHidden && node.status === 1)
    .sort((a, b) => a.sort - b.sort)
    .map((node) => ({
      ...node,
      children: node.children ? normalizeTree(node.children) : [],
    }));
}

/**
 * `/admin/**` 路由下的整体 Shell：侧边栏 + 顶栏 + 主内容区。
 *
 * 同时负责：
 * - 路由级登录守卫：未登录则跳转 `/login`；
 * - 首次进入时调用 `/auth/user/info` 拉取并缓存当前用户信息；
 * - 调用 `/auth/user/route` 获取动态侧边栏菜单（admin 直通全部菜单，普通用户按角色聚合）；
 * - 顶栏提供登出按钮，登出后清空登录态并跳转登录页。
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const setMenuTree = useAuthStore((s) => s.setMenuTree);
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

  const routeQuery = useQuery({
    queryKey: ["auth", "user-route"],
    queryFn: getUserRoute,
    enabled: hydrated && !!token,
    staleTime: 5 * 60 * 1000,
  });

  useEffect(() => {
    if (routeQuery.data) setMenuTree(routeQuery.data);
  }, [routeQuery.data, setMenuTree]);

  const navTree = useMemo(
    () => normalizeTree(routeQuery.data ?? []),
    [routeQuery.data],
  );

  const [openIds, setOpenIds] = useState<Set<number>>(new Set());

  // 路径变化时，自动展开命中的目录链路（顶级目录至少打开一个）
  useEffect(() => {
    const next = new Set<number>();
    function walk(nodes: MenuResp[]): boolean {
      let matched = false;
      for (const n of nodes) {
        const childMatched = n.children ? walk(n.children) : false;
        const selfMatched = !!n.path && pathname.startsWith(n.path);
        if (childMatched || selfMatched) {
          next.add(n.id);
          matched = true;
        }
      }
      return matched;
    }
    walk(navTree);
    setOpenIds((prev) => {
      const merged = new Set(prev);
      next.forEach((id) => merged.add(id));
      return merged;
    });
  }, [pathname, navTree]);

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

  /**
   * 渲染一个菜单节点：
   * - 目录（type=1，存在子节点）：渲染为可展开的折叠分组；
   * - 菜单（type=2）：渲染为 `Link`；
   * - 外链（isExternal）：渲染为 `<a target="_blank">`。
   */
  function renderNavNode(node: MenuResp, depth: number): React.ReactNode {
    const Icon = resolveIcon(node.icon);
    const hasChildren = (node.children?.length ?? 0) > 0;
    const isDir = node.type === 1 && hasChildren;
    const isExternal = node.isExternal && node.path;

    if (isDir) {
      const isOpen = openIds.has(node.id);
      const toggle = () =>
        setOpenIds((prev) => {
          const next = new Set(prev);
          if (next.has(node.id)) next.delete(node.id);
          else next.add(node.id);
          return next;
        });
      return (
        <div key={node.id} className="mb-1">
          <button
            type="button"
            onClick={toggle}
            className={cn(
              "flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-zinc-600 transition-colors hover:bg-zinc-100",
            )}
            style={{ paddingLeft: 12 + depth * 12 }}
          >
            {Icon ? <Icon size={16} /> : <span className="inline-block w-4" />}
            <span className="flex-1">{node.title}</span>
            <span className="text-xs text-zinc-400">{isOpen ? "▾" : "▸"}</span>
          </button>
          {isOpen ? (
            <div className="ml-2 border-l border-zinc-100 pl-2">
              {node.children!.map((c) => renderNavNode(c, depth + 1))}
            </div>
          ) : null}
        </div>
      );
    }

    if (isExternal) {
      return (
        <a
          key={node.id}
          href={node.path!}
          target="_blank"
          rel="noreferrer"
          className={cn(
            "mb-1 flex items-center gap-2 rounded-md px-3 py-2 text-zinc-600 transition-colors hover:bg-zinc-100",
          )}
          style={{ paddingLeft: 12 + depth * 12 }}
        >
          {Icon ? <Icon size={16} /> : <ExternalLink size={16} />}
          {node.title}
        </a>
      );
    }

    if (!node.path) {
      // 没有 path 的非目录节点（数据异常）：直接降级为不可点击文本
      return (
        <div
          key={node.id}
          className="mb-1 flex items-center gap-2 rounded-md px-3 py-2 text-zinc-400"
          style={{ paddingLeft: 12 + depth * 12 }}
        >
          {Icon ? <Icon size={16} /> : <span className="inline-block w-4" />}
          {node.title}
        </div>
      );
    }

    const active = pathname === node.path || pathname.startsWith(`${node.path}/`);
    return (
      <Link
        key={node.id}
        href={node.path}
        className={cn(
          "mb-1 flex items-center gap-2 rounded-md px-3 py-2 transition-colors",
          active
            ? "bg-blue-50 text-blue-700"
            : "text-zinc-600 hover:bg-zinc-100",
        )}
        style={{ paddingLeft: 12 + depth * 12 }}
      >
        {Icon ? <Icon size={16} /> : <span className="inline-block w-4" />}
        {node.title}
      </Link>
    );
  }

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
        <nav className="flex-1 overflow-y-auto px-2 py-3 text-sm">
          <Link
            href="/admin"
            className={cn(
              "mb-1 flex items-center gap-2 rounded-md px-3 py-2 transition-colors",
              pathname === "/admin"
                ? "bg-blue-50 text-blue-700"
                : "text-zinc-600 hover:bg-zinc-100",
            )}
          >
            <LayoutDashboard size={16} />
            概览
          </Link>
          {routeQuery.isLoading ? (
            <div className="px-3 py-2 text-xs text-zinc-400">加载菜单中…</div>
          ) : (
            navTree.map((n) => renderNavNode(n, 0))
          )}
          <Link
            href="/admin/profile"
            className={cn(
              "mb-1 flex items-center gap-2 rounded-md px-3 py-2 transition-colors",
              pathname.startsWith("/admin/profile")
                ? "bg-blue-50 text-blue-700"
                : "text-zinc-600 hover:bg-zinc-100",
            )}
          >
            <CircleUser size={16} />
            个人中心
          </Link>
        </nav>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex h-14 items-center justify-end border-b border-zinc-200 bg-white px-6">
          <div className="flex items-center gap-3 text-sm">
            <Link
              href="/admin/profile"
              className="flex items-center gap-2 rounded-md px-2 py-1 transition-colors hover:bg-zinc-100"
              title="进入个人中心"
            >
              <div className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-50 text-xs font-semibold text-blue-700">
                {(user?.nickname ?? user?.username ?? "?").trim().slice(0, 1).toUpperCase()}
              </div>
              <div className="flex flex-col items-start leading-tight">
                <span className="font-medium text-zinc-800">
                  {user?.nickname ?? user?.username ?? "未知用户"}
                </span>
                <span className="text-xs text-zinc-400">
                  {user?.roles?.join(" / ") ?? "—"}
                </span>
              </div>
            </Link>
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
