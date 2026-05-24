import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { useDict } from "./use-dict";

vi.mock("~/lib/api/dict", () => ({
  listDictItemByCode: vi.fn(),
}));

import { listDictItemByCode } from "~/lib/api/dict";

const mocked = listDictItemByCode as unknown as ReturnType<typeof vi.fn>;

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

beforeEach(() => {
  mocked.mockReset();
});

afterEach(() => vi.restoreAllMocks());

describe("useDict", () => {
  it("根据 code 拉取字典明细", async () => {
    mocked.mockResolvedValue([
      { id: 1, dictId: 1, label: "公告", value: "1", color: "primary", sort: 1, status: 1, isSystem: true },
      { id: 2, dictId: 1, label: "通知", value: "2", color: "success", sort: 2, status: 1, isSystem: true },
    ]);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { result } = renderHook(() => useDict("notice_type"), { wrapper: wrapper(client) });
    await waitFor(() => expect(result.current.items.length).toBeGreaterThan(0));
    expect(result.current.getLabel("1")).toBe("公告");
    expect(result.current.getColor(2)).toBe("success");
  });

  it("匹配不到时 getLabel 回落到原 value", async () => {
    mocked.mockResolvedValue([
      { id: 1, dictId: 1, label: "公告", value: "1", color: null, sort: 1, status: 1, isSystem: true },
    ]);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { result } = renderHook(() => useDict("notice_type"), { wrapper: wrapper(client) });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.getLabel("99")).toBe("99");
    expect(result.current.getColor("99")).toBeNull();
  });

  it("enabled=false 时不发请求", () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    renderHook(() => useDict("notice_type", false), { wrapper: wrapper(client) });
    expect(mocked).not.toHaveBeenCalled();
  });

  it("getLabel(null/undefined/'') 返回空字符串", async () => {
    mocked.mockResolvedValue([]);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { result } = renderHook(() => useDict("notice_type"), { wrapper: wrapper(client) });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.getLabel(null)).toBe("");
    expect(result.current.getLabel(undefined)).toBe("");
    expect(result.current.getLabel("")).toBe("");
  });
});
