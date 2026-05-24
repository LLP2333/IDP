import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { DictBadge } from "./dict-badge";
import type { DictItemResp } from "~/lib/api/types";

const items: DictItemResp[] = [
  { id: 1, dictId: 1, label: "公告", value: "1", color: "primary", sort: 1, status: 1, isSystem: true },
  { id: 2, dictId: 1, label: "通知", value: "2", color: "success", sort: 2, status: 1, isSystem: true },
];

describe("DictBadge", () => {
  it("命中字典明细时展示 label", () => {
    render(<DictBadge items={items} value="1" />);
    expect(screen.getByText("公告")).toBeInTheDocument();
  });

  it("数字 value 自动转字符串后匹配", () => {
    render(<DictBadge items={items} value={2} />);
    expect(screen.getByText("通知")).toBeInTheDocument();
  });

  it("匹配不到时使用 fallback", () => {
    render(<DictBadge items={items} value="9" fallback="自定义" />);
    expect(screen.getByText("自定义")).toBeInTheDocument();
  });

  it("空 value 展示破折号", () => {
    render(<DictBadge items={items} value={null} />);
    expect(screen.getByText("—")).toBeInTheDocument();
  });
});
