"use client";

import { type ReactNode } from "react";

import { cn } from "~/lib/utils";

interface FormFieldProps {
  /** 字段标题。 */
  label: string;
  /** 是否必填，会在标题后展示 `*`。 */
  required?: boolean;
  /** 错误信息：有值时优先展示，hint 会被屏蔽。 */
  error?: string;
  /** 字段提示文案（仅在没有 error 时展示）。 */
  hint?: string;
  /** 外层容器样式扩展。 */
  className?: string;
  /** 真正的表单控件（Input / Select / Textarea 等）。 */
  children: ReactNode;
}

/**
 * 通用表单字段容器：标题 + 控件 + 错误/提示。
 *
 * 与 `react-hook-form` 的 `formState.errors[xxx]?.message` 配合最为方便。
 */
export function FormField({
  label,
  required,
  error,
  hint,
  className,
  children,
}: FormFieldProps) {
  return (
    <div className={cn("flex flex-col gap-1", className)}>
      <label className="text-xs font-medium text-zinc-700">
        {label}
        {required ? <span className="ml-0.5 text-red-500">*</span> : null}
      </label>
      {children}
      {error ? <span className="text-xs text-red-500">{error}</span> : null}
      {!error && hint ? (
        <span className="text-xs text-zinc-400">{hint}</span>
      ) : null}
    </div>
  );
}
