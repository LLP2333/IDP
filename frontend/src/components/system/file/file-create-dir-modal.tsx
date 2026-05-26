"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { createDir } from "~/lib/api/file";
import { HttpError } from "~/lib/api/http";

/**
 * `FileCreateDirModal` Props。
 */
export interface FileCreateDirModalProps {
  /** 是否可见。 */
  open: boolean;
  /** 上级目录路径，如 `/`、`/foo`。 */
  parentPath: string;
  /** 关闭回调。 */
  onClose: () => void;
}

/**
 * 新建文件夹模态框。
 */
export function FileCreateDirModal({ open, parentPath, onClose }: FileCreateDirModalProps) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setName("");
      setError(null);
    }
  }, [open]);

  const mutation = useMutation({
    mutationFn: () => createDir({ parentPath, originalName: name.trim() }),
    onSuccess: () => {
      toast.success("已创建文件夹");
      void queryClient.invalidateQueries({ queryKey: ["file", "page"] });
      onClose();
    },
    onError: (err: unknown) => {
      setError(err instanceof HttpError ? err.message : "创建失败");
    },
  });

  const submit = () => {
    const trimmed = name.trim();
    if (!trimmed) {
      setError("名称不能为空");
      return;
    }
    if (trimmed.includes("/") || trimmed.includes("\\")) {
      setError("名称不能包含 / 或 \\");
      return;
    }
    mutation.mutate();
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="新建文件夹"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={mutation.isPending}>
            取消
          </Button>
          <Button onClick={submit} loading={mutation.isPending}>
            确认
          </Button>
        </>
      }
    >
      <FormField label="文件夹名称" required error={error ?? undefined}>
        <Input
          autoFocus
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") submit();
          }}
          placeholder="例如：报表 2026Q1"
        />
      </FormField>
      <p className="mt-2 text-xs text-zinc-400">上级目录：{parentPath || "/"}</p>
    </Modal>
  );
}
