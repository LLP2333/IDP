"use client";

import { useQueryClient } from "@tanstack/react-query";
import { Upload, X } from "lucide-react";
import { useRef, useState } from "react";
import { toast } from "sonner";

import { Button } from "~/components/ui/button";
import { Modal } from "~/components/ui/modal";
import { Progress } from "~/components/ui/progress";
import { HttpError } from "~/lib/api/http";

import { uploadMultipart } from "./multipart-uploader-core";

/**
 * `MultipartUploader` Props。
 */
export interface MultipartUploaderProps {
  /** 是否可见。 */
  open: boolean;
  /** 当前父目录。 */
  parentPath: string;
  /** 关闭回调。 */
  onClose: () => void;
}

interface RowState {
  file: File;
  percent: number;
  status: "queued" | "uploading" | "done" | "failed" | "canceled";
  message?: string;
  controller?: AbortController;
}

/**
 * 分片上传弹窗组件。
 *
 * 支持：
 * - 多文件入列；
 * - 并发分片上传（默认 3 个分片）；
 * - 进度条 / 单独取消 / 全部取消。
 */
export function MultipartUploaderModal({ open, parentPath, onClose }: MultipartUploaderProps) {
  const queryClient = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);
  const [rows, setRows] = useState<RowState[]>([]);

  const updateRow = (idx: number, patch: Partial<RowState>) => {
    setRows((prev) => {
      const next = [...prev];
      const item = next[idx];
      if (!item) return prev;
      next[idx] = { ...item, ...patch };
      return next;
    });
  };

  const startUpload = async (idx: number) => {
    setRows((prev) => {
      const item = prev[idx];
      if (!item) return prev;
      const controller = new AbortController();
      const next = [...prev];
      next[idx] = { ...item, status: "uploading", percent: 0, controller, message: undefined };
      return next;
    });
    const current = rows[idx];
    if (!current) return;
    const controller = new AbortController();
    try {
      await uploadMultipart({
        file: current.file,
        parentPath,
        signal: controller.signal,
        onProgress: (p) => updateRow(idx, { percent: p }),
      });
      updateRow(idx, { status: "done", percent: 100 });
      void queryClient.invalidateQueries({ queryKey: ["file", "page"] });
      void queryClient.invalidateQueries({ queryKey: ["file", "statistics"] });
      toast.success(`「${current.file.name}」上传完成`);
    } catch (err) {
      const msg = err instanceof HttpError ? err.message : err instanceof Error ? err.message : String(err);
      const aborted = err instanceof DOMException && err.name === "AbortError";
      updateRow(idx, { status: aborted ? "canceled" : "failed", message: msg });
      if (!aborted) toast.error(`「${current.file.name}」上传失败：${msg}`);
    }
  };

  const handleFiles = (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const arr = Array.from(files).map<RowState>((file) => ({
      file,
      percent: 0,
      status: "queued",
    }));
    setRows((prev) => {
      const next = [...prev, ...arr];
      arr.forEach((_, offset) => {
        void startUpload(prev.length + offset);
      });
      return next;
    });
  };

  const cancelRow = (idx: number) => {
    const row = rows[idx];
    if (!row?.controller) return;
    row.controller.abort();
  };

  const clear = () => setRows([]);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="分片上传"
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={clear} disabled={rows.length === 0}>
            清空
          </Button>
          <Button variant="outline" onClick={onClose}>
            关闭
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Button onClick={() => inputRef.current?.click()}>
            <Upload size={14} /> 选择文件
          </Button>
          <span className="text-xs text-zinc-500">默认 5MB 分片，并发 3，支持断点续传</span>
        </div>
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          aria-label="选择文件"
          onChange={(e) => {
            handleFiles(e.target.files);
            e.target.value = "";
          }}
        />
        {rows.length === 0 ? (
          <p className="rounded-md border border-dashed border-zinc-200 px-4 py-6 text-center text-sm text-zinc-400">
            点击 “选择文件” 添加上传任务
          </p>
        ) : (
          <ul className="space-y-2">
            {rows.map((row, idx) => (
              <li key={`${row.file.name}-${idx}`} className="rounded-md border border-zinc-200 p-3 text-sm">
                <div className="flex items-center justify-between gap-2">
                  <span className="truncate" title={row.file.name}>
                    {row.file.name}
                  </span>
                  <span className="shrink-0 text-xs text-zinc-500">
                    {row.status === "done"
                      ? "已完成"
                      : row.status === "failed"
                        ? "失败"
                        : row.status === "canceled"
                          ? "已取消"
                          : row.status === "uploading"
                            ? "上传中"
                            : "等待中"}
                  </span>
                  {row.status === "uploading" ? (
                    <button
                      type="button"
                      onClick={() => cancelRow(idx)}
                      className="text-red-500 hover:text-red-600"
                      aria-label="取消"
                    >
                      <X size={14} />
                    </button>
                  ) : null}
                </div>
                <Progress
                  className="mt-2"
                  value={row.percent}
                  showLabel
                  variant={
                    row.status === "failed" ? "red" : row.status === "done" ? "green" : "blue"
                  }
                />
                {row.message ? (
                  <p className="mt-1 text-xs text-red-500">{row.message}</p>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>
    </Modal>
  );
}
