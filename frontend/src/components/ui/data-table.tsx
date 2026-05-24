"use client";

import { type ReactNode } from "react";

import { cn } from "~/lib/utils";

/**
 * 数据表格的列定义。
 *
 * @template T 行数据类型
 */
export interface ColumnDef<T> {
  /** 列唯一标识（同时也是 `react` 的 `key`）。 */
  key: string;
  /** 列标题。 */
  title: string;
  /** 列宽（如 `"120px"` / `"15%"`）；不传时由表格自适应。 */
  width?: string;
  /** 单元格对齐方式。 */
  align?: "left" | "right" | "center";
  /** 自定义渲染：拿到行对象与索引，返回最终展示节点。 */
  render?: (row: T, index: number) => ReactNode;
}

interface DataTableProps<T> {
  /** 列定义数组。 */
  columns: ColumnDef<T>[];
  /** 行数据数组。 */
  data: T[];
  /** 取行唯一 key 的函数。 */
  rowKey: (row: T) => string | number;
  /** 是否处于加载中状态（会展示 “加载中…”）。 */
  loading?: boolean;
  /** 空数据展示内容，默认 “暂无数据”。 */
  empty?: ReactNode;
}

/**
 * 通用数据表格。
 *
 * - 列宽既可以通过 `width` 指定，也可以让浏览器自适应；
 * - 无 `render` 时会按 `row[col.key]` 取值并兜底显示 “—”；
 * - 与 `Pagination` 组合即可实现分页表格。
 */
export function DataTable<T>({
  columns,
  data,
  rowKey,
  loading,
  empty = "暂无数据",
}: DataTableProps<T>) {
  return (
    <div className="overflow-hidden rounded-md border border-zinc-200 bg-white">
      <table className="w-full text-sm">
        <thead className="bg-zinc-50 text-zinc-600">
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                style={col.width ? { width: col.width } : undefined}
                className={cn(
                  "px-3 py-2 text-left font-medium",
                  col.align === "right" && "text-right",
                  col.align === "center" && "text-center",
                )}
              >
                {col.title}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-3 py-10 text-center text-zinc-400"
              >
                加载中…
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-3 py-10 text-center text-zinc-400"
              >
                {empty}
              </td>
            </tr>
          ) : (
            data.map((row, idx) => (
              <tr
                key={rowKey(row)}
                className="border-t border-zinc-100 hover:bg-zinc-50/60"
              >
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={cn(
                      "px-3 py-2",
                      col.align === "right" && "text-right",
                      col.align === "center" && "text-center",
                    )}
                  >
                    {col.render
                      ? col.render(row, idx)
                      : ((row as unknown as Record<string, ReactNode>)[col.key] ??
                        "—")}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

interface PaginationProps {
  /** 当前页（从 1 开始）。 */
  page: number;
  /** 每页条数。 */
  size: number;
  /** 总条数。 */
  total: number;
  /** 翻页回调。 */
  onPageChange: (page: number) => void;
}

/**
 * 简易分页组件，与 `DataTable` 配套使用。
 *
 * 内部根据 `total / size` 计算总页数，按钮自动禁用边界状态。
 */
export function Pagination({
  page,
  size,
  total,
  onPageChange,
}: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / size));
  return (
    <div className="flex items-center justify-end gap-3 py-2 text-sm text-zinc-600">
      <span>
        共 {total} 条，第 {page} / {totalPages} 页
      </span>
      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
        className="rounded border border-zinc-300 px-2 py-1 text-xs disabled:opacity-50"
      >
        上一页
      </button>
      <button
        type="button"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
        className="rounded border border-zinc-300 px-2 py-1 text-xs disabled:opacity-50"
      >
        下一页
      </button>
    </div>
  );
}
