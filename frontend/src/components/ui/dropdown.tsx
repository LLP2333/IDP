"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";

import { cn } from "~/lib/utils";

/**
 * 下拉菜单项。
 */
export interface DropdownItem {
  /** 唯一 key，用于 React 列表。 */
  key: string;
  /** 文本或自定义节点。 */
  label: ReactNode;
  /** 可选图标。 */
  icon?: ReactNode;
  /** 点击回调；返回 false 可阻止自动关闭。 */
  onSelect?: () => void | boolean;
  /** 是否禁用。 */
  disabled?: boolean;
  /** 是否危险动作（红色文字）。 */
  danger?: boolean;
  /** 用作分隔符的占位项。 */
  divider?: boolean;
}

/**
 * `Dropdown` Props。
 */
export interface DropdownProps {
  /** 触发器节点，建议自行处理点击高亮。 */
  trigger: ReactNode;
  /** 菜单项。 */
  items: DropdownItem[];
  /** 自定义对齐方向，默认 `start`（左对齐）。 */
  align?: "start" | "end";
  /**
   * 触发方式：
   * - `click`（默认）：点击 trigger 切换显示；
   * - `hover`：鼠标悬浮即展开，移出 trigger / 菜单后延迟收起（兼顾触屏点击）。
   */
  triggerOn?: "click" | "hover";
  /** 自定义菜单 className。 */
  menuClassName?: string;
  /** 自定义容器 className。 */
  className?: string;
}

/** hover 模式下离开 trigger 后到收起菜单的延迟（毫秒），把 trigger ↔ 菜单之间的"空隙"算作同一悬浮区域。 */
const HOVER_CLOSE_DELAY = 150;

/**
 * 通用下拉菜单。
 *
 * 通过 `triggerOn` 选择点击或悬浮触发，监听文档点击与 `Escape` 自动关闭。
 */
export function Dropdown({
  trigger,
  items,
  align = "start",
  triggerOn = "click",
  menuClassName,
  className,
}: DropdownProps) {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement | null>(null);
  const hoverCloseTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!open) return;
    function handleClickAway(e: MouseEvent) {
      if (!wrapperRef.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    function handleEscape(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", handleClickAway);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickAway);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open]);

  useEffect(
    () => () => {
      if (hoverCloseTimer.current) clearTimeout(hoverCloseTimer.current);
    },
    [],
  );

  const cancelClose = () => {
    if (hoverCloseTimer.current) {
      clearTimeout(hoverCloseTimer.current);
      hoverCloseTimer.current = null;
    }
  };

  const scheduleClose = () => {
    cancelClose();
    hoverCloseTimer.current = setTimeout(() => setOpen(false), HOVER_CLOSE_DELAY);
  };

  const isHover = triggerOn === "hover";

  return (
    <div
      ref={wrapperRef}
      className={cn("relative inline-block", className)}
      onMouseEnter={
        isHover
          ? () => {
              cancelClose();
              setOpen(true);
            }
          : undefined
      }
      onMouseLeave={isHover ? scheduleClose : undefined}
    >
      <div
        onClick={() => setOpen((p) => !p)}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") setOpen((p) => !p);
        }}
      >
        {trigger}
      </div>
      {open ? (
        <div
          role="menu"
          className={cn(
            // 默认按内容自适应宽度，避免菜单项右侧出现大段留白；调用方需要更宽时通过 `menuClassName` 覆盖
            "absolute z-50 mt-1 min-w-fit rounded-md border border-zinc-200 bg-white py-1 shadow-lg",
            align === "end" ? "right-0" : "left-0",
            menuClassName,
          )}
        >
          {items.map((item) =>
            item.divider ? (
              <div key={item.key} className="my-1 h-px bg-zinc-100" role="separator" />
            ) : (
              <button
                key={item.key}
                type="button"
                role="menuitem"
                disabled={item.disabled}
                onClick={() => {
                  const result = item.onSelect?.();
                  if (result !== false) setOpen(false);
                }}
                className={cn(
                  "flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm transition-colors",
                  item.danger ? "text-red-600" : "text-zinc-700",
                  item.disabled
                    ? "cursor-not-allowed opacity-50"
                    : "hover:bg-zinc-50",
                )}
              >
                {item.icon}
                <span>{item.label}</span>
              </button>
            ),
          )}
        </div>
      ) : null}
    </div>
  );
}
