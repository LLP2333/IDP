"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { RoleForm, type RoleFormValues } from "~/components/system/role-form";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { DataTable, Pagination, type ColumnDef } from "~/components/ui/data-table";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { Select } from "~/components/ui/select";
import { addRole, deleteRole, listRole, updateRole } from "~/lib/api/role";
import { HttpError } from "~/lib/api/http";
import type { RoleResp } from "~/lib/api/types";

const FORM_ID = "role-form";

export default function RolePage() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [status, setStatus] = useState<number | "">("");

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<RoleResp | null>(null);

  const listQuery = useQuery({
    queryKey: ["role", "list", { page, keyword, status }],
    queryFn: () =>
      listRole({
        page,
        size: 10,
        keyword: keyword || undefined,
        status: status === "" ? undefined : status,
      }),
  });

  const createMutation = useMutation({
    mutationFn: (values: RoleFormValues) => addRole(values),
    onSuccess: () => {
      toast.success("已新增角色");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["role", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, values }: { id: number; values: RoleFormValues }) =>
      updateRole(id, values),
    onSuccess: () => {
      toast.success("已更新角色");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["role", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteRole([id]),
    onSuccess: () => {
      toast.success("已删除");
      void queryClient.invalidateQueries({ queryKey: ["role", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const handleSubmit = (values: RoleFormValues) => {
    if (editing) {
      updateMutation.mutate({ id: editing.id, values });
    } else {
      createMutation.mutate(values);
    }
  };

  const columns: ColumnDef<RoleResp>[] = [
    { key: "id", title: "ID", width: "60px" },
    { key: "name", title: "名称" },
    { key: "code", title: "编码" },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (row) =>
        row.status === 1 ? (
          <Badge tone="success">启用</Badge>
        ) : (
          <Badge tone="danger">禁用</Badge>
        ),
    },
    { key: "sort", title: "排序", width: "70px" },
    {
      key: "isSystem",
      title: "类型",
      width: "80px",
      render: (row) =>
        row.isSystem ? (
          <Badge tone="info">系统</Badge>
        ) : (
          <Badge tone="default">自定义</Badge>
        ),
    },
    {
      key: "description",
      title: "描述",
      render: (row) => row.description ?? "—",
    },
    {
      key: "actions",
      title: "操作",
      width: "180px",
      render: (row) => (
        <div className="flex justify-end gap-2">
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              setEditing(row);
              setOpen(true);
            }}
          >
            <Pencil size={14} /> 编辑
          </Button>
          <Button
            size="sm"
            variant="ghost"
            disabled={row.isSystem}
            onClick={() => {
              if (window.confirm(`确认删除角色「${row.name}」？`)) {
                deleteMutation.mutate(row.id);
              }
            }}
            className="!text-red-600 hover:!bg-red-50"
          >
            <Trash2 size={14} /> 删除
          </Button>
        </div>
      ),
      align: "right",
    },
  ];

  const data = listQuery.data;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">角色管理</h2>
          <p className="mt-1 text-sm text-zinc-500">
            维护系统角色，作为 RBAC 权限分配的基础。
          </p>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setOpen(true);
          }}
        >
          <Plus size={14} />
          新增角色
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-2 rounded-md border border-zinc-200 bg-white p-3">
        <Input
          className="!w-56"
          placeholder="按名称或编码搜索"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              setPage(1);
              setKeyword(keywordInput);
            }
          }}
        />
        <Select
          className="!w-32"
          value={status}
          onChange={(e) => {
            const v = e.target.value;
            setStatus(v === "" ? "" : Number(v));
            setPage(1);
          }}
        >
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </Select>
        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            setPage(1);
            setKeyword(keywordInput);
          }}
        >
          搜索
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => {
            setKeyword("");
            setKeywordInput("");
            setStatus("");
            setPage(1);
          }}
        >
          重置
        </Button>
      </div>

      <DataTable<RoleResp>
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

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? "编辑角色" : "新增角色"}
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button
              type="submit"
              form={FORM_ID}
              loading={createMutation.isPending || updateMutation.isPending}
            >
              保存
            </Button>
          </>
        }
      >
        <RoleForm formId={FORM_ID} initial={editing} onSubmit={handleSubmit} />
      </Modal>
    </div>
  );
}
