"use client";

import { useMemo, useState } from "react";

import { cn } from "~/lib/utils";
import type { PermissionResp } from "~/lib/api/types";

/** PermissionTree props。 */
export interface PermissionTreeProps {
  /** 树形权限数据（已经按 parentId 组装好的根节点列表）。 */
  data: PermissionResp[];
  /** 已选中的权限 ID 集合。 */
  value: number[];
  /** 选中变更回调（已勾选的全部 ID）。 */
  onChange: (ids: number[]) => void;
  /** 是否禁用（仅展示，不可勾选）。 */
  disabled?: boolean;
  className?: string;
}

/** 提取节点 -> 子节点 ID 列表（递归）。 */
function collectIds(node: PermissionResp): number[] {
  const ids = [node.id];
  for (const child of node.children ?? []) {
    ids.push(...collectIds(child));
  }
  return ids;
}

/** 提取节点 -> 全部父节点链路（不含自己）。 */
function buildParentMap(roots: PermissionResp[]): Map<number, number> {
  const map = new Map<number, number>();
  function walk(node: PermissionResp, parentId: number) {
    map.set(node.id, parentId);
    for (const c of node.children ?? []) walk(c, node.id);
  }
  for (const r of roots) walk(r, 0);
  return map;
}

/**
 * 受控权限树。
 *
 * 行为：
 * - 父节点 checkbox 联动整棵子树；
 * - 子节点全部勾选时父节点自动勾选；
 * - 子节点部分勾选时父节点显示半选（indeterminate）。
 */
export function PermissionTree({
  data,
  value,
  onChange,
  disabled,
  className,
}: PermissionTreeProps) {
  const valueSet = useMemo(() => new Set(value), [value]);
  const parentMap = useMemo(() => buildParentMap(data), [data]);
  const [expanded, setExpanded] = useState<Set<number>>(() => {
    const ids = new Set<number>();
    for (const root of data) ids.add(root.id);
    return ids;
  });

  function isChecked(node: PermissionResp): boolean {
    if ((node.children?.length ?? 0) === 0) return valueSet.has(node.id);
    const childIds = collectIds(node).filter((id) => id !== node.id);
    return childIds.length > 0 && childIds.every((id) => valueSet.has(id));
  }

  function isIndeterminate(node: PermissionResp): boolean {
    if ((node.children?.length ?? 0) === 0) return false;
    const childIds = collectIds(node).filter((id) => id !== node.id);
    const sel = childIds.filter((id) => valueSet.has(id)).length;
    return sel > 0 && sel < childIds.length;
  }

  function handleToggle(node: PermissionResp, checked: boolean) {
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

  function renderNode(node: PermissionResp, depth: number) {
    const hasChildren = (node.children?.length ?? 0) > 0;
    const isOpen = expanded.has(node.id);
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
          <span className="text-sm text-zinc-800">{node.name}</span>
          <span className="text-xs text-zinc-400">{node.code}</span>
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
    <div className={cn("max-h-[60vh] overflow-auto rounded border border-zinc-200 p-2", className)}>
      {data.length === 0 ? (
        <div className="py-4 text-center text-sm text-zinc-400">暂无权限数据</div>
      ) : (
        data.map((n) => renderNode(n, 0))
      )}
    </div>
  );
}
