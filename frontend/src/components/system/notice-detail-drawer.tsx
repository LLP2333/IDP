"use client";

import { useEffect } from "react";

import { DictBadge } from "~/components/system/dict-badge";
import { Badge } from "~/components/ui/badge";
import { useDict } from "~/lib/hooks/use-dict";
import type { NoticeDetailResp } from "~/lib/api/types";
import { cn, formatDateTime } from "~/lib/utils";

interface NoticeDetailDrawerProps {
  /** 是否展示。 */
  open: boolean;
  /** 关闭回调。 */
  onClose: () => void;
  /** 公告详情（关闭状态下可为 null）。 */
  data: NoticeDetailResp | null;
}

/**
 * 公告详情抽屉。
 *
 * <p>右侧滑入式抽屉，对齐 continew-admin {@code DetailDrawer.vue} 的信息架构：
 * 标题 / 元信息 / 通知方式 / 通知用户 / 正文。</p>
 *
 * <p>正文按纯文本 / Markdown 处理：使用 {@code <pre className="whitespace-pre-wrap">}
 * 保留换行；后续若需要渲染 Markdown，再引入 {@code react-markdown}。</p>
 */
export function NoticeDetailDrawer({ open, onClose, data }: NoticeDetailDrawerProps) {
  const typeDict = useDict("notice_type", open);
  const scopeDict = useDict("notice_scope_enum", open);
  const methodDict = useDict("notice_method_enum", open);
  const statusDict = useDict("notice_status_enum", open);

  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-40">
      <div className="absolute inset-0 bg-black/30" onClick={onClose} />
      <div className="absolute right-0 top-0 flex h-full w-full max-w-2xl flex-col bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-zinc-200 px-5 py-3">
          <h3 className="text-base font-semibold">公告详情</h3>
          <button
            type="button"
            className="text-zinc-400 hover:text-zinc-600"
            onClick={onClose}
            aria-label="关闭"
          >
            ✕
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-5 py-4 text-sm">
          {!data ? (
            <div className="text-zinc-400">无数据</div>
          ) : (
            <div className="flex flex-col gap-3">
              <Row label="标题" value={data.title} valueClass="font-semibold text-zinc-900" />
              <Row
                label="分类"
                value={<DictBadge items={typeDict.items} value={data.type} />}
              />
              <Row
                label="状态"
                value={<DictBadge items={statusDict.items} value={data.status} />}
              />
              <Row
                label="通知范围"
                value={<DictBadge items={scopeDict.items} value={data.noticeScope} />}
              />
              <Row
                label="通知方式"
                value={
                  data.noticeMethods && data.noticeMethods.length > 0 ? (
                    <div className="flex flex-wrap gap-1">
                      {data.noticeMethods.map((m) => (
                        <DictBadge key={m} items={methodDict.items} value={m} />
                      ))}
                    </div>
                  ) : (
                    <span className="text-zinc-400">—</span>
                  )
                }
              />
              <Row
                label="是否置顶"
                value={
                  data.isTop ? (
                    <Badge tone="warning">置顶</Badge>
                  ) : (
                    <Badge tone="default">否</Badge>
                  )
                }
              />
              <Row label="发布时间" value={formatDateTime(data.publishTime)} />
              <Row label="发布人" value={data.createUserString ?? "—"} />
              <Row label="创建时间" value={formatDateTime(data.createdAt)} />
              {data.noticeScope === 2 && data.noticeUsers && data.noticeUsers.length > 0 ? (
                <Row
                  label="通知用户"
                  value={
                    <span>已指定 {data.noticeUsers.length} 位用户</span>
                  }
                />
              ) : null}
              <div className="mt-2 flex flex-col gap-1">
                <span className="text-xs font-medium text-zinc-500">正文</span>
                <pre className="whitespace-pre-wrap rounded-md border border-zinc-200 bg-zinc-50 p-3 text-sm leading-relaxed text-zinc-800">
                  {data.content}
                </pre>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Row({
  label,
  value,
  valueClass,
}: {
  label: string;
  value: React.ReactNode;
  valueClass?: string;
}) {
  return (
    <div className="grid grid-cols-[80px_1fr] items-start gap-3">
      <div className="text-xs text-zinc-500">{label}</div>
      <div className={cn("min-w-0 text-sm text-zinc-700", valueClass)}>{value}</div>
    </div>
  );
}
