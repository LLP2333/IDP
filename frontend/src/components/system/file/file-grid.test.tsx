import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { FileResp } from "~/lib/api/types";

import { FileGrid } from "./file-grid";

function makeItem(overrides: Partial<FileResp> = {}): FileResp {
  return {
    id: 1,
    name: "1.png",
    originalName: "封面.png",
    size: 1024,
    url: "/file/local/1.png",
    thumbnailUrl: null,
    parentPath: "/",
    path: "/封面.png",
    extension: "png",
    contentType: "image/png",
    type: 2,
    sha256: null,
    metadata: null,
    storageId: 1,
    storageName: "local",
    createdAt: "2026-01-01T00:00:00",
    updatedAt: null,
    ...overrides,
  };
}

describe("FileGrid 交互", () => {
  it("单击卡片触发 onOpen", () => {
    const onOpen = vi.fn();
    render(
      <FileGrid
        items={[makeItem()]}
        selectedIds={new Set()}
        onToggleSelect={vi.fn()}
        onOpen={onOpen}
      />,
    );
    fireEvent.click(screen.getByText("封面.png"));
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it("点勾选框只切换选中,不触发 onOpen", () => {
    const onOpen = vi.fn();
    const onToggleSelect = vi.fn();
    render(
      <FileGrid
        items={[makeItem()]}
        selectedIds={new Set()}
        onToggleSelect={onToggleSelect}
        onOpen={onOpen}
      />,
    );
    fireEvent.click(screen.getByLabelText("选择 封面.png"));
    expect(onToggleSelect).toHaveBeenCalledWith(1, false);
    expect(onOpen).not.toHaveBeenCalled();
  });

  it("右键触发 onContextMenu 而不是 onOpen", () => {
    const onOpen = vi.fn();
    const onContextMenu = vi.fn();
    render(
      <FileGrid
        items={[makeItem()]}
        selectedIds={new Set()}
        onToggleSelect={vi.fn()}
        onOpen={onOpen}
        onContextMenu={onContextMenu}
      />,
    );
    fireEvent.contextMenu(screen.getByText("封面.png"));
    expect(onContextMenu).toHaveBeenCalledTimes(1);
    expect(onOpen).not.toHaveBeenCalled();
  });
});
