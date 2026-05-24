"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCheck, Eye } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { DataTable, Pagination, type ColumnDef } from "~/components/ui/data-table";
import { Input } from "~/components/ui/input";
import { Select } from "~/components/ui/select";
import { HttpError } from "~/lib/api/http";
import { listMessage, readAllMessage, readMessage } from "~/lib/api/message";
import type { MessageResp } from "~/lib/api/types";

/**
 * 消息中心：当前登录用户的收件箱。
 *
 * <p>支持按标题模糊 + 已读筛选；点击 “查看” 既会跳转到消息绑定的 path
 * （通常是公告详情），也会顺手把消息标记为已读。</p>
 */
export default function MessagePage() {
  const queryClient = useQueryClient();
  const router = useRouter();

  const [page, setPage] = useState(1);
  const [title, setTitle] = useState("");
  const [titleInput, setTitleInput] = useState("");
  const [isRead, setIsRead] = useState<string>("");

  const listQuery = useQuery({
    queryKey: ["message", "list", { page, title, isRead }],
    queryFn: () =>
      listMessage({
        page,
        size: 10,
        title: title || undefined,
        isRead: isRead === "" ? undefined : isRead === "true",
      }),
  });

  const readMutation = useMutation({
    mutationFn: (id: number) => readMessage(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["message"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const readAllMutation = useMutation({
    mutationFn: () => readAllMessage(),
    onSuccess: () => {
      toast.success("已全部标记为已读");
      void queryClient.invalidateQueries({ queryKey: ["message"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const handleView = (row: MessageResp) => {
    if (!row.isRead) readMutation.mutate(row.id);
    if (row.path) router.push(row.path);
  };

  const columns: ColumnDef<MessageResp>[] = [
    {
      key: "isRead",
      title: "状态",
      width: "80px",
      render: (row) =>
        row.isRead ? (
          <Badge tone="default">已读</Badge>
        ) : (
          <Badge tone="warning">未读</Badge>
        ),
    },
    {
      key: "title",
      title: "标题",
      render: (row) => (
        <button
          type="button"
          className="text-blue-600 hover:underline"
          onClick={() => handleView(row)}
        >
          {row.title}
        </button>
      ),
    },
    {
      key: "content",
      title: "摘要",
      render: (row) => (
        <span className="text-zinc-600">
          {row.content.length > 60 ? `${row.content.slice(0, 60)}…` : row.content}
        </span>
      ),
    },
    {
      key: "createdAt",
      title: "时间",
      width: "180px",
    },
    {
      key: "actions",
      title: "操作",
      width: "100px",
      align: "right",
      render: (row) => (
        <Button size="sm" variant="ghost" onClick={() => handleView(row)}>
          <Eye size={14} /> 查看
        </Button>
      ),
    },
  ];

  const data = listQuery.data;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">消息中心</h2>
          <p className="mt-1 text-sm text-zinc-500">查看个人收件箱，包括系统公告、业务通知等。</p>
        </div>
        <Button
          variant="outline"
          onClick={() => readAllMutation.mutate()}
          loading={readAllMutation.isPending}
        >
          <CheckCheck size={14} /> 全部标为已读
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-2 rounded-md border border-zinc-200 bg-white p-3">
        <Input
          className="!w-56"
          placeholder="按标题搜索"
          value={titleInput}
          onChange={(e) => setTitleInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              setPage(1);
              setTitle(titleInput);
            }
          }}
        />
        <Select
          className="!w-32"
          value={isRead}
          onChange={(e) => {
            setIsRead(e.target.value);
            setPage(1);
          }}
        >
          <option value="">全部</option>
          <option value="false">未读</option>
          <option value="true">已读</option>
        </Select>
        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            setPage(1);
            setTitle(titleInput);
          }}
        >
          搜索
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => {
            setTitleInput("");
            setTitle("");
            setIsRead("");
            setPage(1);
          }}
        >
          重置
        </Button>
      </div>

      <DataTable<MessageResp>
        columns={columns}
        data={data?.list ?? []}
        rowKey={(row) => row.id}
        loading={listQuery.isLoading}
      />

      {data ? (
        <Pagination
          page={data.page}
          size={data.size}
          total={data.total}
          onPageChange={setPage}
        />
      ) : null}
    </div>
  );
}
