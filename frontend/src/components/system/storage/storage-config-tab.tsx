"use client";

import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";

import { Empty } from "~/components/ui/empty";
import { Input } from "~/components/ui/input";
import { listStorage } from "~/lib/api/storage";
import type { StorageResp, StorageType } from "~/lib/api/types";
import { usePermission } from "~/lib/hooks/use-permission";

import { StorageAddCard } from "./storage-add-card";
import { StorageCard } from "./storage-card";
import { StorageFormModal } from "./storage-form-modal";

/**
 * 存储配置 Tab 入口。
 *
 * 顶部搜索 + 类型分组（本地存储 / 对象存储） + 卡片网格 + 末位 "+" 添加卡。
 */
export function StorageConfigTab() {
  const { hasPermission } = usePermission();
  const [keyword, setKeyword] = useState("");
  const [editTarget, setEditTarget] = useState<StorageResp | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const { data, isLoading, error } = useQuery({
    queryKey: ["storage", "list"],
    queryFn: () => listStorage(),
    staleTime: 30 * 1000,
  });

  const filtered = useMemo(() => {
    const list = data ?? [];
    const kw = keyword.trim().toLowerCase();
    return kw
      ? list.filter(
          (it) =>
            it.name.toLowerCase().includes(kw) ||
            it.code.toLowerCase().includes(kw) ||
            (it.description ?? "").toLowerCase().includes(kw),
        )
      : list;
  }, [data, keyword]);

  const grouped = useMemo(() => {
    const map: Record<StorageType, StorageResp[]> = { 1: [], 2: [] };
    for (const it of filtered) {
      map[it.type].push(it);
    }
    return map;
  }, [filtered]);

  const handleEdit = (s: StorageResp) => {
    setEditTarget(s);
    setModalOpen(true);
  };

  const handleAdd = () => {
    setEditTarget(null);
    setModalOpen(true);
  };

  if (isLoading) return <p className="text-sm text-zinc-500">加载中…</p>;
  if (error) return <p className="text-sm text-red-500">加载失败：{error.message}</p>;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <Input
          className="!w-72"
          placeholder="按名称 / 编码搜索"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
      </div>

      <StorageSection
        title="本地存储"
        items={grouped[1]}
        onEdit={handleEdit}
        canAdd={hasPermission("system:storage:add")}
        onAdd={handleAdd}
      />
      <StorageSection
        title="对象存储"
        items={grouped[2]}
        onEdit={handleEdit}
        canAdd={hasPermission("system:storage:add")}
        onAdd={handleAdd}
      />

      <StorageFormModal
        open={modalOpen}
        target={editTarget}
        onClose={() => setModalOpen(false)}
      />
    </div>
  );
}

interface SectionProps {
  title: string;
  items: StorageResp[];
  onEdit: (s: StorageResp) => void;
  canAdd: boolean;
  onAdd: () => void;
}

function StorageSection({ title, items, onEdit, canAdd, onAdd }: SectionProps) {
  return (
    <section>
      <header className="mb-2 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-zinc-700">{title}</h3>
        <span className="text-xs text-zinc-400">共 {items.length} 项</span>
      </header>
      {items.length === 0 && !canAdd ? (
        <Empty title="暂无存储" />
      ) : (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
          {items.map((s) => (
            <StorageCard key={s.id} storage={s} onEdit={onEdit} />
          ))}
          {canAdd ? <StorageAddCard onClick={onAdd} /> : null}
        </div>
      )}
    </section>
  );
}
