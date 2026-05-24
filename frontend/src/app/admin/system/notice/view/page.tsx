"use client";

import { useQuery } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";

import { DictBadge } from "~/components/system/dict-badge";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { getNotice, readNotice } from "~/lib/api/notice";
import { useDict } from "~/lib/hooks/use-dict";

/**
 * 公告预览页：标题 + 元信息 + 正文（保留换行）。
 *
 * <p>同时会上报阅读状态：进入页面成功拉到详情后调用 {@code POST /system/notice/{id}/read}，
 * 这样 message 收件箱里 “您收到一条公告通知” 的链接也能顺手标已读。</p>
 */
export default function NoticeViewPage() {
  const router = useRouter();
  const params = useSearchParams();
  const id = params.get("id");

  const typeDict = useDict("notice_type");
  const scopeDict = useDict("notice_scope_enum");
  const methodDict = useDict("notice_method_enum");
  const statusDict = useDict("notice_status_enum");

  const detailQuery = useQuery({
    queryKey: ["notice", "detail", id],
    queryFn: () => getNotice(id!),
    enabled: !!id,
  });

  useEffect(() => {
    if (id) {
      void readNotice(id).catch(() => {
        // 标记已读失败不影响阅读体验，忽略即可
      });
    }
  }, [id]);

  if (!id) {
    return <div className="text-sm text-zinc-500">未指定公告 ID。</div>;
  }
  if (detailQuery.isLoading) {
    return <div className="text-sm text-zinc-500">加载中…</div>;
  }
  if (!detailQuery.data) {
    return <div className="text-sm text-zinc-500">公告不存在或已被删除。</div>;
  }

  const data = detailQuery.data;
  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4">
      <div className="flex items-center justify-between">
        <Button variant="outline" size="sm" onClick={() => router.back()}>
          ← 返回
        </Button>
      </div>
      <div className="rounded-md border border-zinc-200 bg-white px-6 py-6">
        <div className="flex flex-wrap items-center gap-2">
          {data.isTop ? <Badge tone="warning">置顶</Badge> : null}
          <DictBadge items={typeDict.items} value={data.type} />
          <DictBadge items={statusDict.items} value={data.status} />
        </div>
        <h1 className="mt-3 text-2xl font-semibold text-zinc-900">{data.title}</h1>
        <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-zinc-500">
          <span>发布人：{data.createUserString ?? "—"}</span>
          <span>发布时间：{data.publishTime ?? "—"}</span>
          <span>
            通知范围：
            <DictBadge items={scopeDict.items} value={data.noticeScope} />
          </span>
          {data.noticeMethods && data.noticeMethods.length > 0 ? (
            <span className="flex items-center gap-1">
              通知方式：
              {data.noticeMethods.map((m) => (
                <DictBadge key={m} items={methodDict.items} value={m} />
              ))}
            </span>
          ) : null}
        </div>
        <pre className="mt-6 whitespace-pre-wrap rounded-md border border-zinc-100 bg-zinc-50 p-4 text-sm leading-7 text-zinc-800">
          {data.content}
        </pre>
      </div>
    </div>
  );
}
