"use client";

import { useState } from "react";

import { FileAside } from "~/components/system/file/file-aside";
import { FileMain } from "~/components/system/file/file-main";

/**
 * `/admin/system/file` 文件管理页。
 *
 * 双列布局：
 * - 左侧：分类筛选（全部 / 图片 / 文档 / 视频 / 音频 / 其他） + 资源统计；
 * - 右侧：面包屑 + 操作栏 + 文件 grid/list + 分页。
 */
export default function FilePage() {
  const [type, setType] = useState<number | undefined>(undefined);
  return (
    <div className="-m-6 flex h-[calc(100%+3rem)] overflow-hidden bg-zinc-50">
      <FileAside current={type} onChange={setType} />
      <FileMain type={type} />
    </div>
  );
}
