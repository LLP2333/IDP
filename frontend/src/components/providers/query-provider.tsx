"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

/**
 * 全局 React Query Provider。
 *
 * 通过 `useState` 把 `QueryClient` 创建在组件内部，保证 Next.js App Router 下的
 * 每次水合（hydrate）都得到稳定实例，避免开发模式下被 React Strict Mode 重复创建。
 *
 * 默认策略：
 * - `staleTime: 30s`：减少切回页面后的额外请求；
 * - `refetchOnWindowFocus: false`：避免后台标签页频繁触发请求；
 * - `retry: 1`：网络抖动时重试一次。
 *
 * @param children 任意被包裹的 React 子节点
 */
export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30 * 1000,
            refetchOnWindowFocus: false,
            retry: 1,
          },
        },
      }),
  );

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
