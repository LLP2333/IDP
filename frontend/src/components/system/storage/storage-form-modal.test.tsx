import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("~/lib/api/storage", () => ({
  addStorage: vi.fn(),
  updateStorage: vi.fn(),
}));

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

import { StorageFormModal } from "./storage-form-modal";

function withQuery(node: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{node}</QueryClientProvider>;
}

beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("StorageFormModal 字段联动", () => {
  it("LOCAL 类型不显示 endpoint / accessKey / secretKey 字段", () => {
    const onClose = vi.fn();
    render(withQuery(<StorageFormModal open target={null} onClose={onClose} />));
    expect(screen.queryByText("Endpoint")).toBeNull();
    expect(screen.queryByText("Access Key")).toBeNull();
    expect(screen.queryByText("Secret Key")).toBeNull();
  });

  it("切换到 S3 类型后，新增 endpoint / accessKey / secretKey 字段", () => {
    const onClose = vi.fn();
    render(withQuery(<StorageFormModal open target={null} onClose={onClose} />));
    const typeSelect = screen.getByDisplayValue("本地存储");
    fireEvent.change(typeSelect, { target: { value: "2" } });
    expect(screen.getByText("Endpoint")).toBeTruthy();
    expect(screen.getByText("Access Key")).toBeTruthy();
    expect(screen.getByText("Secret Key")).toBeTruthy();
  });

  it("编辑态：编码禁用 + Secret Key hint 提示留空表示不修改", () => {
    const onClose = vi.fn();
    render(
      withQuery(
        <StorageFormModal
          open
          target={{
            id: 1,
            name: "本地",
            code: "local",
            type: 2,
            accessKey: "ak",
            secretKey: "******",
            endpoint: "http://x",
            bucketName: "idp",
            domain: null,
            recycleBinEnabled: false,
            recycleBinPath: null,
            description: null,
            isDefault: true,
            sort: 1,
            status: 1,
            createdAt: "x",
            updatedAt: null,
          }}
          onClose={onClose}
        />,
      ),
    );
    const codeInput = screen.getByDisplayValue("local");
    expect((codeInput as HTMLInputElement).disabled).toBe(true);
    expect(screen.getByText(/留空表示不修改原密钥/)).toBeTruthy();
  });
});
