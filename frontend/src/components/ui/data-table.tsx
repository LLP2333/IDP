"use client";

import { type ReactNode } from "react";

import { cn } from "~/lib/utils";

export interface ColumnDef<T> {
  key: string;
  title: string;
  width?: string;
  align?: "left" | "right" | "center";
  render?: (row: T, index: number) => ReactNode;
}

interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  rowKey: (row: T) => string | number;
  loading?: boolean;
  empty?: ReactNode;
}

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
  page: number;
  size: number;
  total: number;
  onPageChange: (page: number) => void;
}

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
