"use client";

import { forwardRef, type ButtonHTMLAttributes } from "react";

import { cn } from "~/lib/utils";

/** 按钮视觉变体。 */
type Variant = "primary" | "secondary" | "ghost" | "danger" | "outline";
/** 按钮尺寸。 */
type Size = "sm" | "md" | "lg";

/**
 * 按钮组件 Props，继承原生 `button` 全部 HTML 属性。
 */
export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** 视觉变体，默认 `primary`。 */
  variant?: Variant;
  /** 尺寸，默认 `md`。 */
  size?: Size;
  /** 是否处于 loading 状态：会自动禁用并展示菊花。 */
  loading?: boolean;
}

const variantClass: Record<Variant, string> = {
  primary:
    "bg-blue-600 text-white hover:bg-blue-700 focus-visible:ring-blue-500 disabled:bg-blue-300",
  secondary:
    "bg-zinc-100 text-zinc-900 hover:bg-zinc-200 focus-visible:ring-zinc-400 disabled:opacity-50",
  ghost:
    "bg-transparent text-zinc-700 hover:bg-zinc-100 focus-visible:ring-zinc-400 disabled:opacity-50",
  outline:
    "border border-zinc-300 bg-white text-zinc-700 hover:bg-zinc-50 focus-visible:ring-zinc-400 disabled:opacity-50",
  danger:
    "bg-red-600 text-white hover:bg-red-700 focus-visible:ring-red-500 disabled:bg-red-300",
};

const sizeClass: Record<Size, string> = {
  sm: "h-8 px-3 text-xs",
  md: "h-9 px-4 text-sm",
  lg: "h-10 px-5 text-base",
};

/**
 * 通用按钮组件。
 *
 * - 自带 `type="button"` 默认值，避免在表单中意外触发提交；
 * - `loading` 与 `disabled` 任一为 true 时按钮均不可点击。
 */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = "primary",
      size = "md",
      loading,
      disabled,
      children,
      type = "button",
      ...rest
    },
    ref,
  ) => {
    return (
      <button
        ref={ref}
        type={type}
        disabled={disabled ?? loading}
        className={cn(
          "inline-flex items-center justify-center gap-1.5 rounded-md font-medium transition-colors",
          "focus-visible:ring-2 focus-visible:ring-offset-1 focus-visible:outline-none",
          "disabled:cursor-not-allowed",
          variantClass[variant],
          sizeClass[size],
          className,
        )}
        {...rest}
      >
        {loading ? (
          <span className="h-3 w-3 animate-spin rounded-full border-2 border-current border-t-transparent" />
        ) : null}
        {children}
      </button>
    );
  },
);
Button.displayName = "Button";
