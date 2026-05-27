import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn(), refresh: vi.fn() }),
  usePathname: () => "/admin",
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
  Toaster: () => null,
}));

vi.mock("~/lib/api/auth", () => ({
  getUserInfo: vi.fn(),
  logout: vi.fn(),
}));

vi.mock("~/lib/api/menu", () => ({
  getUserRoute: vi.fn(),
}));

vi.mock("~/lib/api/option", () => ({
  getSiteConfigPublic: vi.fn(),
}));

// NoticePopup / NotificationBell 内部有 useQuery 调用，
// 这里 stub 掉避免触发额外的 HTTP mock 工作 —— 本测试只关心 layout 容器结构。
vi.mock("~/components/system/notice-popup", () => ({
  NoticePopup: () => null,
}));

vi.mock("~/components/system/notification-bell", () => ({
  NotificationBell: () => null,
}));

import { getUserInfo } from "~/lib/api/auth";
import { getUserRoute } from "~/lib/api/menu";
import { getSiteConfigPublic } from "~/lib/api/option";
import { useAuthStore } from "~/lib/store/auth-store";

import AdminLayout from "./layout";

function renderLayout() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <AdminLayout>
        <div data-testid="page-children">page</div>
      </AdminLayout>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  (getUserInfo as unknown as Mock).mockResolvedValue({
    id: 1,
    username: "admin",
    nickname: "管理员",
    avatar: null,
    email: null,
    phone: null,
    gender: 0,
    roles: ["admin"],
    permissions: [],
  });
  (getUserRoute as unknown as Mock).mockResolvedValue([]);
  (getSiteConfigPublic as unknown as Mock).mockResolvedValue({
    title: "IDP",
    description: "test",
    logo: null,
    favicon: null,
    copyright: "",
    beian: "",
  });
  useAuthStore.setState({
    token: "test-token",
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
    menuTree: null,
    hydrated: true,
  });
});

afterEach(() => {
  vi.restoreAllMocks();
  useAuthStore.setState({ token: null, user: null, hydrated: true });
});

/**
 * 这一组测试专门锁定 admin shell 的布局结构，防止以后有人误改
 * 导致左侧菜单跟着主内容一起滚动的回归。
 *
 * 关键不变量：
 * 1. 最外层使用 `h-screen` + `overflow-hidden` —— 否则浏览器页面级会出现滚动条；
 * 2. `<main>` 使用 `overflow-auto` —— 主内容独立滚动；
 * 3. `<aside>` 内的 `<nav>` 使用 `overflow-y-auto` —— 菜单超长时只在菜单内部滚。
 */
describe("AdminLayout 滚动容器结构", () => {
  it("最外层为固定视口高度，禁止页面级滚动", async () => {
    const { container } = renderLayout();
    // 等首屏 useEffect / useQuery 触发的状态更新稳定下来，避免 act 警告
    await screen.findByRole("link", { name: /概览/ });
    const root = container.firstElementChild as HTMLElement;
    expect(root).toBeTruthy();
    const rootClass = root.className;

    expect(rootClass).toContain("h-screen");
    expect(rootClass).toContain("overflow-hidden");
    // 一旦写成 min-h-screen 就会引入页面级滚动 → 侧边栏跟着滚
    expect(rootClass).not.toMatch(/(^|\s)min-h-screen(\s|$)/);
  });

  it("主内容区 <main> 独立 overflow-auto，且 children 落在其中", async () => {
    renderLayout();
    await screen.findByRole("link", { name: /概览/ });
    const page = screen.getByTestId("page-children");
    const main = page.closest("main");
    expect(main).not.toBeNull();
    expect(main!.className).toContain("overflow-auto");
    // flex-1 + min-h-0 是让 overflow-auto 真正生效的必要条件
    expect(main!.className).toContain("flex-1");
    expect(main!.className).toContain("min-h-0");
  });

  it("侧边栏内菜单 <nav> 自己 overflow-y-auto，不依赖页面滚动", async () => {
    renderLayout();
    // “概览” 链接位于 <nav> 内
    const overview = await screen.findByRole("link", { name: /概览/ });
    const nav = overview.closest("nav");
    expect(nav).not.toBeNull();
    expect(nav!.className).toContain("overflow-y-auto");
    expect(nav!.className).toContain("min-h-0");
  });

  it("侧边栏固定入口位于动态菜单前，动态菜单按 sort 升序", async () => {
    (getUserRoute as unknown as Mock).mockResolvedValue([
      {
        id: 1,
        title: "系统管理",
        parentId: 0,
        type: 1,
        path: "/system",
        name: "system",
        component: "Layout",
        redirect: null,
        icon: "settings",
        isExternal: false,
        isCache: false,
        isHidden: false,
        permission: null,
        sort: 200,
        status: 1,
        isSystem: true,
        description: null,
        createdAt: "2026-05-27T00:00:00",
        updatedAt: null,
        children: [],
      },
      {
        id: 2,
        title: "系统监控",
        parentId: 0,
        type: 1,
        path: "/monitor",
        name: "Monitor",
        component: "Layout",
        redirect: null,
        icon: "computer",
        isExternal: false,
        isCache: false,
        isHidden: false,
        permission: null,
        sort: 100,
        status: 1,
        isSystem: true,
        description: null,
        createdAt: "2026-05-27T00:00:00",
        updatedAt: null,
        children: [],
      },
    ]);

    renderLayout();
    const overview = await screen.findByRole("link", { name: /概览/ });
    await screen.findByText("系统管理");
    const navText = overview.closest("nav")!.textContent ?? "";

    expect(navText.indexOf("概览")).toBeLessThan(navText.indexOf("个人中心"));
    expect(navText.indexOf("个人中心")).toBeLessThan(navText.indexOf("系统监控"));
    expect(navText.indexOf("系统监控")).toBeLessThan(navText.indexOf("系统管理"));
  });
});
