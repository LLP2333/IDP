"use client";

import { Check } from "lucide-react";

import type { FileResp } from "~/lib/api/types";
import { cn } from "~/lib/utils";

import { formatBytes } from "./file-aside-statistics";
import { FileIcon } from "./file-icon";

/**
 * `FileGrid` Props。
 */
export interface FileGridProps {
  /** 文件 / 文件夹列表。 */
  items: FileResp[];
  /** 当前选中 ID 集合。 */
  selectedIds: Set<number>;
  /** 切换选中(由勾选框触发)。 */
  onToggleSelect: (id: number, shiftKey?: boolean) => void;
  /** 单击或双击触发:文件夹进入下一级、文件打开预览。 */
  onOpen: (item: FileResp) => void;
  /** 右键回调。 */
  onContextMenu?: (e: React.MouseEvent, item: FileResp) => void;
  /** 自定义 className。 */
  className?: string;
}

/**
 * 文件管理网格视图。
 *
 * <p>交互约定:</p>
 * <ul>
 *   <li>整张卡片单击 = 打开(文件夹进入下一级 / 文件触发预览);</li>
 *   <li>左上角勾选框 = 仅切换选中状态,不会冒泡触发打开;</li>
 *   <li>勾选框默认隐藏,鼠标 hover 或卡片已选中时显示。</li>
 * </ul>
 */
export function FileGrid({
  items,
  selectedIds,
  onToggleSelect,
  onOpen,
  onContextMenu,
  className,
}: FileGridProps) {
  return (
    <div
      className={cn(
        "grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6",
        className,
      )}
    >
      {items.map((item) => {
        const isDir = item.type === 0;
        const selected = selectedIds.has(item.id);
        return (
          <div
            key={item.id}
            role="button"
            tabIndex={0}
            onClick={() => onOpen(item)}
            onContextMenu={(e) => onContextMenu?.(e, item)}
            onKeyDown={(e) => {
              if (e.key === "Enter") onOpen(item);
            }}
            className={cn(
              "group relative flex cursor-pointer flex-col items-center gap-2 rounded-md border bg-white p-3 text-center transition-colors",
              selected
                ? "border-blue-400 ring-2 ring-blue-100"
                : "border-zinc-200 hover:border-blue-300 hover:bg-blue-50",
            )}
          >
            <button
              type="button"
              role="checkbox"
              aria-checked={selected}
              aria-label={`选择 ${item.originalName}`}
              onClick={(e) => {
                e.stopPropagation();
                onToggleSelect(item.id, e.shiftKey);
              }}
              className={cn(
                "absolute left-2 top-2 flex h-5 w-5 items-center justify-center rounded border bg-white transition-opacity",
                selected
                  ? "border-blue-500 bg-blue-500 opacity-100"
                  : "border-zinc-300 opacity-0 group-hover:opacity-100",
              )}
            >
              {selected ? <Check size={14} className="text-white" /> : null}
            </button>
            <FileIcon
              isDir={isDir}
              extension={item.extension}
              thumbnailUrl={item.thumbnailUrl}
              size={56}
            />
            <div
              className="w-full truncate text-sm text-zinc-700"
              title={item.originalName}
            >
              {item.originalName}
            </div>
            <div className="text-xs text-zinc-400">
              {isDir ? "文件夹" : formatBytes(item.size)}
            </div>
          </div>
        );
      })}
    </div>
  );
}
