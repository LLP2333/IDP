import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { Tabs } from "./tabs";

describe("Tabs", () => {
  it("仅渲染当前激活 Tab 的内容，点击其他 Tab 触发 onChange", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <Tabs
        value="a"
        onChange={onChange}
        items={[
          { key: "a", label: "Alpha", content: <div>content-a</div> },
          { key: "b", label: "Beta", content: <div>content-b</div> },
        ]}
      />,
    );
    expect(screen.getByText("content-a")).toBeInTheDocument();
    expect(screen.queryByText("content-b")).not.toBeInTheDocument();
    await user.click(screen.getByText("Beta"));
    expect(onChange).toHaveBeenCalledWith("b");
  });

  it("disabled 的 Tab 不响应点击", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <Tabs
        value="a"
        onChange={onChange}
        items={[
          { key: "a", label: "Alpha" },
          { key: "b", label: "Beta", disabled: true },
        ]}
      />,
    );
    await user.click(screen.getByText("Beta"));
    expect(onChange).not.toHaveBeenCalled();
  });
});
