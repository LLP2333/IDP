"use client";

import { useQuery } from "@tanstack/react-query";

import { Progress } from "~/components/ui/progress";
import { getFileStatistics } from "~/lib/api/file";
import type { FileStatisticsDetail } from "~/lib/api/types";

/**
 * 把字节数转人类可读字符串：B / KB / MB / GB / TB 。
 *
 * @param bytes 字节数
 * @returns 字符串，保留 2 位小数
 */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB", "PB"];
  let value = bytes;
  let idx = 0;
  while (value >= 1024 && idx < units.length - 1) {
    value /= 1024;
    idx += 1;
  }
  return `${value.toFixed(idx === 0 ? 0 : 2)} ${units[idx]}`;
}

/** 类型名到 Tailwind 进度条颜色变体的映射。 */
const COLOR_MAP: Record<number, "blue" | "green" | "red" | "amber"> = {
  2: "blue",
  3: "green",
  4: "red",
  5: "amber",
  1: "amber",
};

/**
 * 文件管理资源统计卡片。
 *
 * 展示总占用 + 各类型的占比 + 数量。
 */
export function FileAsideStatistics() {
  const { data, isLoading } = useQuery({
    queryKey: ["file", "statistics"],
    queryFn: getFileStatistics,
    staleTime: 30 * 1000,
  });

  if (isLoading) {
    return <p className="text-xs text-zinc-400">统计加载中…</p>;
  }
  if (!data) {
    return <p className="text-xs text-zinc-400">暂无数据</p>;
  }
  const totalSize = data.size > 0 ? data.size : 1;
  return (
    <div className="flex flex-col gap-2">
      <div className="text-xs font-semibold text-zinc-500">资源占用</div>
      <div className="text-sm font-medium text-zinc-700">{formatBytes(data.size)}</div>
      <div className="text-xs text-zinc-500">共 {data.number} 个文件</div>
      <div className="mt-1 flex flex-col gap-2">
        {data.data.map((item: FileStatisticsDetail) => {
          const ratio = (item.size / totalSize) * 100;
          return (
            <div key={item.type} className="flex flex-col gap-1">
              <div className="flex items-center justify-between text-xs text-zinc-600">
                <span>{item.name}</span>
                <span className="tabular-nums">{item.number}</span>
              </div>
              <Progress value={ratio} variant={COLOR_MAP[item.type] ?? "blue"} />
              <div className="text-[10px] text-zinc-400">{formatBytes(item.size)}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
