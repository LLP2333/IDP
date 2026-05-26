"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";

import { cn } from "~/lib/utils";

/**
 * `Pagination` Props。
 */
export interface PaginationProps {
  /** 当前页码，从 1 开始。 */
  page: number;
  /** 每页条数。 */
  size: number;
  /** 总条数。 */
  total: number;
  /** 页码切换。 */
  onPageChange: (page: number) => void;
  /** 每页条数切换（可选）。 */
  onSizeChange?: (size: number) => void;
  /** 可选的每页条数选项。 */
  pageSizeOptions?: number[];
  /** 自定义 className。 */
  className?: string;
}

/**
 * 通用分页组件。
 *
 * 显示 “共 X 条 / 第 P/N 页 / 上一页 / 下一页 / 每页 N 条”。
 */
export function Pagination({
  page,
  size,
  total,
  onPageChange,
  onSizeChange,
  pageSizeOptions = [10, 20, 50, 100],
  className,
}: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / size));
  const canPrev = page > 1;
  const canNext = page < totalPages;
  return (
    <div className={cn("flex items-center justify-between gap-3 text-sm text-zinc-600", className)}>
      <span>共 {total} 条</span>
      <div className="flex items-center gap-2">
        <button
          type="button"
          disabled={!canPrev}
          onClick={() => onPageChange(page - 1)}
          className={cn(
            "inline-flex h-8 w-8 items-center justify-center rounded border border-zinc-200 transition-colors",
            canPrev ? "hover:bg-zinc-100" : "cursor-not-allowed opacity-50",
          )}
          aria-label="上一页"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <span className="px-1">
          {page} / {totalPages}
        </span>
        <button
          type="button"
          disabled={!canNext}
          onClick={() => onPageChange(page + 1)}
          className={cn(
            "inline-flex h-8 w-8 items-center justify-center rounded border border-zinc-200 transition-colors",
            canNext ? "hover:bg-zinc-100" : "cursor-not-allowed opacity-50",
          )}
          aria-label="下一页"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
        {onSizeChange ? (
          <select
            value={size}
            onChange={(e) => onSizeChange(Number(e.target.value))}
            className="h-8 rounded border border-zinc-200 bg-white px-2"
          >
            {pageSizeOptions.map((n) => (
              <option key={n} value={n}>
                {n} 条/页
              </option>
            ))}
          </select>
        ) : null}
      </div>
    </div>
  );
}
