"use client";

import { forwardRef, type InputHTMLAttributes } from "react";

import { cn } from "~/lib/utils";

/**
 * Input Props，继承原生 `input` 全部属性。
 */
export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  /** 是否处于校验失败状态，会切换为红色边框。 */
  invalid?: boolean;
}

/**
 * 通用 Input 组件。
 *
 * 使用 `forwardRef` 把 `ref` 透传给底层 `input`，方便配合 `react-hook-form`。
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, invalid, ...rest }, ref) => (
    <input
      ref={ref}
      className={cn(
        "h-9 w-full rounded-md border bg-white px-3 text-sm transition-colors",
        "placeholder:text-zinc-400",
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
Input.displayName = "Input";
