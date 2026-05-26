"use client";

import { useEffect, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";

import { cn } from "~/lib/utils";

/**
 * 上下文菜单项。
 */
export interface ContextMenuItem {
  /** 唯一 key。 */
  key: string;
  /** 文本或自定义节点。 */
  label: ReactNode;
  /** 可选图标。 */
  icon?: ReactNode;
  /** 点击回调。 */
  onSelect?: () => void;
  /** 是否禁用。 */
  disabled?: boolean;
  /** 是否危险动作。 */
  danger?: boolean;
  /** 分隔符占位项。 */
  divider?: boolean;
}

/**
 * 上下文菜单状态。
 */
export interface ContextMenuState {
  /** X 坐标（相对视口）。 */
  x: number;
  /** Y 坐标（相对视口）。 */
  y: number;
  /** 菜单项。 */
  items: ContextMenuItem[];
}

/**
 * 通用上下文菜单（受控）。
 *
 * 子页面用 `useContextMenu()` Hook 管理状态，然后渲染本组件。
 */
export interface ContextMenuProps {
  /** 当前状态；为 null 表示不展示。 */
  state: ContextMenuState | null;
  /** 关闭回调。 */
  onClose: () => void;
}

export function ContextMenu({ state, onClose }: ContextMenuProps) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!state) return;
    function handleClickAway() {
      onClose();
    }
    function handleEscape(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("mousedown", handleClickAway);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickAway);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [state, onClose]);

  if (!mounted || !state) return null;

  return createPortal(
    <div
      role="menu"
      onMouseDown={(e) => e.stopPropagation()}
      style={{ position: "fixed", top: state.y, left: state.x }}
      className="z-50 min-w-[10rem] rounded-md border border-zinc-200 bg-white py-1 shadow-lg"
    >
      {state.items.map((item) =>
        item.divider ? (
          <div key={item.key} className="my-1 h-px bg-zinc-100" role="separator" />
        ) : (
          <button
            key={item.key}
            type="button"
            role="menuitem"
            disabled={item.disabled}
            onClick={() => {
              item.onSelect?.();
              onClose();
            }}
            className={cn(
              "flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm transition-colors",
              item.danger ? "text-red-600" : "text-zinc-700",
              item.disabled ? "cursor-not-allowed opacity-50" : "hover:bg-zinc-50",
            )}
          >
            {item.icon}
            <span>{item.label}</span>
          </button>
        ),
      )}
    </div>,
    document.body,
  );
}

/**
 * `useContextMenu` 的返回值。
 */
export interface UseContextMenuReturn {
  /** 当前状态。 */
  state: ContextMenuState | null;
  /** 弹出菜单。 */
  open: (e: React.MouseEvent | MouseEvent, items: ContextMenuItem[]) => void;
  /** 关闭菜单。 */
  close: () => void;
}

/**
 * 上下文菜单 Hook：封装位置计算与状态管理。
 */
export function useContextMenu(): UseContextMenuReturn {
  const [state, setState] = useState<ContextMenuState | null>(null);
  return {
    state,
    open: (e, items) => {
      e.preventDefault();
      const x = "clientX" in e ? e.clientX : 0;
      const y = "clientY" in e ? e.clientY : 0;
      setState({ x, y, items });
    },
    close: () => setState(null),
  };
}
