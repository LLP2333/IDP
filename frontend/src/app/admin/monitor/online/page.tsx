"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { LogOut, RefreshCw, Search } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "~/components/ui/button";
import { DataTable, Pagination, type ColumnDef } from "~/components/ui/data-table";
import { Input } from "~/components/ui/input";
import { HttpError } from "~/lib/api/http";
import { kickoutOnlineUser, listOnlineUser } from "~/lib/api/monitor";
import type { OnlineUserResp } from "~/lib/api/types";
import { usePermission } from "~/lib/hooks/use-permission";
import { useAuthStore } from "~/lib/store/auth-store";
import { formatDateTime } from "~/lib/utils";

const PAGE_SIZE = 10;

function normalizeDateTime(value: string): string | undefined {
  return value ? value.replace("T", " ") + ":00" : undefined;
}

/** 在线用户监控页：查询当前登录会话，支持按用户与登录时间过滤，并可强退会话。 */
export default function OnlineUserPage() {
  const queryClient = useQueryClient();
  const { hasPermission } = usePermission();
  const currentToken = useAuthStore((s) => s.token);

  const [page, setPage] = useState(1);
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [startInput, setStartInput] = useState("");
  const [endInput, setEndInput] = useState("");
  const [range, setRange] = useState<string[] | undefined>();

  const listQuery = useQuery({
    queryKey: ["monitor", "online", { page, keyword, range }],
    queryFn: () =>
      listOnlineUser({
        page,
        size: PAGE_SIZE,
        nickname: keyword || undefined,
        loginTime: range,
        sort: ["loginTime,desc"],
      }),
  });

  const kickoutMutation = useMutation({
    mutationFn: kickoutOnlineUser,
    onSuccess: () => {
      toast.success("已强退用户");
      void queryClient.invalidateQueries({ queryKey: ["monitor", "online"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "强退失败"),
  });

  const applySearch = () => {
    const start = normalizeDateTime(startInput);
    const end = normalizeDateTime(endInput);
    setPage(1);
    setKeyword(keywordInput);
    setRange(start && end ? [start, end] : undefined);
  };

  const reset = () => {
    setPage(1);
    setKeywordInput("");
    setKeyword("");
    setStartInput("");
    setEndInput("");
    setRange(undefined);
  };

  const columns: ColumnDef<OnlineUserResp>[] = [
    {
      key: "index",
      title: "序号",
      width: "70px",
      align: "center",
      render: (_row, index) => (page - 1) * PAGE_SIZE + index + 1,
    },
    {
      key: "nickname",
      title: "用户昵称",
      render: (row) =>
        row.nickname ? `${row.nickname}(${row.username})` : row.username,
    },
    { key: "ip", title: "登录 IP", render: (row) => row.ip ?? "—" },
    { key: "address", title: "登录地点", render: (row) => row.address ?? "—" },
    { key: "browser", title: "浏览器", render: (row) => row.browser ?? "—" },
    { key: "os", title: "终端系统", render: (row) => row.os ?? "—" },
    {
      key: "loginTime",
      title: "登录时间",
      width: "180px",
      render: (row) => (
        <span className="whitespace-nowrap">{formatDateTime(row.loginTime)}</span>
      ),
    },
    {
      key: "lastActiveTime",
      title: "最后活跃时间",
      width: "180px",
      render: (row) => (
        <span className="whitespace-nowrap">{formatDateTime(row.lastActiveTime)}</span>
      ),
    },
    {
      key: "actions",
      title: "操作",
      width: "110px",
      align: "right",
      render: (row) =>
        hasPermission("monitor:online:kickout") ? (
          <Button
            size="sm"
            variant="ghost"
            disabled={row.token === currentToken || kickoutMutation.isPending}
            onClick={() => {
              if (window.confirm(`确认强退「${row.nickname ?? row.username}」？`)) {
                kickoutMutation.mutate(row.token);
              }
            }}
            className="!text-red-600 hover:!bg-red-50"
            title={row.token === currentToken ? "不能强退自己" : "强退"}
          >
            <LogOut size={14} />
            强退
          </Button>
        ) : (
          "—"
        ),
    },
  ];

  const data = listQuery.data;

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <div>
        <h2 className="text-xl font-semibold">在线用户</h2>
        <p className="mt-1 text-sm text-zinc-500">
          查看当前在线会话，必要时强制指定用户下线。
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-2 rounded-md border border-zinc-200 bg-white p-3">
        <Input
          className="!w-56"
          placeholder="搜索用户名/昵称"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") applySearch();
          }}
        />
        <Input
          className="!w-52"
          type="datetime-local"
          value={startInput}
          onChange={(e) => setStartInput(e.target.value)}
        />
        <Input
          className="!w-52"
          type="datetime-local"
          value={endInput}
          onChange={(e) => setEndInput(e.target.value)}
        />
        <Button variant="outline" size="sm" onClick={applySearch}>
          <Search size={14} />
          搜索
        </Button>
        <Button variant="outline" size="sm" onClick={reset}>
          <RefreshCw size={14} />
          重置
        </Button>
      </div>

      <DataTable
        columns={columns}
        data={data?.list ?? []}
        rowKey={(row) => row.token}
        loading={listQuery.isLoading}
        stickyHeader
        containerClassName="min-h-0 flex-1 overflow-auto"
        tableClassName="min-w-[1160px]"
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
