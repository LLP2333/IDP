"use client";

import { useSiteConfig } from "~/lib/hooks/use-site-config";
import { cn } from "~/lib/utils";

/** SiteFooter props。 */
export interface SiteFooterProps {
  /** 额外类名，例如登录页可以叠加 `mt-6` 等间距。 */
  className?: string;
}

/**
 * 全站统一的版权 footer。
 *
 * 行为：
 * - 从 `useSiteConfig` 读取 `copyright` 与 `beian`；两者都为空时不渲染任何节点；
 * - 数据未到达时也不渲染，避免首屏出现一闪而过的空 footer 抖动；
 * - 与 admin layout / 登录页共用，保证“一次配置，多处生效”。
 */
export function SiteFooter({ className }: SiteFooterProps) {
  const { data } = useSiteConfig();
  const copyright = data?.copyright?.trim() ?? "";
  const beian = data?.beian?.trim() ?? "";

  if (!copyright && !beian) return null;

  return (
    <footer
      className={cn(
        "flex flex-wrap items-center justify-center gap-x-4 gap-y-1 px-4 py-3 text-center text-xs text-zinc-400",
        className,
      )}
    >
      {copyright ? <span>{copyright}</span> : null}
      {beian ? <span>{beian}</span> : null}
    </footer>
  );
}
