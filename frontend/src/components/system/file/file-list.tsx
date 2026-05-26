"use client";

import type { FileResp } from "~/lib/api/types";
import { cn } from "~/lib/utils";

import { formatBytes } from "./file-aside-statistics";
import { FileIcon } from "./file-icon";

/**
 * `FileList` Props。
 */
export interface FileListProps {
  /** 文件 / 文件夹列表。 */
  items: FileResp[];
  /** 当前选中 ID 集合。 */
  selectedIds: Set<number>;
  /** 切换选中(由勾选框触发)。 */
  onToggleSelect: (id: number, shiftKey?: boolean) => void;
  /** 切换全选。 */
  onToggleAll: () => void;
  /** 是否全选状态(用于表头复选框)。 */
  allSelected: boolean;
  /** 单击触发:文件夹进入下一级 / 文件触发预览。 */
  onOpen: (item: FileResp) => void;
  /** 右键回调。 */
  onContextMenu?: (e: React.MouseEvent, item: FileResp) => void;
  /** 自定义 className。 */
  className?: string;
}

/**
 * 文件管理列表视图(表格风)。
 *
 * <p>交互约定:</p>
 * <ul>
 *   <li>整行单击 = 打开(文件夹进入下一级 / 文件触发预览);</li>
 *   <li>勾选框 = 仅切换选中状态,不冒泡触发打开。</li>
 * </ul>
 */
export function FileList({
  items,
  selectedIds,
  onToggleSelect,
  onToggleAll,
  allSelected,
  onOpen,
  onContextMenu,
  className,
}: FileListProps) {
  return (
    <div className={cn("overflow-x-auto rounded-md border border-zinc-200 bg-white", className)}>
      <table className="w-full text-sm">
        <thead className="bg-zinc-50 text-left text-xs text-zinc-500">
          <tr>
            <th className="w-10 px-3 py-2">
              <input
                type="checkbox"
                checked={allSelected}
                onChange={onToggleAll}
                aria-label="全选"
              />
            </th>
            <th className="px-3 py-2">名称</th>
            <th className="w-32 px-3 py-2">大小</th>
            <th className="w-40 px-3 py-2">创建时间</th>
            <th className="w-32 px-3 py-2">扩展名</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => {
            const isDir = item.type === 0;
            const selected = selectedIds.has(item.id);
            return (
              <tr
                key={item.id}
                onClick={() => onOpen(item)}
                onContextMenu={(e) => onContextMenu?.(e, item)}
                className={cn(
                  "cursor-pointer border-t border-zinc-100 transition-colors hover:bg-zinc-50",
                  selected && "bg-blue-50",
                )}
              >
                <td className="px-3 py-2">
                  <input
                    type="checkbox"
                    checked={selected}
                    onChange={(e) => {
                      e.stopPropagation();
                      onToggleSelect(item.id);
                    }}
                    onClick={(e) => e.stopPropagation()}
                    aria-label={`选择 ${item.originalName}`}
                  />
                </td>
                <td className="px-3 py-2">
                  <div className="flex items-center gap-2">
                    <FileIcon
                      isDir={isDir}
                      extension={item.extension}
                      thumbnailUrl={item.thumbnailUrl}
                      size={24}
                    />
                    <span
                      className="truncate text-left text-zinc-700"
                      title={item.originalName}
                    >
                      {item.originalName}
                    </span>
                  </div>
                </td>
                <td className="px-3 py-2 text-zinc-500">
                  {isDir ? "—" : formatBytes(item.size)}
                </td>
                <td className="px-3 py-2 text-zinc-500">{item.createdAt}</td>
                <td className="px-3 py-2 text-zinc-500">{item.extension ?? "—"}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
