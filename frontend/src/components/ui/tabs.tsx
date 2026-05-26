"use client";

import type { ReactNode } from "react";

import { cn } from "~/lib/utils";

/** 单个 Tab 项。 */
export interface TabItem {
  /** 唯一 key（与 value 对齐）。 */
  key: string;
  /** Tab 标题。 */
  label: ReactNode;
  /** Tab 内容；为空时不渲染。 */
  content?: ReactNode;
  /** 是否禁用（仍展示，但不可点击）。 */
  disabled?: boolean;
}

/** Tabs 组件 props。 */
export interface TabsProps {
  /** 当前选中的 key。 */
  value: string;
  /** 切换回调。 */
  onChange: (value: string) => void;
  /** Tab 列表。 */
  items: TabItem[];
  /** 排列方向：horizontal=顶部，vertical=左侧。 */
  orientation?: "horizontal" | "vertical";
  /** 根容器额外样式。 */
  className?: string;
  /** 内容面板额外样式。 */
  panelClassName?: string;
}

/**
 * 通用 Tabs 组件（受控）。
 *
 * 使用 Tailwind 实现，与 `system/config` 等多 Tab 页面共用。
 */
export function Tabs({
  value,
  onChange,
  items,
  orientation = "horizontal",
  className,
  panelClassName,
}: TabsProps) {
  const active = items.find((it) => it.key === value);
  if (orientation === "vertical") {
    return (
      <div className={cn("flex gap-6", className)}>
        <div role="tablist" aria-orientation="vertical" className="flex w-44 flex-col gap-1">
          {items.map((it) => (
            <button
              key={it.key}
              type="button"
              role="tab"
              aria-selected={it.key === value}
              disabled={it.disabled}
              onClick={() => !it.disabled && onChange(it.key)}
              className={cn(
                "rounded-md px-3 py-2 text-left text-sm font-medium transition-colors",
                "focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:outline-none",
                it.key === value
                  ? "bg-blue-50 text-blue-700"
                  : "text-zinc-600 hover:bg-zinc-50",
                it.disabled && "cursor-not-allowed opacity-50",
              )}
            >
              {it.label}
            </button>
          ))}
        </div>
        <div className={cn("flex-1", panelClassName)} role="tabpanel">
          {active?.content}
        </div>
      </div>
    );
  }

  return (
    <div className={className}>
      <div role="tablist" className="flex border-b border-zinc-200">
        {items.map((it) => (
          <button
            key={it.key}
            type="button"
            role="tab"
            aria-selected={it.key === value}
            disabled={it.disabled}
            onClick={() => !it.disabled && onChange(it.key)}
            className={cn(
              "-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors",
              "focus-visible:outline-none",
              it.key === value
                ? "border-blue-600 text-blue-700"
                : "border-transparent text-zinc-600 hover:text-zinc-900",
              it.disabled && "cursor-not-allowed opacity-50",
            )}
          >
            {it.label}
          </button>
        ))}
      </div>
      <div className={cn("pt-4", panelClassName)} role="tabpanel">
        {active?.content}
      </div>
    </div>
  );
}
