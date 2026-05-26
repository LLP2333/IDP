import { fireEvent, render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { Breadcrumb } from "./breadcrumb";

describe("Breadcrumb", () => {
  it("最后一项渲染但不可点击", () => {
    const onClick = vi.fn();
    const { getAllByRole } = render(
      <Breadcrumb items={[{ label: "根", onClick }, { label: "末尾", onClick }]} />,
    );
    const buttons = getAllByRole("button");
    expect(buttons[buttons.length - 1]).toBeDisabled();
    expect(buttons[0]).not.toBeDisabled();
    fireEvent.click(buttons[0]!);
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
