import { act, fireEvent, render } from "@testing-library/react";
import { useEffect } from "react";
import { describe, expect, it, vi } from "vitest";

import { ContextMenu, useContextMenu } from "./context-menu";

function Harness({ openX = 100, openY = 200, onSelect }: { openX?: number; openY?: number; onSelect: () => void }) {
  const cm = useContextMenu();
  useEffect(() => {
    const noop = vi.fn();
    cm.open(
      {
        preventDefault: vi.fn(),
        clientX: openX,
        clientY: openY,
      } as unknown as React.MouseEvent,
      [
        { key: "rename", label: "重命名", onSelect },
        { key: "div", label: "", divider: true },
        { key: "del", label: "删除", danger: true, onSelect: noop },
      ],
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return <ContextMenu state={cm.state} onClose={cm.close} />;
}

describe("ContextMenu", () => {
  it("渲染到 body，按钮可点击触发 onSelect", () => {
    const onSelect = vi.fn();
    const { getByText } = render(<Harness onSelect={onSelect} />);
    expect(getByText("重命名")).toBeTruthy();
    fireEvent.click(getByText("重命名"));
    expect(onSelect).toHaveBeenCalledTimes(1);
  });

  it("按 Escape 关闭菜单", () => {
    const onSelect = vi.fn();
    const { queryByText } = render(<Harness onSelect={onSelect} />);
    expect(queryByText("重命名")).not.toBeNull();
    act(() => {
      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
    });
    expect(queryByText("重命名")).toBeNull();
  });
});
