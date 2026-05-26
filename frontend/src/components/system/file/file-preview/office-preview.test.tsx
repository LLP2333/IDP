import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { getOfficePreviewKind, OfficePreview } from "./office-preview";

describe("getOfficePreviewKind", () => {
  it("识别常见扩展名", () => {
    expect(getOfficePreviewKind("pdf")).toBe("pdf");
    expect(getOfficePreviewKind("PDF")).toBe("pdf");
    expect(getOfficePreviewKind("docx")).toBe("docx");
    expect(getOfficePreviewKind("doc")).toBe("docx");
    expect(getOfficePreviewKind("xlsx")).toBe("xlsx");
    expect(getOfficePreviewKind("xls")).toBe("xlsx");
    expect(getOfficePreviewKind("pptx")).toBe("unsupported");
    expect(getOfficePreviewKind("ppt")).toBe("unsupported");
  });

  it("非 Office 扩展名返回 null", () => {
    expect(getOfficePreviewKind("png")).toBeNull();
    expect(getOfficePreviewKind("")).toBeNull();
    expect(getOfficePreviewKind(null)).toBeNull();
    expect(getOfficePreviewKind(undefined)).toBeNull();
  });
});

describe("OfficePreview", () => {
  it("url 为 null 时不渲染", () => {
    const { container } = render(
      <OfficePreview url={null} onClose={vi.fn()} extension="pdf" />,
    );
    expect(container.querySelector("iframe")).toBeNull();
  });

  it("PDF 走 iframe", () => {
    render(
      <OfficePreview
        url="/file/local/a.pdf"
        name="a.pdf"
        extension="pdf"
        onClose={vi.fn()}
      />,
    );
    const iframe = document.querySelector("iframe");
    expect(iframe).not.toBeNull();
    expect(iframe?.getAttribute("src")).toBe("/file/local/a.pdf");
  });

  it("不支持的扩展名展示下载与新窗口入口", () => {
    render(
      <OfficePreview
        url="/file/local/a.pptx"
        name="a.pptx"
        extension="pptx"
        onClose={vi.fn()}
      />,
    );
    expect(screen.getByText(/\.pptx 文件暂不支持在线预览/)).toBeTruthy();
    expect(screen.getAllByText("下载").length).toBeGreaterThan(0);
  });

  it("点 Esc 触发 onClose", () => {
    const onClose = vi.fn();
    render(
      <OfficePreview
        url="/file/local/a.pdf"
        name="a.pdf"
        extension="pdf"
        onClose={onClose}
      />,
    );
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalled();
  });
});
