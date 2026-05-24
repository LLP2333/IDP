"use client";

import { forwardRef, type SelectHTMLAttributes } from "react";

import { cn } from "~/lib/utils";

/**
 * Select Props，继承原生 `select` 全部属性。
 */
export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  /** 是否处于校验失败状态，会切换为红色边框。 */
  invalid?: boolean;
}

/**
 * 通用 Select 组件。
 *
 * 透传 `ref` 给底层 `select`，方便配合 `react-hook-form`。
 */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, invalid, ...rest }, ref) => (
    <select
      ref={ref}
      className={cn(
        "h-9 w-full rounded-md border bg-white px-2 text-sm transition-colors",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-1",
        invalid
          ? "border-red-400 focus-visible:ring-red-400"
          : "border-zinc-300 focus-visible:ring-blue-500",
        "disabled:cursor-not-allowed disabled:bg-zinc-50",
        className,
      )}
      {...rest}
    />
  ),
);
Select.displayName = "Select";
