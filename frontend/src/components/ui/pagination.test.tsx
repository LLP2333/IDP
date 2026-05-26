import { fireEvent, render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { Pagination } from "./pagination";

describe("Pagination", () => {
  it("第一页时上一页禁用", () => {
    const onPageChange = vi.fn();
    const { getByLabelText } = render(
      <Pagination page={1} size={10} total={50} onPageChange={onPageChange} />,
    );
    expect(getByLabelText("上一页")).toBeDisabled();
    expect(getByLabelText("下一页")).not.toBeDisabled();
  });

  it("点击下一页触发回调", () => {
    const onPageChange = vi.fn();
    const { getByLabelText } = render(
      <Pagination page={1} size={10} total={50} onPageChange={onPageChange} />,
    );
    fireEvent.click(getByLabelText("下一页"));
    expect(onPageChange).toHaveBeenCalledWith(2);
  });

  it("总条数为 0 时仍展示 1/1 且都禁用", () => {
    const onPageChange = vi.fn();
    const { getByLabelText, getByText } = render(
      <Pagination page={1} size={10} total={0} onPageChange={onPageChange} />,
    );
    expect(getByLabelText("上一页")).toBeDisabled();
    expect(getByLabelText("下一页")).toBeDisabled();
    expect(getByText("1 / 1")).toBeTruthy();
  });
});
