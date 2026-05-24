"use client";

import { useQuery } from "@tanstack/react-query";

import { listDictItemByCode } from "~/lib/api/dict";
import type { DictItemResp } from "~/lib/api/types";

/**
 * 字典明细在 TanStack Query 中的缓存 key 构造器。
 *
 * <p>各业务模块如需在写入字典后强制刷新某个 code 的下拉，可调用
 * {@code queryClient.invalidateQueries({ queryKey: dictQueryKey(code) })}。</p>
 */
export function dictQueryKey(code: string) {
  return ["dict", code] as const;
}

/**
 * 按字典编码拉取启用的明细列表，并附带几个常用的衍生方法。
 *
 * <p>缓存策略：</p>
 * <ul>
 *   <li>{@code staleTime} 5 分钟：字典是相对静态的配置；</li>
 *   <li>同一 {@code code} 的多个调用方共享同一份缓存；</li>
 *   <li>{@code enabled=false} 时不会触发请求，可用于条件加载。</li>
 * </ul>
 *
 * @param code    字典编码，如 {@code notice_type}
 * @param enabled 是否启用查询（默认 {@code true}）
 * @returns 查询结果 + 工具函数（按 value 找 label / color）
 */
export function useDict(code: string, enabled = true) {
  const query = useQuery<DictItemResp[]>({
    queryKey: dictQueryKey(code),
    queryFn: () => listDictItemByCode(code),
    staleTime: 5 * 60 * 1000,
    enabled,
  });

  const items = query.data ?? [];

  const getLabel = (value: string | number | null | undefined): string => {
    if (value === null || value === undefined || value === "") return "";
    const item = items.find((x) => x.value === String(value));
    return item?.label ?? String(value);
  };

  const getColor = (value: string | number | null | undefined): string | null => {
    if (value === null || value === undefined || value === "") return null;
    const item = items.find((x) => x.value === String(value));
    return item?.color ?? null;
  };

  return { ...query, items, getLabel, getColor };
}
