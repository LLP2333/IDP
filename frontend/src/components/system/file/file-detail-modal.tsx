"use client";

import { Button } from "~/components/ui/button";
import { Modal } from "~/components/ui/modal";
import type { FileResp } from "~/lib/api/types";

import { formatBytes } from "./file-aside-statistics";
import { FileIcon } from "./file-icon";

/**
 * `FileDetailModal` Props。
 */
export interface FileDetailModalProps {
  /** 要展示的文件；null 表示关闭。 */
  target: FileResp | null;
  /** 关闭回调。 */
  onClose: () => void;
}

/**
 * 文件详情模态框：展示尺寸 / 类型 / 路径 / SHA256 等元数据。
 */
export function FileDetailModal({ target, onClose }: FileDetailModalProps) {
  if (!target) return null;
  const isDir = target.type === 0;
  return (
    <Modal
      open={!!target}
      onClose={onClose}
      title={isDir ? "文件夹详情" : "文件详情"}
      size="md"
      footer={
        <Button variant="outline" onClick={onClose}>
          关闭
        </Button>
      }
    >
      <div className="flex items-start gap-4">
        <FileIcon
          isDir={isDir}
          extension={target.extension}
          thumbnailUrl={target.thumbnailUrl}
          size={64}
        />
        <div className="min-w-0 flex-1 space-y-2 text-sm">
          <DetailRow label="名称" value={target.originalName} breakAll />
          <DetailRow label="存储名" value={target.name} mono breakAll />
          <DetailRow label="路径" value={target.path} mono breakAll />
          <DetailRow label="大小" value={isDir ? "—" : formatBytes(target.size)} />
          {!isDir ? (
            <>
              <DetailRow label="类型" value={target.contentType ?? "—"} />
              <DetailRow label="扩展名" value={target.extension ?? "—"} />
              <DetailRow label="SHA256" value={target.sha256 ?? "—"} mono breakAll />
              {target.url ? <DetailRow label="访问链接" value={target.url} mono breakAll /> : null}
            </>
          ) : null}
          <DetailRow label="创建时间" value={target.createdAt} />
          {target.updatedAt ? <DetailRow label="更新时间" value={target.updatedAt} /> : null}
        </div>
      </div>
    </Modal>
  );
}

interface DetailRowProps {
  label: string;
  value: string | number;
  mono?: boolean;
  breakAll?: boolean;
}

function DetailRow({ label, value, mono, breakAll }: DetailRowProps) {
  return (
    <div className="grid grid-cols-[80px_1fr] items-baseline gap-3">
      <span className="text-xs text-zinc-500">{label}</span>
      <span
        className={`text-sm text-zinc-700 ${mono ? "font-mono text-xs" : ""} ${breakAll ? "break-all" : ""}`}
      >
        {value}
      </span>
    </div>
  );
}
