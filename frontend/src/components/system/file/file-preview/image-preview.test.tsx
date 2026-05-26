import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ImagePreview, type ImagePreviewItem } from "./image-preview";

const sample: ImagePreviewItem[] = [
  { id: 1, url: "/file/local/a.png", name: "a.png" },
  { id: 2, url: "/file/local/b.png", name: "b.png" },
  { id: 3, url: "/file/local/c.png", name: "c.png" },
];

describe("ImagePreview", () => {
  it("当 images 为 null 时不渲染", () => {
    const { container } = render(<ImagePreview images={null} onClose={vi.fn()} />);
    expect(container.querySelector("img")).toBeNull();
  });

  it("展示初始图片与计数", () => {
    render(<ImagePreview images={sample} initialIndex={1} onClose={vi.fn()} />);
    expect(screen.getByText("b.png")).toBeTruthy();
    expect(screen.getByText("2 / 3")).toBeTruthy();
  });

  it("点击右箭头切到下一张", () => {
    render(<ImagePreview images={sample} initialIndex={0} onClose={vi.fn()} />);
    fireEvent.click(screen.getByLabelText("下一张"));
    expect(screen.getByText("b.png")).toBeTruthy();
    expect(screen.getByText("2 / 3")).toBeTruthy();
  });

  it("点击左箭头会环回到最后一张", () => {
    render(<ImagePreview images={sample} initialIndex={0} onClose={vi.fn()} />);
    fireEvent.click(screen.getByLabelText("上一张"));
    expect(screen.getByText("c.png")).toBeTruthy();
    expect(screen.getByText("3 / 3")).toBeTruthy();
  });

  it("键盘 ArrowRight 切到下一张,Escape 触发关闭", () => {
    const onClose = vi.fn();
    render(<ImagePreview images={sample} initialIndex={0} onClose={onClose} />);
    fireEvent.keyDown(document, { key: "ArrowRight" });
    expect(screen.getByText("2 / 3")).toBeTruthy();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalled();
  });

  it("点击放大按钮缩放比例从 100% 到 120%", () => {
    render(<ImagePreview images={sample} onClose={vi.fn()} />);
    expect(screen.getByText("100%")).toBeTruthy();
    fireEvent.click(screen.getByLabelText("放大"));
    expect(screen.getByText("120%")).toBeTruthy();
  });

  it("单图时不出现左右切换按钮与计数", () => {
    render(<ImagePreview images={[sample[0]!]} onClose={vi.fn()} />);
    expect(screen.queryByLabelText("下一张")).toBeNull();
    expect(screen.queryByLabelText("上一张")).toBeNull();
    expect(screen.queryByText(/\d+ \/ \d+/)).toBeNull();
  });
});
