"use client";

import { cn } from "~/lib/utils";

/** Switch 组件 props。 */
export interface SwitchProps {
  /** 当前是否开启。 */
  checked: boolean;
  /** 状态改变回调。 */
  onChange: (checked: boolean) => void;
  /** 是否禁用。 */
  disabled?: boolean;
  /** ARIA Label / 屏幕阅读器文案。 */
  label?: string;
  className?: string;
}

/**
 * 开关组件（受控）。
 *
 * Tailwind 实现，无外部依赖；可用于布尔型配置项。
 */
export function Switch({ checked, onChange, disabled, label, className }: SwitchProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={() => !disabled && onChange(!checked)}
      className={cn(
        "relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full transition-colors",
        "focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1 focus-visible:outline-none",
        checked ? "bg-blue-600" : "bg-zinc-300",
        disabled && "cursor-not-allowed opacity-50",
        className,
      )}
    >
      <span
        className={cn(
          "inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform",
          checked ? "translate-x-4" : "translate-x-0.5",
        )}
      />
    </button>
  );
}
