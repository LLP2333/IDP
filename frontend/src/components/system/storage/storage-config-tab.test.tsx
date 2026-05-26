import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

vi.mock("~/lib/api/storage", () => ({
  listStorage: vi.fn(),
  setDefaultStorage: vi.fn(),
  deleteStorage: vi.fn(),
  updateStorageStatus: vi.fn(),
  addStorage: vi.fn(),
  updateStorage: vi.fn(),
}));

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

import { useAuthStore } from "~/lib/store/auth-store";
import { listStorage } from "~/lib/api/storage";

import { StorageConfigTab } from "./storage-config-tab";

const listMock = listStorage as unknown as Mock;

function withQuery(node: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{node}</QueryClientProvider>;
}

beforeEach(() => {
  listMock.mockReset();
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
});

afterEach(() => {
  vi.restoreAllMocks();
  useAuthStore.setState({ token: null, user: null, hydrated: true });
});

describe("StorageConfigTab", () => {
  it("按类型分组渲染卡片，并展示 + 添加卡（admin 直通）", async () => {
    listMock.mockResolvedValue([
      {
        id: 1,
        name: "本地",
        code: "local",
        type: 1,
        accessKey: null,
        secretKey: null,
        endpoint: null,
        bucketName: "/tmp",
        domain: "http://x",
        recycleBinEnabled: true,
        recycleBinPath: "/.r",
        description: null,
        isDefault: true,
        sort: 1,
        status: 1,
        createdAt: "2026-01-01",
        updatedAt: null,
      },
      {
        id: 2,
        name: "MinIO 开发",
        code: "minio-dev",
        type: 2,
        accessKey: "ak",
        secretKey: "******",
        endpoint: "http://localhost:9000",
        bucketName: "idp",
        domain: null,
        recycleBinEnabled: false,
        recycleBinPath: null,
        description: null,
        isDefault: false,
        sort: 2,
        status: 1,
        createdAt: "2026-01-02",
        updatedAt: null,
      },
    ]);
    render(withQuery(<StorageConfigTab />));
    await waitFor(() => {
      expect(screen.getByText("本地")).toBeTruthy();
      expect(screen.getByText("MinIO 开发")).toBeTruthy();
    });
    expect(screen.getAllByText("新增存储").length).toBeGreaterThan(0);
  });

  it("搜索关键字过滤匹配名称", async () => {
    listMock.mockResolvedValue([
      {
        id: 1,
        name: "本地",
        code: "local",
        type: 1,
        accessKey: null,
        secretKey: null,
        endpoint: null,
        bucketName: "/tmp",
        domain: null,
        recycleBinEnabled: false,
        recycleBinPath: null,
        description: null,
        isDefault: true,
        sort: 1,
        status: 1,
        createdAt: "2026-01-01",
        updatedAt: null,
      },
      {
        id: 2,
        name: "MinIO 开发",
        code: "minio-dev",
        type: 2,
        accessKey: "ak",
        secretKey: "******",
        endpoint: "http://localhost:9000",
        bucketName: "idp",
        domain: null,
        recycleBinEnabled: false,
        recycleBinPath: null,
        description: null,
        isDefault: false,
        sort: 2,
        status: 1,
        createdAt: "2026-01-02",
        updatedAt: null,
      },
    ]);
    render(withQuery(<StorageConfigTab />));
    await waitFor(() => expect(screen.getByText("本地")).toBeTruthy());
    fireEvent.change(screen.getByPlaceholderText("按名称 / 编码搜索"), {
      target: { value: "minio" },
    });
    expect(screen.queryByText("本地")).toBeNull();
    expect(screen.getByText("MinIO 开发")).toBeTruthy();
  });
});
