"use client";

import { useQuery } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";

import { Button } from "~/components/ui/button";
import { Modal } from "~/components/ui/modal";
import { listPopupNotice, readNotice } from "~/lib/api/notice";
import { useAuthStore } from "~/lib/store/auth-store";

const SESSION_KEY = "idp:notice:popup-shown";

/**
 * 取本次浏览器会话已展示过的弹窗公告 ID 集合。
 *
 * <p>用 {@code sessionStorage} 而不是 {@code localStorage}：</p>
 * <ul>
 *   <li>关闭浏览器会重置，符合 “每次会话展示一次” 的预期；</li>
 *   <li>跨标签页独立，避免一个标签关掉后另一个标签静默；</li>
 * </ul>
 */
function getShownIds(): Set<number> {
  if (typeof window === "undefined") return new Set();
  try {
    const raw = window.sessionStorage.getItem(SESSION_KEY);
    return raw ? new Set(JSON.parse(raw) as number[]) : new Set();
  } catch {
    return new Set();
  }
}

function rememberShown(id: number) {
  if (typeof window === "undefined") return;
  try {
    const next = getShownIds();
    next.add(id);
    window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(Array.from(next)));
  } catch {
    // 隐私模式 / quota 失败 — 忽略，下次还能正常弹
  }
}

/**
 * 登录后强制阅读的弹窗公告。
 *
 * <p>策略：</p>
 * <ol>
 *   <li>登录态进入 admin 后台后拉取 {@code GET /system/notice/popup}；</li>
 *   <li>过滤掉本次会话已弹过的 ID；</li>
 *   <li>剩余的依次展示，关闭一个调用 {@code read} 接口标已读；</li>
 *   <li>关闭后再次 invalidate 让数据刷新。</li>
 * </ol>
 */
export function NoticePopup() {
  const token = useAuthStore((s) => s.token);
  const hydrated = useAuthStore((s) => s.hydrated);

  const popupQuery = useQuery({
    queryKey: ["notice", "popup"],
    queryFn: listPopupNotice,
    enabled: hydrated && !!token,
    refetchOnWindowFocus: false,
    staleTime: 60 * 1000,
  });

  const [index, setIndex] = useState(0);
  const [pending, setPending] = useState(false);
  const list = useMemo(() => popupQuery.data ?? [], [popupQuery.data]);

  // 来到 admin 后，找出第一个 “本会话还没弹过” 的公告，设定 index
  useEffect(() => {
    if (list.length === 0) {
      setIndex(0);
      return;
    }
    const shown = getShownIds();
    const next = list.findIndex((n) => !shown.has(n.id));
    setIndex(next === -1 ? list.length : next);
  }, [list]);

  if (!hydrated || !token) return null;
  if (list.length === 0 || index >= list.length) return null;
  const current = list[index];
  if (!current) return null;

  const handleClose = async () => {
    if (pending) return;
    setPending(true);
    rememberShown(current.id);
    try {
      await readNotice(current.id);
    } catch {
      // 阅读上报失败不影响关闭
    }
    setPending(false);
    setIndex((i) => i + 1);
  };

  return (
    <Modal
      open={true}
      onClose={handleClose}
      title={current.title}
      size="lg"
      footer={
        <>
          <span className="mr-auto text-xs text-zinc-400">
            {index + 1} / {list.length}
          </span>
          <Button loading={pending} onClick={handleClose}>
            我已阅读
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-2">
        <div className="flex flex-wrap gap-2 text-xs text-zinc-500">
          <span>发布时间：{current.publishTime ?? "—"}</span>
          <span>发布人：{current.createUserString ?? "—"}</span>
        </div>
        <pre className="mt-2 max-h-[60vh] overflow-y-auto whitespace-pre-wrap rounded-md border border-zinc-100 bg-zinc-50 p-3 text-sm leading-relaxed text-zinc-800">
          {current.content}
        </pre>
      </div>
    </Modal>
  );
}
