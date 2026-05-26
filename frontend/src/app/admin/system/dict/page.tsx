"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ListTree, Pencil, Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { DictBadge } from "~/components/system/dict-badge";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { DataTable, type ColumnDef } from "~/components/ui/data-table";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { Select } from "~/components/ui/select";
import { HttpError } from "~/lib/api/http";
import {
  addDict,
  addDictItem,
  deleteDict,
  deleteDictItem,
  listDict,
  listDictItem,
  updateDict,
  updateDictItem,
} from "~/lib/api/dict";
import { dictQueryKey } from "~/lib/hooks/use-dict";
import { usePermission } from "~/lib/hooks/use-permission";
import type { DictItemResp, DictResp } from "~/lib/api/types";

const dictSchema = z.object({
  name: z.string().min(1, "请输入字典名称").max(64),
  code: z
    .string()
    .min(1, "请输入字典编码")
    .regex(/^[a-zA-Z][a-zA-Z0-9_]{1,63}$/, "必须以字母开头，仅可包含字母、数字、下划线"),
  description: z.string().max(255).optional().or(z.literal("")),
});

const itemSchema = z.object({
  label: z.string().min(1, "请输入展示文案").max(64),
  value: z.string().min(1, "请输入存储值").max(64),
  color: z.string().max(32).optional().or(z.literal("")),
  sort: z.number({ message: "排序必须为数字" }).int().min(0).max(9999),
  status: z.union([z.literal(0), z.literal(1)]),
});

type DictForm = z.infer<typeof dictSchema>;
type ItemForm = z.infer<typeof itemSchema>;

const COLOR_OPTIONS = ["primary", "success", "warning", "danger", "info"];

/**
 * 把空字符串归一为 {@code undefined}，避免后端把 "" 当作有效值落库。
 */
function emptyToUndefined(v: string | undefined): string | undefined {
  if (v === undefined) return undefined;
  return v.trim().length === 0 ? undefined : v;
}

/**
 * 字典管理页：左侧字典列表，右侧选中字典的明细管理。
 */
