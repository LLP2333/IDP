import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Button } from "./button";

describe("Button.normalizeChildren", () => {
  it("纯文字按钮 trim 后保留为单一 text 节点（无 span 包裹）", () => {
    const { container } = render(<Button>{"  保存  "}</Button>);
    const btn = container.querySelector("button")!;
    expect(btn.textContent).toBe("保存");
    expect(btn.querySelector("span")).toBeNull();
  });

  it("icon + 文字按钮：文字被 trim 并 wrap 为 span，不会出现前导空格", () => {
    const { container } = render(
      <Button>
        <i data-testid="icon" /> 编辑
      </Button>,
    );
    const btn = container.querySelector("button")!;
    const spans = btn.querySelectorAll("span");
    expect(spans.length).toBe(1);
    expect(spans[0]!.textContent).toBe("编辑");
    expect(btn.textContent).toBe("编辑");
  });
});
