import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

vi.mock("~/lib/api/file", () => ({
  pageFile: vi.fn(),
  uploadFile: vi.fn(),
  deleteFile: vi.fn(),
  getFileStatistics: vi.fn(),
  createDir: vi.fn(),
  pageRecycle: vi.fn(),
  restoreFile: vi.fn(),
  permanentDelete: vi.fn(),
  cleanRecycle: vi.fn(),
  renameFile: vi.fn(),
  calcDirSize: vi.fn(),
}));

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

import { useAuthStore } from "~/lib/store/auth-store";
import { pageFile, getFileStatistics } from "~/lib/api/file";

import FilePage from "./page";

const pageMock = pageFile as unknown as Mock;
const statsMock = getFileStatistics as unknown as Mock;

function setupAdmin() {
  useAuthStore.setState({
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
    token: "t",
    hydrated: true,
    menuTree: null,
  });
}

function withQuery(node: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{node}</QueryClientProvider>;
}

beforeEach(() => {
  pageMock.mockReset();
  statsMock.mockReset();
  setupAdmin();
  pageMock.mockResolvedValue({
    list: [],
    total: 0,
    page: 1,
    size: 20,
  });
  statsMock.mockResolvedValue({ size: 0, number: 0, data: [] });
});

afterEach(() => {
  vi.restoreAllMocks();
  useAuthStore.setState({ token: null, user: null, hydrated: true });
});

describe("FilePage", () => {
  it("空目录时渲染 Empty + 操作栏按钮", async () => {
    render(withQuery(<FilePage />));
    await waitFor(() => {
      expect(screen.getByText("当前目录为空")).toBeTruthy();
    });
    expect(screen.getByText("普通上传")).toBeTruthy();
    expect(screen.getByText("新建文件夹")).toBeTruthy();
    expect(screen.getByText("回收站")).toBeTruthy();
  });

  it("点击图片分类后会带 type=2 重新请求", async () => {
    render(withQuery(<FilePage />));
    await waitFor(() => expect(screen.getByText("当前目录为空")).toBeTruthy());
    fireEvent.click(screen.getByText("图片"));
    await waitFor(() => {
      const lastCall = pageMock.mock.calls[pageMock.mock.calls.length - 1];
      expect((lastCall![0] as { type: number }).type).toBe(2);
    });
  });
});
