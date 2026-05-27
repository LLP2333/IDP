"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { KeySquare, Pencil, Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { MenuTree } from "~/components/system/menu-tree";
import { RoleForm, type RoleFormValues } from "~/components/system/role-form";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { DataTable, Pagination, type ColumnDef } from "~/components/ui/data-table";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { Select } from "~/components/ui/select";
import {
  assignRoleMenu,
  getMenuTree,
  getRoleMenu,
} from "~/lib/api/menu";
import { addRole, deleteRole, listRole, updateRole } from "~/lib/api/role";
import { HttpError } from "~/lib/api/http";
import { usePermission } from "~/lib/hooks/use-permission";
import type { RoleResp } from "~/lib/api/types";

const FORM_ID = "role-form";

/**
 * 角色管理页：表格 + 搜索栏 + 新增/编辑 Modal。
 *
 * 数据流均通过 `useQuery / useMutation` 管理，并在变更后通过
 * `queryClient.invalidateQueries(["role", "list"])` 刷新列表缓存。
 */
export default function RolePage() {
  const queryClient = useQueryClient();
  const { hasPermission } = usePermission();

  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [status, setStatus] = useState<number | "">("");

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<RoleResp | null>(null);

  const [assignRole, setAssignRole] = useState<RoleResp | null>(null);
  const [assignIds, setAssignIds] = useState<number[]>([]);

  const treeQuery = useQuery({
    queryKey: ["menu", "tree"],
    queryFn: () => getMenuTree(),
    enabled: !!assignRole,
  });
  const roleMenuQuery = useQuery({
    queryKey: ["role", "menu", assignRole?.id],
    queryFn: () => (assignRole ? getRoleMenu(assignRole.id) : Promise.resolve([])),
    enabled: !!assignRole,
  });
  useEffect(() => {
    if (roleMenuQuery.data) setAssignIds(roleMenuQuery.data);
  }, [roleMenuQuery.data]);

  const assignMutation = useMutation({
    mutationFn: (ids: number[]) => {
      if (!assignRole) throw new Error("未选择角色");
      return assignRoleMenu(assignRole.id, ids);
    },
    onSuccess: () => {
      toast.success("已保存角色菜单");
      setAssignRole(null);
      void queryClient.invalidateQueries({ queryKey: ["role", "menu"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

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

  /**
   * 表单提交分发：根据是否存在 `editing` 走新增或更新接口。
   */
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
      width: "280px",
      render: (row) => (
        <div className="flex flex-nowrap items-center justify-end gap-1 whitespace-nowrap">
          {hasPermission("system:role:assignPermission") ? (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                setAssignRole(row);
                setAssignIds([]);
              }}
            >
              <KeySquare size={14} /> 分配菜单
            </Button>
          ) : null}
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
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
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
          variant="outline"
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
        stickyHeader
        containerClassName="min-h-0 flex-1 overflow-auto"
        tableClassName="min-w-[900px]"
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

      <Modal
        open={!!assignRole}
        onClose={() => setAssignRole(null)}
        title={`分配菜单 - ${assignRole?.name ?? ""}`}
        footer={
          <>
            <Button variant="outline" onClick={() => setAssignRole(null)}>
              取消
            </Button>
            <Button
              loading={assignMutation.isPending}
              onClick={() => assignMutation.mutate(assignIds)}
            >
              保存
            </Button>
          </>
        }
      >
        {treeQuery.isLoading || roleMenuQuery.isLoading ? (
          <div className="text-sm text-zinc-500">加载中…</div>
        ) : (
          <MenuTree
            data={treeQuery.data ?? []}
            value={assignIds}
            onChange={setAssignIds}
          />
        )}
      </Modal>
    </div>
  );
}
