"use client";

import { ChevronRight } from "lucide-react";
import { Fragment, type ReactNode } from "react";

import { cn } from "~/lib/utils";

/**
 * 单项面包屑配置。
 */
export interface BreadcrumbItem {
  /** 显示文本或自定义节点。 */
  label: ReactNode;
  /** 点击回调，未提供时该项不可点击。 */
  onClick?: () => void;
  /** 自定义图标，渲染在 label 前。 */
  icon?: ReactNode;
}

/**
 * `Breadcrumb` Props。
 */
export interface BreadcrumbProps {
  /** 面包屑项数组，最后一项默认置灰且不响应点击。 */
  items: BreadcrumbItem[];
  /** 自定义分隔符，默认 `>`。 */
  separator?: ReactNode;
  /** 自定义容器 className。 */
  className?: string;
}

/**
 * 通用面包屑组件。
 *
 * 用于文件管理页根据 `parentPath` 拆分出层级。
 */
export function Breadcrumb({ items, separator, className }: BreadcrumbProps) {
  return (
    <nav className={cn("flex items-center gap-1 text-sm", className)} aria-label="breadcrumb">
      {items.map((item, idx) => {
        const isLast = idx === items.length - 1;
        const clickable = !isLast && typeof item.onClick === "function";
        return (
          <Fragment key={idx}>
            <button
              type="button"
              disabled={!clickable}
              onClick={clickable ? item.onClick : undefined}
              className={cn(
                "inline-flex items-center gap-1 rounded px-1.5 py-0.5 transition-colors",
                clickable
                  ? "text-zinc-600 hover:bg-zinc-100 hover:text-blue-600"
                  : "cursor-default text-zinc-500",
                isLast && "text-zinc-900",
              )}
            >
              {item.icon}
              <span>{item.label}</span>
            </button>
            {!isLast ? (
              <span className="text-zinc-400" aria-hidden>
                {separator ?? <ChevronRight className="h-3.5 w-3.5" />}
              </span>
            ) : null}
          </Fragment>
        );
      })}
    </nav>
  );
}
