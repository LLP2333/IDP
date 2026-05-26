import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("~/lib/api/file", () => ({
  getFileStatistics: vi.fn().mockResolvedValue({ size: 0, number: 0, data: [] }),
}));

import { FileAside } from "./file-aside";

afterEach(() => vi.restoreAllMocks());

function withQuery(node: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{node}</QueryClientProvider>;
}

describe("FileAside", () => {
  it("点击图片分类触发回调，并传入 type=2", () => {
    const onChange = vi.fn();
    const { getByText } = render(withQuery(<FileAside current={undefined} onChange={onChange} />));
    fireEvent.click(getByText("图片"));
    expect(onChange).toHaveBeenCalledWith(2);
  });

  it("当前选中分类 highlight", () => {
    const noop = vi.fn();
    const { getByText } = render(withQuery(<FileAside current={3} onChange={noop} />));
    const btn = getByText("文档").closest("button")!;
    expect(btn.className).toContain("bg-blue-50");
  });
});
