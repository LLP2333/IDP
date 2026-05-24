import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

import { NoticeDetailDrawer } from "./notice-detail-drawer";
import type { NoticeDetailResp } from "~/lib/api/types";

vi.mock("~/lib/api/dict", () => ({
  listDictItemByCode: vi.fn().mockResolvedValue([]),
}));

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

const sample: NoticeDetailResp = {
  id: 1,
  title: "我的公告",
  content: "正文\n换行",
  type: "1",
  noticeScope: 1,
  noticeMethods: [1, 2],
  isTiming: false,
  publishTime: "2026-05-24T16:00:00",
  isTop: true,
  status: 3,
  isRead: false,
  createUserString: "admin",
  createdBy: 1,
  createdAt: "2026-05-24T10:00:00",
  updatedAt: null,
  noticeUsers: null,
};

describe("NoticeDetailDrawer", () => {
  it("closed 状态下不渲染", () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<NoticeDetailDrawer open={false} onClose={() => undefined} data={sample} />, {
      wrapper: wrapper(client),
    });
    expect(screen.queryByText("公告详情")).not.toBeInTheDocument();
  });

  it("打开时显示标题与正文", () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<NoticeDetailDrawer open={true} onClose={() => undefined} data={sample} />, {
      wrapper: wrapper(client),
    });
    expect(screen.getByText("公告详情")).toBeInTheDocument();
    expect(screen.getByText("我的公告")).toBeInTheDocument();
    expect(screen.getAllByText(/正文/).length).toBeGreaterThan(0);
  });
});
