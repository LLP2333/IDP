"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { renameFile } from "~/lib/api/file";
import { HttpError } from "~/lib/api/http";
import type { FileResp } from "~/lib/api/types";

/**
 * `FileRenameModal` Props。
 */
export interface FileRenameModalProps {
  /** 待重命名的文件 / 文件夹；null 表示关闭。 */
  target: FileResp | null;
  /** 关闭回调。 */
  onClose: () => void;
}

/**
 * 文件 / 文件夹重命名模态框。
 *
 * 文件保留扩展名编辑：表单值是 originalName，后端会拒绝包含 `/` 的字符。
 */
export function FileRenameModal({ target, onClose }: FileRenameModalProps) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setName(target?.originalName ?? "");
    setError(null);
  }, [target]);

  const mutation = useMutation({
    mutationFn: () => renameFile(target!.id, { originalName: name.trim() }),
    onSuccess: () => {
      toast.success("已重命名");
      void queryClient.invalidateQueries({ queryKey: ["file", "page"] });
      onClose();
    },
    onError: (err: unknown) => {
      setError(err instanceof HttpError ? err.message : "重命名失败");
    },
  });

  if (!target) return null;

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
      open={!!target}
      onClose={onClose}
      title={target.type === 0 ? "重命名文件夹" : "重命名文件"}
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
      <FormField label="新名称" required error={error ?? undefined}>
        <Input
          autoFocus
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") submit();
          }}
        />
      </FormField>
    </Modal>
  );
}
