"use client";

import { File, FileImage, FileText, FileVideo, Files, Music } from "lucide-react";

import { cn } from "~/lib/utils";

import { FileAsideStatistics } from "./file-aside-statistics";

/** 分类项。type=undefined 表示 “全部” 。 */
export interface FileCategory {
  /** 分类显示文本。 */
  label: string;
  /** 后端 type 枚举值；undefined 表示全部。 */
  type?: number;
  /** 图标。 */
  icon: React.ReactNode;
}

/** 文件类型分类（与后端 FileTypeEnum 对齐）。 */
export const FILE_CATEGORIES: FileCategory[] = [
  { label: "全部", icon: <Files size={16} /> },
  { label: "图片", type: 2, icon: <FileImage size={16} /> },
  { label: "文档", type: 3, icon: <FileText size={16} /> },
  { label: "视频", type: 4, icon: <FileVideo size={16} /> },
  { label: "音频", type: 5, icon: <Music size={16} /> },
  { label: "其他", type: 1, icon: <File size={16} /> },
];

/**
 * `FileAside` Props。
 */
export interface FileAsideProps {
  /** 当前选中分类 type；undefined 表示 “全部” 。 */
  current: number | undefined;
  /** 切换分类回调。 */
  onChange: (type: number | undefined) => void;
}

/**
 * 文件管理左侧分类侧栏。
 *
 * 顶部为分类列表，底部展示资源统计。
 */
export function FileAside({ current, onChange }: FileAsideProps) {
  return (
    <aside className="flex w-48 shrink-0 flex-col gap-3 border-r border-zinc-200 bg-white p-3">
      <div className="flex flex-col gap-1">
        {FILE_CATEGORIES.map((cat) => {
          const active = (current ?? undefined) === cat.type;
          return (
            <button
              key={cat.label}
              type="button"
              onClick={() => onChange(cat.type)}
              className={cn(
                "flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors",
                active ? "bg-blue-50 text-blue-700" : "text-zinc-600 hover:bg-zinc-100",
              )}
            >
              {cat.icon}
              <span>{cat.label}</span>
            </button>
          );
        })}
      </div>
      <div className="mt-2 border-t border-zinc-100 pt-3">
        <FileAsideStatistics />
      </div>
    </aside>
  );
}
