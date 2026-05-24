"use client";

import { useMemo, useState } from "react";

import type { MenuResp } from "~/lib/api/types";
import { cn } from "~/lib/utils";

/** MenuTree 组件 props。 */
export interface MenuTreeProps {
  /** 树形菜单数据（已经按 parentId 组装好的根节点列表）。 */
  data: MenuResp[];
  /** 已选中的菜单 ID 集合。 */
  value: number[];
  /** 选中变更回调（已勾选的全部 ID）。 */
  onChange: (ids: number[]) => void;
  /** 是否禁用（仅展示，不可勾选）。 */
  disabled?: boolean;
  className?: string;
}

/** 提取节点 -> 子节点 ID 列表（递归）。 */
function collectIds(node: MenuResp): number[] {
  const ids = [node.id];
  for (const child of node.children ?? []) {
    ids.push(...collectIds(child));
  }
  return ids;
}

/** 提取节点 -> 全部父节点链路（不含自己）。 */
function buildParentMap(roots: MenuResp[]): Map<number, number> {
  const map = new Map<number, number>();
  function walk(node: MenuResp, parentId: number) {
    map.set(node.id, parentId);
    for (const c of node.children ?? []) walk(c, node.id);
  }
  for (const r of roots) walk(r, 0);
  return map;
}

function typeBadge(type: MenuResp["type"]) {
  switch (type) {
    case 1:
      return { text: "目录", className: "bg-blue-50 text-blue-700" };
    case 2:
      return { text: "菜单", className: "bg-emerald-50 text-emerald-700" };
    case 3:
      return { text: "按钮", className: "bg-zinc-100 text-zinc-600" };
  }
}

/**
 * 受控菜单树（树形 checkbox）。
 *
 * 行为：
 * - 父节点 checkbox 联动整棵子树；
 * - 子节点全部勾选时父节点自动勾选；
 * - 子节点部分勾选时父节点显示半选（indeterminate）；
 * - 按钮节点（type=3）额外展示其 `permission` 字段，方便审阅。
 */
export function MenuTree({
  data,
  value,
  onChange,
  disabled,
  className,
}: MenuTreeProps) {
  const valueSet = useMemo(() => new Set(value), [value]);
  const parentMap = useMemo(() => buildParentMap(data), [data]);
  const [expanded, setExpanded] = useState<Set<number>>(() => {
    const ids = new Set<number>();
    for (const root of data) ids.add(root.id);
    return ids;
  });

  function isChecked(node: MenuResp): boolean {
    if ((node.children?.length ?? 0) === 0) return valueSet.has(node.id);
    const childIds = collectIds(node).filter((id) => id !== node.id);
    return childIds.length > 0 && childIds.every((id) => valueSet.has(id));
  }

  function isIndeterminate(node: MenuResp): boolean {
    if ((node.children?.length ?? 0) === 0) return false;
    const childIds = collectIds(node).filter((id) => id !== node.id);
    const sel = childIds.filter((id) => valueSet.has(id)).length;
    return sel > 0 && sel < childIds.length;
  }

  function handleToggle(node: MenuResp, checked: boolean) {
    if (disabled) return;
    const ids = collectIds(node);
    const next = new Set(value);
    if (checked) {
      for (const id of ids) next.add(id);
      let p = parentMap.get(node.id) ?? 0;
      while (p && p !== 0) {
        next.add(p);
        p = parentMap.get(p) ?? 0;
      }
    } else {
      for (const id of ids) next.delete(id);
    }
    onChange([...next]);
  }

  function renderNode(node: MenuResp, depth: number) {
    const hasChildren = (node.children?.length ?? 0) > 0;
    const isOpen = expanded.has(node.id);
    const badge = typeBadge(node.type);
    return (
      <div key={node.id} className="flex flex-col">
        <div
          className="flex items-center gap-2 py-1"
          style={{ paddingLeft: depth * 16 }}
        >
          {hasChildren ? (
            <button
              type="button"
              onClick={() =>
                setExpanded((prev) => {
                  const next = new Set(prev);
                  if (next.has(node.id)) next.delete(node.id);
                  else next.add(node.id);
                  return next;
                })
              }
              className="h-4 w-4 text-xs text-zinc-500"
              aria-label={isOpen ? "折叠" : "展开"}
            >
              {isOpen ? "▾" : "▸"}
            </button>
          ) : (
            <span className="h-4 w-4" />
          )}
          <input
            type="checkbox"
            checked={isChecked(node)}
            ref={(el) => {
              if (el) el.indeterminate = isIndeterminate(node);
            }}
            disabled={disabled}
            onChange={(e) => handleToggle(node, e.target.checked)}
          />
          <span
            className={cn(
              "rounded px-1.5 py-px text-[10px] font-medium",
              badge.className,
            )}
          >
            {badge.text}
          </span>
          <span className="text-sm text-zinc-800">{node.title}</span>
          {node.permission ? (
            <span className="text-xs text-zinc-400">{node.permission}</span>
          ) : null}
        </div>
        {hasChildren && isOpen ? (
          <div className="flex flex-col">
            {node.children!.map((c) => renderNode(c, depth + 1))}
          </div>
        ) : null}
      </div>
    );
  }

  return (
    <div
      className={cn(
        "max-h-[60vh] overflow-auto rounded border border-zinc-200 p-2",
        className,
      )}
    >
      {data.length === 0 ? (
        <div className="py-4 text-center text-sm text-zinc-400">暂无菜单数据</div>
      ) : (
        data.map((n) => renderNode(n, 0))
      )}
    </div>
  );
}