export default function DictPage() {
  const queryClient = useQueryClient();
  const { hasPermission } = usePermission();

  const [selectedDictId, setSelectedDictId] = useState<number | null>(null);

  const [dictOpen, setDictOpen] = useState(false);
  const [editingDict, setEditingDict] = useState<DictResp | null>(null);
  const [itemOpen, setItemOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<DictItemResp | null>(null);

  const dictListQuery = useQuery({
    queryKey: ["dict", "list"],
    queryFn: listDict,
  });

  const itemListQuery = useQuery({
    queryKey: ["dict", "items", selectedDictId],
    queryFn: () => listDictItem(selectedDictId!),
    enabled: !!selectedDictId,
  });

  const selectedDict = dictListQuery.data?.find((d) => d.id === selectedDictId) ?? null;

  const dictForm = useForm<DictForm>({
    resolver: zodResolver(dictSchema),
    defaultValues: { name: "", code: "", description: "" },
  });
  const itemForm = useForm<ItemForm>({
    resolver: zodResolver(itemSchema),
    defaultValues: { label: "", value: "", color: "", sort: 999, status: 1 },
  });

  useEffect(() => {
    if (dictOpen) {
      if (editingDict) {
        dictForm.reset({
          name: editingDict.name,
          code: editingDict.code,
          description: editingDict.description ?? "",
        });
      } else {
        dictForm.reset({ name: "", code: "", description: "" });
      }
    }
  }, [dictOpen, editingDict, dictForm]);

  useEffect(() => {
    if (itemOpen) {
      if (editingItem) {
        itemForm.reset({
          label: editingItem.label,
          value: editingItem.value,
          color: editingItem.color ?? "",
          sort: editingItem.sort,
          status: editingItem.status === 0 ? 0 : 1,
        });
      } else {
        itemForm.reset({ label: "", value: "", color: "", sort: 999, status: 1 });
      }
    }
  }, [itemOpen, editingItem, itemForm]);

  const dictCreate = useMutation({
    mutationFn: (values: DictForm) =>
      addDict({ ...values, description: emptyToUndefined(values.description) }),
    onSuccess: () => {
      toast.success("已新增字典");
      setDictOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["dict", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });
  const dictUpdate = useMutation({
    mutationFn: ({ id, values }: { id: number; values: DictForm }) =>
      updateDict(id, { ...values, description: emptyToUndefined(values.description) }),
    onSuccess: (_data, vars) => {
      toast.success("已更新字典");
      setDictOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["dict", "list"] });
      const oldCode = editingDict?.code;
      if (oldCode) void queryClient.invalidateQueries({ queryKey: dictQueryKey(oldCode) });
      void queryClient.invalidateQueries({ queryKey: dictQueryKey(vars.values.code) });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });
  const dictDelete = useMutation({
    mutationFn: (id: number) => deleteDict([id]),
    onSuccess: () => {
      toast.success("已删除");
      setSelectedDictId(null);
      void queryClient.invalidateQueries({ queryKey: ["dict", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const itemCreate = useMutation({
    mutationFn: (values: ItemForm) =>
      addDictItem(selectedDictId!, { ...values, color: emptyToUndefined(values.color) }),
    onSuccess: () => {
      toast.success("已新增字典明细");
      setItemOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["dict", "items", selectedDictId] });
      if (selectedDict) void queryClient.invalidateQueries({ queryKey: dictQueryKey(selectedDict.code) });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });
  const itemUpdate = useMutation({
    mutationFn: ({ id, values }: { id: number; values: ItemForm }) =>
      updateDictItem(id, { ...values, color: emptyToUndefined(values.color) }),
    onSuccess: () => {
      toast.success("已更新字典明细");
      setItemOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["dict", "items", selectedDictId] });
      if (selectedDict) void queryClient.invalidateQueries({ queryKey: dictQueryKey(selectedDict.code) });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });
  const itemDelete = useMutation({
    mutationFn: (id: number) => deleteDictItem([id]),
    onSuccess: () => {
      toast.success("已删除");
      void queryClient.invalidateQueries({ queryKey: ["dict", "items", selectedDictId] });
      if (selectedDict) void queryClient.invalidateQueries({ queryKey: dictQueryKey(selectedDict.code) });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const itemColumns: ColumnDef<DictItemResp>[] = [
    { key: "id", title: "ID", width: "60px" },
    { key: "label", title: "展示文案" },
    { key: "value", title: "存储值", width: "120px" },
    {
      key: "color",
      title: "颜色",
      width: "100px",
      render: (row) =>
        row.color ? <DictBadge items={[row]} value={row.value} /> : <span>—</span>,
    },
    { key: "sort", title: "排序", width: "80px" },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (row) =>
        row.status === 1 ? <Badge tone="success">启用</Badge> : <Badge tone="danger">禁用</Badge>,
    },
    {
      key: "isSystem",
      title: "类型",
      width: "80px",
      render: (row) =>
        row.isSystem ? <Badge tone="info">系统</Badge> : <Badge tone="default">自定义</Badge>,
    },
    {
      key: "actions",
      title: "操作",
      width: "180px",
      align: "right",
      render: (row) => (
        <div className="flex items-center justify-end gap-1 whitespace-nowrap">
          {hasPermission("system:dict:update") ? (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                setEditingItem(row);
                setItemOpen(true);
              }}
            >
              <Pencil size={14} /> 编辑
            </Button>
          ) : null}
          {hasPermission("system:dict:delete") ? (
            <Button
              size="sm"
              variant="ghost"
              disabled={row.isSystem}
              onClick={() => {
                if (window.confirm(`确认删除「${row.label}」？`)) {
                  itemDelete.mutate(row.id);
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

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">字典管理</h2>
          <p className="mt-1 text-sm text-zinc-500">
            维护各业务下拉的选项，左侧选择字典分类，右侧管理明细。
          </p>
        </div>
        {hasPermission("system:dict:add") ? (
          <Button
            onClick={() => {
              setEditingDict(null);
              setDictOpen(true);
            }}
          >
            <Plus size={14} /> 新增字典
          </Button>
        ) : null}
      </div>

      <div className="grid min-h-0 flex-1 grid-cols-1 gap-4 lg:grid-cols-[280px_1fr]">
        <div className="flex min-h-0 flex-col rounded-md border border-zinc-200 bg-white">
          <div className="border-b border-zinc-200 px-3 py-2 text-sm font-medium text-zinc-600">
            字典列表
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto">
            {dictListQuery.isLoading ? (
              <div className="px-3 py-4 text-sm text-zinc-400">加载中…</div>
            ) : (dictListQuery.data ?? []).length === 0 ? (
              <div className="px-3 py-4 text-sm text-zinc-400">暂无字典</div>
            ) : (
              (dictListQuery.data ?? []).map((d) => (
                <div
                  key={d.id}
                  className={
                    "flex cursor-pointer items-center justify-between border-b border-zinc-100 px-3 py-2 text-sm hover:bg-zinc-50 " +
                    (selectedDictId === d.id ? "bg-blue-50 text-blue-700" : "text-zinc-700")
                  }
                  onClick={() => setSelectedDictId(d.id)}
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-1 truncate font-medium">
                      <ListTree size={14} />
                      {d.name}
                    </div>
                    <div className="text-xs text-zinc-400">{d.code}</div>
                  </div>
                  {d.isSystem ? <Badge tone="info">系统</Badge> : null}
                </div>
              ))
            )}
          </div>
        </div>

        <div className="flex min-h-0 flex-col rounded-md border border-zinc-200 bg-white">
          <div className="flex items-center justify-between border-b border-zinc-200 px-3 py-2">
            <div className="text-sm font-medium text-zinc-700">
              {selectedDict ? `明细：${selectedDict.name}（${selectedDict.code}）` : "请选择左侧字典"}
            </div>
            <div className="flex items-center gap-1">
              {selectedDict && hasPermission("system:dict:update") ? (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setEditingDict(selectedDict);
                    setDictOpen(true);
                  }}
                >
                  <Pencil size={14} /> 编辑字典
                </Button>
              ) : null}
              {selectedDict && hasPermission("system:dict:delete") ? (
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={selectedDict.isSystem}
                  className="!text-red-600 hover:!bg-red-50"
                  onClick={() => {
                    if (window.confirm(`确认删除字典「${selectedDict.name}」及其全部明细？`)) {
                      dictDelete.mutate(selectedDict.id);
                    }
                  }}
                >
                  <Trash2 size={14} /> 删除字典
                </Button>
              ) : null}
              {selectedDict && hasPermission("system:dict:add") ? (
                <Button
                  size="sm"
                  onClick={() => {
                    setEditingItem(null);
                    setItemOpen(true);
                  }}
                >
                  <Plus size={14} /> 新增明细
                </Button>
              ) : null}
            </div>
          </div>
          <div className="flex min-h-0 flex-1 flex-col p-3">
            <DataTable<DictItemResp>
              columns={itemColumns}
              data={itemListQuery.data ?? []}
              rowKey={(row) => row.id}
              loading={!!selectedDictId && itemListQuery.isLoading}
              empty={selectedDictId ? "暂无明细" : "请选择左侧字典"}
              stickyHeader
              containerClassName="min-h-0 flex-1 overflow-auto"
              tableClassName="min-w-[760px]"
            />
          </div>
        </div>
      </div>

      <Modal
        open={dictOpen}
        onClose={() => setDictOpen(false)}
        title={editingDict ? "编辑字典" : "新增字典"}
        footer={
          <>
            <Button variant="outline" onClick={() => setDictOpen(false)}>
              取消
            </Button>
            <Button
              loading={dictCreate.isPending || dictUpdate.isPending}
              onClick={dictForm.handleSubmit((values) =>
                editingDict ? dictUpdate.mutate({ id: editingDict.id, values }) : dictCreate.mutate(values),
              )}
            >
              保存
            </Button>
          </>
        }
      >
        <form className="grid grid-cols-1 gap-3">
          <FormField label="字典名称" required error={dictForm.formState.errors.name?.message}>
            <Input {...dictForm.register("name")} invalid={!!dictForm.formState.errors.name} />
          </FormField>
          <FormField label="字典编码" required error={dictForm.formState.errors.code?.message}>
            <Input
              {...dictForm.register("code")}
              disabled={!!editingDict?.isSystem}
              invalid={!!dictForm.formState.errors.code}
            />
          </FormField>
          <FormField label="描述" error={dictForm.formState.errors.description?.message}>
            <Input
              {...dictForm.register("description")}
              invalid={!!dictForm.formState.errors.description}
            />
          </FormField>
        </form>
      </Modal>

      <Modal
        open={itemOpen}
        onClose={() => setItemOpen(false)}
        title={editingItem ? "编辑字典明细" : "新增字典明细"}
        footer={
          <>
            <Button variant="outline" onClick={() => setItemOpen(false)}>
              取消
            </Button>
            <Button
              loading={itemCreate.isPending || itemUpdate.isPending}
              onClick={itemForm.handleSubmit((values) =>
                editingItem
                  ? itemUpdate.mutate({ id: editingItem.id, values })
                  : itemCreate.mutate(values),
              )}
            >
              保存
            </Button>
          </>
        }
      >
        <form className="grid grid-cols-2 gap-3">
          <FormField label="展示文案" required error={itemForm.formState.errors.label?.message}>
            <Input {...itemForm.register("label")} invalid={!!itemForm.formState.errors.label} />
          </FormField>
          <FormField label="存储值" required error={itemForm.formState.errors.value?.message}>
            <Input
              {...itemForm.register("value")}
              disabled={!!editingItem?.isSystem}
              invalid={!!itemForm.formState.errors.value}
            />
          </FormField>
          <FormField label="颜色" error={itemForm.formState.errors.color?.message}>
            <Select {...itemForm.register("color")} invalid={!!itemForm.formState.errors.color}>
              <option value="">默认</option>
              {COLOR_OPTIONS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="排序" error={itemForm.formState.errors.sort?.message}>
            <Input
              type="number"
              {...itemForm.register("sort", { valueAsNumber: true })}
              invalid={!!itemForm.formState.errors.sort}
            />
          </FormField>
          <FormField label="状态" error={itemForm.formState.errors.status?.message}>
            <Select {...itemForm.register("status", { valueAsNumber: true })}>
              <option value={1}>启用</option>
              <option value={0}>禁用</option>
            </Select>
          </FormField>
        </form>
      </Modal>
    </div>
  );
}
