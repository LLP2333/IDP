"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Eye, Pencil, Plus, Trash2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { DictBadge } from "~/components/system/dict-badge";
import { NoticeDetailDrawer } from "~/components/system/notice-detail-drawer";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { DataTable, Pagination, type ColumnDef } from "~/components/ui/data-table";
import { Input } from "~/components/ui/input";
import { Select } from "~/components/ui/select";
import { HttpError } from "~/lib/api/http";
import { deleteNotice, getNotice, listNotice } from "~/lib/api/notice";
import type { NoticeDetailResp, NoticeResp, NoticeStatus } from "~/lib/api/types";
import { useDict } from "~/lib/hooks/use-dict";
import { usePermission } from "~/lib/hooks/use-permission";

/**
 * 通知公告管理列表页。
 *
 * <p>支持按标题模糊 + 分类下拉 + 状态下拉筛选；新增 / 编辑跳到独立子页，
 * 预览复用右侧抽屉组件，避免离开列表。</p>
 */
export default function NoticeListPage() {
  const queryClient = useQueryClient();
  const router = useRouter();
  const { hasPermission } = usePermission();

  const [page, setPage] = useState(1);
  const [title, setTitle] = useState("");
  const [titleInput, setTitleInput] = useState("");
  const [type, setType] = useState<string>("");
  const [status, setStatus] = useState<NoticeStatus | "">("");

  const typeDict = useDict("notice_type");
  const statusDict = useDict("notice_status_enum");
  const scopeDict = useDict("notice_scope_enum");

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerData, setDrawerData] = useState<NoticeDetailResp | null>(null);

  const listQuery = useQuery({
    queryKey: ["notice", "list", { page, title, type, status }],
    queryFn: () =>
      listNotice({
        page,
        size: 10,
        title: title || undefined,
        type: type || undefined,
        status: status === "" ? undefined : status,
      }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteNotice([id]),
    onSuccess: () => {
      toast.success("已删除");
      void queryClient.invalidateQueries({ queryKey: ["notice", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const openDetail = async (id: number) => {
    try {
      const detail = await getNotice(id);
      setDrawerData(detail);
      setDrawerOpen(true);
    } catch (err) {
      toast.error(err instanceof HttpError ? err.message : "加载详情失败");
    }
  };

  const columns: ColumnDef<NoticeResp>[] = [
    { key: "id", title: "ID", width: "64px" },
    {
      key: "title",
      title: "标题",
      render: (row) => (
        <div className="flex items-center gap-2">
          {row.isTop ? <Badge tone="warning">置顶</Badge> : null}
          <button
            type="button"
            className="text-blue-600 hover:underline"
            onClick={() => openDetail(row.id)}
          >
            {row.title}
          </button>
        </div>
      ),
    },
    {
      key: "type",
      title: "分类",
      width: "100px",
      render: (row) => <DictBadge items={typeDict.items} value={row.type} />,
    },
    {
      key: "noticeScope",
      title: "通知范围",
      width: "110px",
      render: (row) => <DictBadge items={scopeDict.items} value={row.noticeScope} />,
    },
    {
      key: "status",
      title: "状态",
      width: "100px",
      render: (row) => <DictBadge items={statusDict.items} value={row.status} />,
    },
    {
      key: "publishTime",
      title: "发布时间",
      width: "180px",
      render: (row) => row.publishTime ?? "—",
    },
    {
      key: "createUserString",
      title: "发布人",
      width: "120px",
      render: (row) => row.createUserString ?? "—",
    },
    {
      key: "actions",
      title: "操作",
      width: "260px",
      align: "right",
      render: (row) => (
        <div className="flex flex-nowrap items-center justify-end gap-1 whitespace-nowrap">
          <Button size="sm" variant="ghost" onClick={() => openDetail(row.id)}>
            <Eye size={14} /> 预览
          </Button>
          {hasPermission("system:notice:update") ? (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => router.push(`/admin/system/notice/add?id=${row.id}`)}
            >
              <Pencil size={14} /> 编辑
            </Button>
          ) : null}
          {hasPermission("system:notice:delete") ? (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                if (window.confirm(`确认删除公告「${row.title}」？`)) {
                  deleteMutation.mutate(row.id);
                }
              }}
              className="!text-red-600 hover:!bg-red-50"
            >
              <Trash2 size={14} /> 删除
            </Button>
          ) : null}
        </div>
      ),
    },
  ];

  const data = listQuery.data;

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">通知公告</h2>
          <p className="mt-1 text-sm text-zinc-500">
            发布系统公告，可选所有人或指定用户，支持立即 / 定时发布、登录弹窗。
          </p>
        </div>
        {hasPermission("system:notice:add") ? (
          <Button onClick={() => router.push("/admin/system/notice/add")}>
            <Plus size={14} />
            新增公告
          </Button>
        ) : null}
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
          value={type}
          onChange={(e) => {
            setType(e.target.value);
            setPage(1);
          }}
        >
          <option value="">全部分类</option>
          {typeDict.items.map((it) => (
            <option key={it.id} value={it.value}>
              {it.label}
            </option>
          ))}
        </Select>
        <Select
          className="!w-32"
          value={status}
          onChange={(e) => {
            const v = e.target.value;
            setStatus(v === "" ? "" : (Number(v) as NoticeStatus));
            setPage(1);
          }}
        >
          <option value="">全部状态</option>
          {statusDict.items.map((it) => (
            <option key={it.id} value={it.value}>
              {it.label}
            </option>
          ))}
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
          variant="outline"
          size="sm"
          onClick={() => {
            setTitleInput("");
            setTitle("");
            setType("");
            setStatus("");
            setPage(1);
          }}
        >
          重置
        </Button>
      </div>

      <DataTable<NoticeResp>
        columns={columns}
        data={data?.list ?? []}
        rowKey={(row) => row.id}
        loading={listQuery.isLoading}
        stickyHeader
        containerClassName="min-h-0 flex-1 overflow-auto"
        tableClassName="min-w-[1040px]"
      />

      {data ? (
        <Pagination
          page={data.page}
          size={data.size}
          total={data.total}
          onPageChange={setPage}
        />
      ) : null}

      <NoticeDetailDrawer
        open={drawerOpen}
        data={drawerData}
        onClose={() => setDrawerOpen(false)}
      />
    </div>
  );
}
