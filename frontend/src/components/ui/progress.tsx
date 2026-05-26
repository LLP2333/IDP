"use client";

import { cn } from "~/lib/utils";

/**
 * `Progress` Props。
 */
export interface ProgressProps {
  /** 进度值（0-100）。 */
  value: number;
  /** 自定义高度，默认 `h-2`。 */
  className?: string;
  /** 是否展示百分比文字（在条右侧）。 */
  showLabel?: boolean;
  /** 主题色，默认 blue。 */
  variant?: "blue" | "green" | "red" | "amber";
  /** 是否动画显示（不确定进度时设为 true）。 */
  indeterminate?: boolean;
}

const variantClass: Record<NonNullable<ProgressProps["variant"]>, string> = {
  blue: "bg-blue-500",
  green: "bg-green-500",
  red: "bg-red-500",
  amber: "bg-amber-500",
};

/**
 * 通用进度条。
 *
 * 用于上传 / 下载 / 计算文件夹大小等耗时操作的可视化反馈。
 */
export function Progress({ value, className, showLabel, variant = "blue", indeterminate }: ProgressProps) {
  const clamped = Math.max(0, Math.min(100, Math.round(value)));
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <div className={cn("relative h-2 w-full overflow-hidden rounded-full bg-zinc-100")}>
        <div
          data-testid="progress-bar"
          className={cn(
            "h-full transition-[width] duration-200",
            variantClass[variant],
            indeterminate && "animate-pulse",
          )}
          style={{ width: indeterminate ? "100%" : `${clamped}%` }}
        />
      </div>
      {showLabel ? <span className="text-xs text-zinc-500 tabular-nums">{clamped}%</span> : null}
    </div>
  );
}
