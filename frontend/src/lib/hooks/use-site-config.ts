"use client";

import { useQuery } from "@tanstack/react-query";
import { useEffect } from "react";

import { getSiteConfigPublic } from "~/lib/api/option";

/**
 * 公开站点配置在 TanStack Query 中的缓存 key。
 *
 * 系统配置页保存 SITE 类参数后，请用 `queryClient.invalidateQueries({ queryKey: SITE_CONFIG_QUERY_KEY })`
 * 触发 admin 顶部 / 登录页等所有调用方刷新。
 */
export const SITE_CONFIG_QUERY_KEY = ["public", "site"] as const;

/**
 * 拉取并同步全局站点配置。
 *
 * 行为说明：
 * - 调用 `GET /system/option/site`，与登录页、admin 顶栏共用同一份缓存（同 key 全站只触发一次请求）；
 * - 数据到达后，自动把 `title` 写到 `document.title`，`favicon` 替换 `<link rel="icon">`；
 * - 服务端渲染场景下不做任何 DOM 操作，仅返回查询结果，调用方自行做空值兜底。
 *
 * @returns TanStack Query 的标准返回值（`data`、`isLoading`、`error` 等）
 */
export function useSiteConfig() {
  const query = useQuery({
    queryKey: SITE_CONFIG_QUERY_KEY,
    queryFn: getSiteConfigPublic,
    retry: false,
    staleTime: 60 * 1000,
  });

  useEffect(() => {
    const data = query.data;
    if (typeof document === "undefined" || !data) return;
    if (data.title) document.title = data.title;
    if (data.favicon) updateFavicon(data.favicon);
  }, [query.data]);

  return query;
}

/**
 * 在 `<head>` 中替换或创建 `<link rel="icon">`，用于动态切换 favicon。
 *
 * 设计取舍：直接操作 DOM 而非通过 Next.js Metadata API，是因为 favicon 来自 base64，
 * 且要求登录后立即生效，无需走完整的路由级 metadata 流程。
 *
 * @param href 新 favicon 的 URL（http(s) 或 data URL 均可）
 */
function updateFavicon(href: string) {
  let link = document.querySelector<HTMLLinkElement>("link[rel~='icon']");
  if (!link) {
    link = document.createElement("link");
    link.rel = "icon";
    document.head.appendChild(link);
  }
  link.href = href;
}
