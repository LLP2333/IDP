"use client";

import { Badge } from "~/components/ui/badge";
import type { DictItemResp } from "~/lib/api/types";

/**
 * 将字典明细的 {@code color} 字段映射为 {@link Badge} 的 {@code tone}。
 *
 * <p>常见调色板：{@code primary} → info，其余按名直传，未识别时回落到 {@code default}。</p>
 */
function colorToTone(color: string | null | undefined): "default" | "success" | "warning" | "danger" | "info" {
  switch ((color ?? "").trim().toLowerCase()) {
    case "primary":
    case "info":
      return "info";
    case "success":
      return "success";
    case "warning":
      return "warning";
    case "danger":
    case "error":
      return "danger";
    default:
      return "default";
  }
}

interface DictBadgeProps {
  /** 字典明细列表（来自 {@code useDict}）。 */
  items: DictItemResp[];
  /** 当前要展示的值（业务字段，字符串或数字均可）。 */
  value: string | number | null | undefined;
  /** 值匹配不到字典明细时的 fallback 文案，默认显示原 value。 */
  fallback?: string;
}

/**
 * 根据字典明细渲染一个带颜色的 Badge：
 * 找不到匹配项时降级为灰色 Badge，避免出现空白行。
 */
export function DictBadge({ items, value, fallback }: DictBadgeProps) {
  if (value === null || value === undefined || value === "") {
    return <span className="text-zinc-400">—</span>;
  }
  const target = String(value);
  const item = items.find((x) => x.value === target);
  if (!item) {
    return <Badge tone="default">{fallback ?? target}</Badge>;
  }
  return <Badge tone={colorToTone(item.color)}>{item.label}</Badge>;
}
