"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { Button } from "~/components/ui/button";
import { getUnreadCount, listMessage, readMessage } from "~/lib/api/message";
import type { MessageResp } from "~/lib/api/types";
import { cn } from "~/lib/utils";

/**
 * 顶栏未读消息铃铛。
 *
 * <p>点击展开未读消息下拉，最多展示最新 5 条；点击某条会跳到对应 {@code path}
 * 并标记已读；提供 “查看全部” 入口跳转消息中心。每 60s 后台轮询未读计数。</p>
 */
export function NotificationBell() {
  const queryClient = useQueryClient();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const unreadQuery = useQuery({
    queryKey: ["message", "unread-count"],
    queryFn: getUnreadCount,
    refetchInterval: 60 * 1000,
    refetchOnWindowFocus: true,
  });
  const count = unreadQuery.data?.count ?? 0;

  const latestQuery = useQuery({
    queryKey: ["message", "list", "bell"],
    queryFn: () => listMessage({ page: 1, size: 5, isRead: false }),
    enabled: open,
  });

  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [open]);

  const handleClick = async (m: MessageResp) => {
    setOpen(false);
    if (!m.isRead) {
      try {
        await readMessage(m.id);
        void queryClient.invalidateQueries({ queryKey: ["message"] });
      } catch {
        // 阅读上报失败不阻塞跳转
      }
    }
    if (m.path) router.push(m.path);
    else router.push("/admin/message");
  };

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className={cn(
          "relative flex h-9 w-9 items-center justify-center rounded-md text-zinc-500 transition-colors hover:bg-zinc-100",
          open && "bg-zinc-100",
        )}
        aria-label="消息中心"
      >
        <Bell size={18} />
        {count > 0 ? (
          <span className="absolute right-1 top-1 inline-flex h-4 min-w-[16px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold leading-none text-white">
            {count > 99 ? "99+" : count}
          </span>
        ) : null}
      </button>
      {open ? (
        <div className="absolute right-0 top-10 z-30 w-80 rounded-md border border-zinc-200 bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-zinc-100 px-3 py-2 text-sm">
            <span className="font-medium">未读消息</span>
            <span className="text-xs text-zinc-400">共 {count} 条</span>
          </div>
          <div className="max-h-72 overflow-y-auto">
            {latestQuery.isLoading ? (
              <div className="px-3 py-6 text-center text-sm text-zinc-400">加载中…</div>
            ) : (latestQuery.data?.list ?? []).length === 0 ? (
              <div className="px-3 py-6 text-center text-sm text-zinc-400">没有未读消息</div>
            ) : (
              latestQuery.data?.list.map((m) => (
                <button
                  key={m.id}
                  type="button"
                  className="flex w-full flex-col items-start gap-1 border-b border-zinc-100 px-3 py-2 text-left text-sm hover:bg-zinc-50"
                  onClick={() => handleClick(m)}
                >
                  <span className="line-clamp-1 font-medium text-zinc-800">{m.title}</span>
                  <span className="line-clamp-2 text-xs text-zinc-500">{m.content}</span>
                  <span className="text-[11px] text-zinc-400">{m.createdAt}</span>
                </button>
              ))
            )}
          </div>
          <div className="flex justify-end border-t border-zinc-100 px-2 py-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setOpen(false);
                router.push("/admin/message");
              }}
            >
              查看全部
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
