import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { FileResp } from "~/lib/api/types";

import { FileList } from "./file-list";

function makeItem(overrides: Partial<FileResp> = {}): FileResp {
  return {
    id: 7,
    name: "1.pdf",
    originalName: "report.pdf",
    size: 2048,
    url: "/file/local/1.pdf",
    thumbnailUrl: null,
    parentPath: "/",
    path: "/report.pdf",
    extension: "pdf",
    contentType: "application/pdf",
    type: 3,
    sha256: null,
    metadata: null,
    storageId: 1,
    storageName: "local",
    createdAt: "2026-01-01T00:00:00",
    updatedAt: null,
    ...overrides,
  };
}

describe("FileList 交互", () => {
  it("整行单击触发 onOpen", () => {
    const onOpen = vi.fn();
    render(
      <FileList
        items={[makeItem()]}
        selectedIds={new Set()}
        onToggleSelect={vi.fn()}
        onToggleAll={vi.fn()}
        allSelected={false}
        onOpen={onOpen}
      />,
    );
    fireEvent.click(screen.getByText("report.pdf"));
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it("行内勾选框只切换选中,不触发 onOpen", () => {
    const onOpen = vi.fn();
    const onToggleSelect = vi.fn();
    render(
      <FileList
        items={[makeItem()]}
        selectedIds={new Set()}
        onToggleSelect={onToggleSelect}
        onToggleAll={vi.fn()}
        allSelected={false}
        onOpen={onOpen}
      />,
    );
    fireEvent.click(screen.getByLabelText("选择 report.pdf"));
    expect(onToggleSelect).toHaveBeenCalledWith(7);
    expect(onOpen).not.toHaveBeenCalled();
  });
});
