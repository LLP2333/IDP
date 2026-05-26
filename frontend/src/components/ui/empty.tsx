"use client";

import { FolderOpen } from "lucide-react";
import { type ReactNode } from "react";

import { cn } from "~/lib/utils";

/**
 * `Empty` Props。
 */
export interface EmptyProps {
  /** 自定义图标，默认 `FolderOpen`。 */
  icon?: ReactNode;
  /** 主文案，默认 “暂无数据”。 */
  title?: ReactNode;
  /** 补充描述。 */
  description?: ReactNode;
  /** 操作区域（按钮等）。 */
  action?: ReactNode;
  /** 自定义 className。 */
  className?: string;
}

/**
 * 通用空状态组件。
 *
 * 用于列表 / 表格 / 网格等没有数据时的占位。
 */
export function Empty({ icon, title = "暂无数据", description, action, className }: EmptyProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-2 rounded-md border border-dashed border-zinc-200 px-4 py-12 text-center",
        className,
      )}
    >
      <div className="text-zinc-400">{icon ?? <FolderOpen className="h-10 w-10" />}</div>
      <div className="text-sm font-medium text-zinc-700">{title}</div>
      {description ? <div className="text-xs text-zinc-500">{description}</div> : null}
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}
