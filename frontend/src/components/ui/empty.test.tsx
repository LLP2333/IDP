import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Empty } from "./empty";

describe("Empty", () => {
  it("默认渲染 '暂无数据'", () => {
    const { getByText } = render(<Empty />);
    expect(getByText("暂无数据")).toBeTruthy();
  });

  it("自定义 title / description / action 都会渲染", () => {
    const { getByText } = render(
      <Empty title="无文件" description="请上传你的第一个文件" action={<button>上传</button>} />,
    );
    expect(getByText("无文件")).toBeTruthy();
    expect(getByText("请上传你的第一个文件")).toBeTruthy();
    expect(getByText("上传")).toBeTruthy();
  });
});
