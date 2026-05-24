"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";

import { LoginConfigForm } from "~/components/system/login-config-form";
import { SecurityConfigForm } from "~/components/system/security-config-form";
import { SiteConfigForm } from "~/components/system/site-config-form";
import { Tabs, type TabItem } from "~/components/ui/tabs";
import { usePermission } from "~/lib/hooks/use-permission";
import { SITE_CONFIG_QUERY_KEY } from "~/lib/hooks/use-site-config";
import { listOption } from "~/lib/api/option";
import type { OptionCategory, OptionResp } from "~/lib/api/types";

/** 系统配置页面（SITE / PASSWORD / LOGIN 三个 Tab，按权限过滤）。 */
export default function SystemConfigPage() {
  const { hasPermission } = usePermission();
  const queryClient = useQueryClient();
  const queryKey = ["system", "option", "all"] as const;

  const { data, isLoading, error } = useQuery({
    queryKey,
    queryFn: () => listOption(),
  });

  const grouped = useMemo(() => {
    const map: Record<OptionCategory, OptionResp[]> = {
      SITE: [],
      PASSWORD: [],
      LOGIN: [],
    };
    for (const opt of data ?? []) map[opt.category].push(opt);
    return map;
  }, [data]);

  const reload = () => {
    void queryClient.invalidateQueries({ queryKey });
    void queryClient.invalidateQueries({ queryKey: SITE_CONFIG_QUERY_KEY });
    void queryClient.invalidateQueries({ queryKey: ["public", "login"] });
  };

  const tabs: TabItem[] = [];
  if (hasPermission("system:siteConfig:get")) {
    tabs.push({
      key: "SITE",
      label: "网站配置",
      content: (
        <SiteConfigForm
          options={grouped.SITE}
          onSaved={reload}
          readonly={!hasPermission("system:siteConfig:update")}
        />
      ),
    });
  }
  if (hasPermission("system:securityConfig:get")) {
    tabs.push({
      key: "PASSWORD",
      label: "安全配置",
      content: (
        <SecurityConfigForm
          options={grouped.PASSWORD}
          onSaved={reload}
          readonly={!hasPermission("system:securityConfig:update")}
        />
      ),
    });
  }
  if (hasPermission("system:loginConfig:get")) {
    tabs.push({
      key: "LOGIN",
      label: "登录配置",
      content: (
        <LoginConfigForm
          options={grouped.LOGIN}
          onSaved={reload}
          readonly={!hasPermission("system:loginConfig:update")}
        />
      ),
    });
  }
  const [active, setActive] = useState<string>(tabs[0]?.key ?? "SITE");

  if (isLoading) return <div className="text-sm text-zinc-500">加载中…</div>;
  if (error) return <div className="text-sm text-red-500">加载失败：{error.message}</div>;
  if (tabs.length === 0) {
    return <div className="text-sm text-zinc-500">无系统配置查看权限</div>;
  }

  return (
    <div className="flex flex-col gap-4">
      <header>
        <h2 className="text-lg font-bold text-zinc-900">系统配置</h2>
        <p className="text-xs text-zinc-500">维护网站基础信息、密码策略与登录配置。</p>
      </header>
      <Tabs value={active} onChange={setActive} items={tabs} />
    </div>
  );
}
