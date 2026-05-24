"use client";

import { useQuery } from "@tanstack/react-query";
import { usePathname } from "next/navigation";
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
 * - 数据到达后，自动把 `title` 写到 `document.title`，`favicon` 同步到 `<link rel="icon">` /
 *   `<link rel="shortcut icon">`；
 * - 同时依赖 {@link usePathname}：SPA 导航后 Next.js 的 metadata reconciliation 会把
 *   `<link rel=icon>` 重新写回 `metadata.icons` 里的默认值（如 `/logo.png`），导致用户自定义
 *   上传的 base64 favicon 被覆盖。因此每次路由变化都重新覆盖一次；
 * - 服务端渲染场景下不做任何 DOM 操作，仅返回查询结果，调用方自行做空值兜底。
 *
 * @returns TanStack Query 的标准返回值（`data`、`isLoading`、`error` 等）
 */
export function useSiteConfig() {
  const pathname = usePathname();
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
    // 依赖 pathname 让 SPA 导航后重新覆盖 Next.js metadata 写回的默认 favicon
  }, [query.data, pathname]);

  return query;
}

/**
 * 在 `<head>` 中同步所有 favicon `<link>` 标签，用于动态切换 favicon。
 *
 * 设计取舍：
 * - 直接操作 DOM 而非通过 Next.js Metadata API：favicon 来自 base64，要求登录后立即生效，
 *   不走完整的路由级 metadata 流程；
 * - 同时覆盖 `rel="icon"` 与 `rel="shortcut icon"` 两个变体，避免老浏览器 / IE 仍走
 *   `shortcut icon` 显示默认 `/favicon.ico`；
 * - 当现有 link href 已等于目标值时跳过赋值，避免触发不必要的 favicon 重新请求与闪烁。
 *
 * @param href 新 favicon 的 URL（http(s) 或 data URL 均可）
 */
function updateFavicon(href: string) {
  const links = document.head.querySelectorAll<HTMLLinkElement>(
    "link[rel~='icon'], link[rel='shortcut icon']",
  );
  if (links.length === 0) {
    const link = document.createElement("link");
    link.rel = "icon";
    link.href = href;
    document.head.appendChild(link);
    return;
  }
  for (const link of links) {
    if (link.href !== href) {
      link.href = href;
    }
  }
}
