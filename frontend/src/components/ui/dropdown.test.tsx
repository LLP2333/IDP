import { act, fireEvent, render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { Dropdown } from "./dropdown";

describe("Dropdown", () => {
  it("点击 trigger 切换菜单显隐 + 点击项触发回调并关闭", () => {
    const onSelect = vi.fn();
    const { getByText, queryByText } = render(
      <Dropdown
        trigger={<button type="button">menu</button>}
        items={[{ key: "a", label: "选项A", onSelect }]}
      />,
    );
    expect(queryByText("选项A")).toBeNull();
    fireEvent.click(getByText("menu"));
    expect(queryByText("选项A")).not.toBeNull();
    fireEvent.click(getByText("选项A"));
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(queryByText("选项A")).toBeNull();
  });

  it("divider 渲染为分隔符,而不是按钮", () => {
    const { getByText, getByRole } = render(
      <Dropdown
        trigger={<button type="button">menu</button>}
        items={[
          { key: "a", label: "A" },
          { key: "div", label: "", divider: true },
          { key: "b", label: "B" },
        ]}
      />,
    );
    fireEvent.click(getByText("menu"));
    expect(getByRole("separator")).toBeTruthy();
  });

  it("triggerOn=hover 时鼠标移入即展开，移出后延迟收起", () => {
    vi.useFakeTimers();
    try {
      const { container, queryByText } = render(
        <Dropdown
          triggerOn="hover"
          trigger={<button type="button">menu</button>}
          items={[{ key: "a", label: "选项A" }]}
        />,
      );
      const wrapper = container.firstElementChild as HTMLElement;
      expect(queryByText("选项A")).toBeNull();

      fireEvent.mouseEnter(wrapper);
      expect(queryByText("选项A")).not.toBeNull();

      fireEvent.mouseLeave(wrapper);
      expect(queryByText("选项A")).not.toBeNull();

      // setTimeout 触发的 setState 必须包在 act 里，让 React 把更新应用到 DOM
      act(() => {
        vi.advanceTimersByTime(200);
      });
      expect(queryByText("选项A")).toBeNull();
    } finally {
      vi.useRealTimers();
    }
  });
});
