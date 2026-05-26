import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Progress } from "./progress";

describe("Progress", () => {
  it("根据 value 设置 width，并 clamp 到 0-100", () => {
    const { getByTestId, rerender } = render(<Progress value={42} />);
    expect((getByTestId("progress-bar") as HTMLDivElement).style.width).toBe("42%");
    rerender(<Progress value={150} />);
    expect((getByTestId("progress-bar") as HTMLDivElement).style.width).toBe("100%");
    rerender(<Progress value={-1} />);
    expect((getByTestId("progress-bar") as HTMLDivElement).style.width).toBe("0%");
  });

  it("showLabel 显示百分比数字", () => {
    const { getByText } = render(<Progress value={37.5} showLabel />);
    expect(getByText("38%")).toBeTruthy();
  });
});
