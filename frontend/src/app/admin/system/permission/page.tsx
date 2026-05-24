"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { Select } from "~/components/ui/select";
import { HttpError } from "~/lib/api/http";
import {
  createPermission,
  deletePermission,
  getPermissionTree,
  updatePermission,
} from "~/lib/api/permission";
import { usePermission } from "~/lib/hooks/use-permission";
import type { PermissionReq, PermissionResp, PermissionType } from "~/lib/api/types";

interface FormValues {
  code: string;
  name: string;
  type: PermissionType;
  parentId: number;
  sort: number;
  status: number;
  description: string;
}

const DEFAULT_FORM: FormValues = {
  code: "",
  name: "",
  type: 2,
  parentId: 0,
  sort: 1,
  status: 1,
  description: "",
};

/** 权限管理页面：以树形展示，支持新增 / 修改 / 删除（非内置项）。 */
export default function PermissionPage() {
  const { hasPermission } = usePermission();
  const queryClient = useQueryClient();
  const queryKey = ["permission", "tree"] as const;

  const treeQuery = useQuery({ queryKey, queryFn: getPermissionTree });

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<PermissionResp | null>(null);
  const [form, setForm] = useState<FormValues>(DEFAULT_FORM);

  useEffect(() => {
    if (editing) {
      setForm({
        code: editing.code,
        name: editing.name,
        type: editing.type,
        parentId: editing.parentId,
        sort: editing.sort,
        status: editing.status,
        description: editing.description ?? "",
      });
    } else {
      setForm(DEFAULT_FORM);
    }
  }, [editing]);

  const createMutation = useMutation({
    mutationFn: (req: PermissionReq) => createPermission(req),
    onSuccess: () => {
      toast.success("已新增权限");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, req }: { id: number; req: PermissionReq }) => updatePermission(id, req),
    onSuccess: () => {
      toast.success("已更新权限");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });
  const deleteMutation = useMutation({
    mutationFn: (id: number) => deletePermission([id]),
    onSuccess: () => {
      toast.success("已删除");
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  function handleSubmit() {
    const req: PermissionReq = {
      code: form.code,
      name: form.name,
      type: form.type,
      parentId: form.parentId,
      sort: form.sort,
      status: form.status,
      description: form.description || undefined,
    };
    if (editing) updateMutation.mutate({ id: editing.id, req });
    else createMutation.mutate(req);
  }

  /** 递归渲染权限节点。 */
  function renderNode(node: PermissionResp, depth: number): React.ReactNode {
    return (
      <div key={node.id} className="border-b border-zinc-100 last:border-b-0">
        <div
          className="flex items-center justify-between py-2"
          style={{ paddingLeft: depth * 20 + 12, paddingRight: 12 }}
        >
          <div className="flex items-center gap-2 text-sm">
            <span className="font-medium text-zinc-800">{node.name}</span>
            <span className="text-xs text-zinc-400">{node.code}</span>
            {node.type === 1 ? (
              <Badge tone="info">菜单</Badge>
            ) : (
              <Badge tone="default">按钮</Badge>
            )}
            {node.isSystem ? <Badge tone="info">系统</Badge> : null}
            {node.status === 0 ? <Badge tone="danger">禁用</Badge> : null}
          </div>
          <div className="flex gap-2">
            {hasPermission("system:permission:add") ? (
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setEditing(null);
                  setForm({ ...DEFAULT_FORM, parentId: node.id });
                  setOpen(true);
                }}
              >
                <Plus size={14} /> 子项
              </Button>
            ) : null}
            {hasPermission("system:permission:update") ? (
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setEditing(node);
                  setOpen(true);
                }}
              >
                <Pencil size={14} /> 编辑
              </Button>
            ) : null}
            {hasPermission("system:permission:delete") ? (
              <Button
                size="sm"
                variant="ghost"
                className="!text-red-600 hover:!bg-red-50"
                disabled={node.isSystem}
                onClick={() => {
                  if (window.confirm(`确认删除「${node.name}」？`))
                    deleteMutation.mutate(node.id);
                }}
              >
                <Trash2 size={14} /> 删除
              </Button>
            ) : null}
          </div>
        </div>
        {node.children?.map((c) => renderNode(c, depth + 1))}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">权限管理</h2>
          <p className="mt-1 text-sm text-zinc-500">
            维护系统内置 + 自定义权限码（菜单 / 按钮），与角色绑定后控制可见与可点。
          </p>
        </div>
        {hasPermission("system:permission:add") ? (
          <Button
            onClick={() => {
              setEditing(null);
              setForm(DEFAULT_FORM);
              setOpen(true);
            }}
          >
            <Plus size={14} /> 新增权限
          </Button>
        ) : null}
      </div>

      <div className="rounded-md border border-zinc-200 bg-white">
        {treeQuery.isLoading ? (
          <div className="p-4 text-sm text-zinc-500">加载中…</div>
        ) : (treeQuery.data ?? []).length === 0 ? (
          <div className="p-4 text-sm text-zinc-500">暂无权限数据</div>
        ) : (
          treeQuery.data!.map((n) => renderNode(n, 0))
        )}
      </div>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? "编辑权限" : "新增权限"}
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button
              loading={createMutation.isPending || updateMutation.isPending}
              onClick={handleSubmit}
            >
              保存
            </Button>
          </>
        }
      >
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          <FormField label="权限编码" required>
            <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          </FormField>
          <FormField label="名称" required>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </FormField>
          <FormField label="类型" required>
            <Select
              value={String(form.type)}
              onChange={(e) =>
                setForm({ ...form, type: Number(e.target.value) as PermissionType })
              }
            >
              <option value="1">菜单</option>
              <option value="2">按钮</option>
            </Select>
          </FormField>
          <FormField label="父 ID（0 表示顶级）">
            <Input
              type="number"
              value={form.parentId}
              onChange={(e) => setForm({ ...form, parentId: Number(e.target.value) || 0 })}
            />
          </FormField>
          <FormField label="排序">
            <Input
              type="number"
              value={form.sort}
              onChange={(e) => setForm({ ...form, sort: Number(e.target.value) || 0 })}
            />
          </FormField>
          <FormField label="状态">
            <Select
              value={String(form.status)}
              onChange={(e) => setForm({ ...form, status: Number(e.target.value) })}
            >
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </Select>
          </FormField>
          <FormField label="描述" className="md:col-span-2">
            <Input
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </FormField>
        </div>
      </Modal>
    </div>
  );
}
