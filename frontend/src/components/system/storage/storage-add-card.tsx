"use client";

import { Plus } from "lucide-react";

/**
 * `StorageAddCard` Props。
 */
export interface StorageAddCardProps {
  /** 点击回调。 */
  onClick: () => void;
}

/**
 * 末位的 "+" 添加卡。
 */
export function StorageAddCard({ onClick }: StorageAddCardProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex min-h-[180px] cursor-pointer items-center justify-center rounded-lg border border-dashed border-zinc-300 bg-zinc-50/30 text-zinc-400 transition-colors hover:border-blue-300 hover:bg-blue-50/30 hover:text-blue-500"
    >
      <div className="flex flex-col items-center gap-2 text-sm">
        <Plus size={28} />
        <span>新增存储</span>
      </div>
    </button>
  );
}
