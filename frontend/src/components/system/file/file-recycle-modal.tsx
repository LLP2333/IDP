"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RotateCcw, Trash2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "~/components/ui/button";
import { Empty } from "~/components/ui/empty";
import { Modal } from "~/components/ui/modal";
import { Pagination } from "~/components/ui/pagination";
import { cleanRecycle, pageRecycle, permanentDelete, restoreFile } from "~/lib/api/file";
import { HttpError } from "~/lib/api/http";

import { formatBytes } from "./file-aside-statistics";
import { FileIcon } from "./file-icon";

/**
 * `FileRecycleModal` Props。
 */
export interface FileRecycleModalProps {
  /** 是否可见。 */
  open: boolean;
  /** 关闭回调。 */
  onClose: () => void;
}

/**
 * 回收站管理模态框。
 *
 * 列出 deleted=1 的文件，支持还原 / 物理删除 / 清空。
 */
export function FileRecycleModal({ open, onClose }: FileRecycleModalProps) {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);

  const listQuery = useQuery({
    queryKey: ["file", "recycle", { page }],
    queryFn: () => pageRecycle({ page, size: 10 }),
    enabled: open,
    staleTime: 0,
  });

  const restoreMutation = useMutation({
    mutationFn: (id: number) => restoreFile(id),
    onSuccess: () => {
      toast.success("已还原");
      void queryClient.invalidateQueries({ queryKey: ["file", "recycle"] });
      void queryClient.invalidateQueries({ queryKey: ["file", "page"] });
      void queryClient.invalidateQueries({ queryKey: ["file", "statistics"] });
    },
    onError: (err: unknown) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => permanentDelete(id),
    onSuccess: () => {
      toast.success("已永久删除");
      void queryClient.invalidateQueries({ queryKey: ["file", "recycle"] });
      void queryClient.invalidateQueries({ queryKey: ["file", "statistics"] });
    },
    onError: (err: unknown) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const cleanMutation = useMutation({
    mutationFn: () => cleanRecycle(),
    onSuccess: () => {
      toast.success("已清空回收站");
      void queryClient.invalidateQueries({ queryKey: ["file", "recycle"] });
      void queryClient.invalidateQueries({ queryKey: ["file", "statistics"] });
    },
    onError: (err: unknown) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const data = listQuery.data;
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="回收站"
      size="lg"
      footer={
        <>
          <Button
            variant="danger"
            onClick={() => {
              if (window.confirm("确定清空回收站？此操作不可撤销。")) {
                cleanMutation.mutate();
              }
            }}
            loading={cleanMutation.isPending}
            disabled={(data?.total ?? 0) === 0}
          >
            清空回收站
          </Button>
          <Button variant="outline" onClick={onClose}>
            关闭
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        {listQuery.isLoading ? (
          <p className="py-6 text-center text-sm text-zinc-400">加载中…</p>
        ) : (data?.list.length ?? 0) === 0 ? (
          <Empty title="回收站为空" description="删除文件时未启用回收站则不会出现在这里" />
        ) : (
          <div className="rounded-md border border-zinc-200">
            <table className="w-full text-sm">
              <thead className="bg-zinc-50 text-left text-xs text-zinc-500">
                <tr>
                  <th className="px-3 py-2">名称</th>
                  <th className="w-32 px-3 py-2">大小</th>
                  <th className="w-44 px-3 py-2">删除时间</th>
                  <th className="w-44 px-3 py-2">操作</th>
                </tr>
              </thead>
              <tbody>
                {data!.list.map((item) => (
                  <tr key={item.id} className="border-t border-zinc-100">
                    <td className="px-3 py-2">
                      <div className="flex items-center gap-2">
                        <FileIcon
                          isDir={item.type === 0}
                          extension={item.extension}
                          thumbnailUrl={item.thumbnailUrl}
                          size={20}
                        />
                        <span className="truncate">{item.originalName}</span>
                      </div>
                    </td>
                    <td className="px-3 py-2 text-zinc-500">
                      {item.type === 0 ? "—" : formatBytes(item.size)}
                    </td>
                    <td className="px-3 py-2 text-zinc-500">{item.deletedAt ?? "—"}</td>
                    <td className="px-3 py-2">
                      <div className="flex gap-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => restoreMutation.mutate(item.id)}
                        >
                          <RotateCcw size={14} /> 还原
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => {
                            if (window.confirm(`确认永久删除「${item.originalName}」？`)) {
                              deleteMutation.mutate(item.id);
                            }
                          }}
                          className="!text-red-600 hover:!bg-red-50"
                        >
                          <Trash2 size={14} /> 永久删除
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data ? (
          <Pagination
            page={data.page}
            size={data.size}
            total={data.total}
            onPageChange={setPage}
          />
        ) : null}
      </div>
    </Modal>
  );
}
