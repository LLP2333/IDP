"use client";

import { useEffect, type ReactNode } from "react";

import { cn } from "~/lib/utils";

interface ModalProps {
  /** 是否可见。 */
  open: boolean;
  /** 关闭回调（点击右上角 ✕ 或按下 ESC 时触发）。 */
  onClose: () => void;
  /** 弹窗标题。 */
  title: string;
  /** 弹窗主体内容（通常是表单）。 */
  children: ReactNode;
  /** 底部操作区（可选）。 */
  footer?: ReactNode;
  /** 弹窗宽度档位。 */
  size?: "sm" | "md" | "lg";
}

const sizeClass = {
  sm: "max-w-sm",
  md: "max-w-lg",
  lg: "max-w-2xl",
};

/**
 * 通用模态弹窗。
 *
 * - 支持 ESC 关闭；
 * - 点击内容区域不会冒泡触发关闭；
 * - 通过 `footer` 槽渲染底部按钮，便于在外层用 `react-hook-form` 控制提交按钮。
 */
export function Modal({
  open,
  onClose,
  title,
  children,
  footer,
  size = "md",
}: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div
        className={cn(
          "w-full rounded-lg bg-white shadow-xl",
          sizeClass[size],
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-zinc-200 px-5 py-3">
          <h2 className="text-base font-semibold">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-zinc-400 hover:text-zinc-600"
            aria-label="关闭"
          >
            ✕
          </button>
        </div>
        <div className="px-5 py-4">{children}</div>
        {footer ? (
          <div className="flex justify-end gap-2 border-t border-zinc-200 px-5 py-3">
            {footer}
          </div>
        ) : null}
      </div>
    </div>
  );
}
