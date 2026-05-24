import { type HTMLAttributes } from "react";

import { cn } from "~/lib/utils";

/** Badge 的语义色调。 */
type Tone = "default" | "success" | "warning" | "danger" | "info";

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  /** 色调，默认 `default`。 */
  tone?: Tone;
}

const toneClass: Record<Tone, string> = {
  default: "bg-zinc-100 text-zinc-700",
  success: "bg-emerald-50 text-emerald-700",
  warning: "bg-amber-50 text-amber-700",
  danger: "bg-red-50 text-red-700",
  info: "bg-blue-50 text-blue-700",
};

/**
 * 行内小标签，常用于状态展示（启用/禁用、系统/自定义 等）。
 *
 * @param props.tone      色调
 * @param props.children  标签内容
 */
export function Badge({ tone = "default", className, ...rest }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
        toneClass[tone],
        className,
      )}
      {...rest}
    />
  );
}
