import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { FileIcon } from "./file-icon";

describe("FileIcon", () => {
  it("文件夹优先展示 folder 图标，忽略 extension / thumbnailUrl", () => {
    const { container } = render(<FileIcon isDir extension="png" thumbnailUrl="x.png" />);
    expect(container.querySelector("img")).toBeNull();
  });

  it("非文件夹且有 thumbnailUrl 时渲染 img", () => {
    const { container } = render(<FileIcon thumbnailUrl="https://example.com/x.png" />);
    expect(container.querySelector("img")).not.toBeNull();
  });

  it("无 thumbnail 但有 extension 时按图标映射渲染 SVG", () => {
    const { container } = render(<FileIcon extension="pdf" />);
    expect(container.querySelector("svg")).not.toBeNull();
  });
});
