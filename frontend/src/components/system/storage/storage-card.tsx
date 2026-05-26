"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Check, HardDrive, Pencil, Server, Star, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { deleteStorage, setDefaultStorage, updateStorageStatus } from "~/lib/api/storage";
import { HttpError } from "~/lib/api/http";
import type { StorageResp } from "~/lib/api/types";
import { usePermission } from "~/lib/hooks/use-permission";
import { cn } from "~/lib/utils";

/**
 * `StorageCard` Props。
 */
export interface StorageCardProps {
  /** 存储信息。 */
  storage: StorageResp;
  /** 点击编辑回调。 */
  onEdit: (storage: StorageResp) => void;
}

/**
 * 单个存储引擎卡片。
 *
 * 展示：图标 + 名称 + 类型 + 默认/状态徽章 + 操作（设为默认 / 编辑 / 删除 / 切换状态）。
 */
export function StorageCard({ storage, onEdit }: StorageCardProps) {
  const queryClient = useQueryClient();
  const { hasPermission } = usePermission();

  const setDefaultMutation = useMutation({
    mutationFn: () => setDefaultStorage(storage.id),
    onSuccess: () => {
      toast.success("已设为默认");
      void queryClient.invalidateQueries({ queryKey: ["storage", "list"] });
    },
    onError: (err: unknown) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteStorage([storage.id]),
    onSuccess: () => {
      toast.success("已删除");
      void queryClient.invalidateQueries({ queryKey: ["storage", "list"] });
    },
    onError: (err: unknown) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const toggleStatusMutation = useMutation({
    mutationFn: (next: 1 | 2) => updateStorageStatus(storage.id, { status: next }),
    onSuccess: () => {
      toast.success("已更新");
      void queryClient.invalidateQueries({ queryKey: ["storage", "list"] });
    },
    onError: (err: unknown) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const Icon = storage.type === 1 ? HardDrive : Server;
  const typeLabel = storage.type === 1 ? "本地存储" : "对象存储";

  return (
    <div
      className={cn(
        "flex flex-col gap-3 rounded-lg border bg-white p-4 transition-colors",
        storage.isDefault ? "border-blue-300 ring-1 ring-blue-100" : "border-zinc-200 hover:border-blue-300",
      )}
    >
      <div className="flex items-center gap-3">
        <div
          className={cn(
            "flex h-10 w-10 items-center justify-center rounded-md",
            storage.type === 1 ? "bg-amber-50 text-amber-500" : "bg-blue-50 text-blue-600",
          )}
        >
          <Icon size={20} />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="truncate font-medium text-zinc-900" title={storage.name}>
              {storage.name}
            </span>
            {storage.isDefault ? <Badge tone="info">默认</Badge> : null}
            {storage.status === 1 ? (
              <Badge tone="success">启用</Badge>
            ) : (
              <Badge tone="danger">禁用</Badge>
            )}
          </div>
          <div className="text-xs text-zinc-500">
            <span className="mr-2">{typeLabel}</span>
            <span className="font-mono">{storage.code}</span>
          </div>
        </div>
      </div>

      <dl className="space-y-1 text-xs text-zinc-500">
        <DescRow label={storage.type === 1 ? "本地路径" : "Bucket"} value={storage.bucketName ?? "—"} />
        {storage.type === 2 ? <DescRow label="Endpoint" value={storage.endpoint ?? "—"} /> : null}
        <DescRow label="访问域名" value={storage.domain ?? "—"} />
        {storage.recycleBinEnabled ? (
          <DescRow label="回收站" value={storage.recycleBinPath ?? "—"} />
        ) : null}
        {storage.description ? <DescRow label="描述" value={storage.description} /> : null}
      </dl>

      <div className="mt-auto flex flex-wrap items-center gap-1 border-t border-zinc-100 pt-2">
        {hasPermission("system:storage:setDefault") && !storage.isDefault ? (
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setDefaultMutation.mutate()}
            disabled={setDefaultMutation.isPending}
          >
            <Star size={14} /> 设为默认
          </Button>
        ) : null}
        {hasPermission("system:storage:updateStatus") ? (
          <Button
            size="sm"
            variant="ghost"
            onClick={() => toggleStatusMutation.mutate(storage.status === 1 ? 2 : 1)}
            disabled={toggleStatusMutation.isPending}
          >
            <Check size={14} /> {storage.status === 1 ? "禁用" : "启用"}
          </Button>
        ) : null}
        {hasPermission("system:storage:update") ? (
          <Button size="sm" variant="ghost" onClick={() => onEdit(storage)}>
            <Pencil size={14} /> 编辑
          </Button>
        ) : null}
        {hasPermission("system:storage:delete") ? (
          <Button
            size="sm"
            variant="ghost"
            disabled={storage.isDefault || deleteMutation.isPending}
            className="!text-red-600 hover:!bg-red-50"
            onClick={() => {
              if (window.confirm(`确认删除存储「${storage.name}」？`)) {
                deleteMutation.mutate();
              }
            }}
          >
            <Trash2 size={14} /> 删除
          </Button>
        ) : null}
      </div>
    </div>
  );
}

function DescRow({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="flex items-baseline justify-between gap-2">
      <dt className="shrink-0">{label}</dt>
      <dd className="truncate text-zinc-700" title={String(value)}>
        {value}
      </dd>
    </div>
  );
}
