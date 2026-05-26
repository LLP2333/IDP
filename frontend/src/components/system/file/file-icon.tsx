"use client";

import {
  File as FileIcn,
  FileArchive,
  FileAudio,
  FileCode,
  FileImage,
  FileText,
  FileVideo,
  Folder,
} from "lucide-react";

import { cn } from "~/lib/utils";

/**
 * 根据扩展名分类的视觉信息。
 */
interface IconEntry {
  Icon: typeof FileIcn;
  color: string;
}

/** 默认（未识别）扩展名图标。 */
const DEFAULT: IconEntry = { Icon: FileIcn, color: "text-zinc-400" };

/** 扩展名 → 图标 / 颜色映射表。 */
const ICON_MAP: Record<string, IconEntry> = {
  // 图片
  jpg: { Icon: FileImage, color: "text-pink-500" },
  jpeg: { Icon: FileImage, color: "text-pink-500" },
  png: { Icon: FileImage, color: "text-pink-500" },
  gif: { Icon: FileImage, color: "text-pink-500" },
  webp: { Icon: FileImage, color: "text-pink-500" },
  svg: { Icon: FileImage, color: "text-pink-500" },
  bmp: { Icon: FileImage, color: "text-pink-500" },
  // 视频
  mp4: { Icon: FileVideo, color: "text-purple-500" },
  mov: { Icon: FileVideo, color: "text-purple-500" },
  avi: { Icon: FileVideo, color: "text-purple-500" },
  mkv: { Icon: FileVideo, color: "text-purple-500" },
  webm: { Icon: FileVideo, color: "text-purple-500" },
  flv: { Icon: FileVideo, color: "text-purple-500" },
  // 音频
  mp3: { Icon: FileAudio, color: "text-amber-500" },
  wav: { Icon: FileAudio, color: "text-amber-500" },
  flac: { Icon: FileAudio, color: "text-amber-500" },
  m4a: { Icon: FileAudio, color: "text-amber-500" },
  // 文档
  txt: { Icon: FileText, color: "text-blue-500" },
  md: { Icon: FileText, color: "text-blue-500" },
  pdf: { Icon: FileText, color: "text-red-500" },
  doc: { Icon: FileText, color: "text-blue-600" },
  docx: { Icon: FileText, color: "text-blue-600" },
  xls: { Icon: FileText, color: "text-green-600" },
  xlsx: { Icon: FileText, color: "text-green-600" },
  ppt: { Icon: FileText, color: "text-orange-500" },
  pptx: { Icon: FileText, color: "text-orange-500" },
  // 压缩
  zip: { Icon: FileArchive, color: "text-yellow-600" },
  rar: { Icon: FileArchive, color: "text-yellow-600" },
  "7z": { Icon: FileArchive, color: "text-yellow-600" },
  tar: { Icon: FileArchive, color: "text-yellow-600" },
  gz: { Icon: FileArchive, color: "text-yellow-600" },
  // 代码
  js: { Icon: FileCode, color: "text-amber-600" },
  ts: { Icon: FileCode, color: "text-blue-500" },
  tsx: { Icon: FileCode, color: "text-blue-500" },
  jsx: { Icon: FileCode, color: "text-amber-600" },
  java: { Icon: FileCode, color: "text-red-500" },
  py: { Icon: FileCode, color: "text-green-500" },
  go: { Icon: FileCode, color: "text-sky-500" },
  rs: { Icon: FileCode, color: "text-orange-600" },
  html: { Icon: FileCode, color: "text-orange-500" },
  css: { Icon: FileCode, color: "text-blue-500" },
  json: { Icon: FileCode, color: "text-amber-600" },
};

/**
 * `FileIcon` Props。
 */
export interface FileIconProps {
  /** 是否文件夹。 */
  isDir?: boolean;
  /** 文件扩展名（不含 . ）；文件夹忽略。 */
  extension?: string | null;
  /** 缩略图 URL；图片有缩略图时优先展示。 */
  thumbnailUrl?: string | null;
  /** 尺寸 px，默认 36。 */
  size?: number;
  /** 自定义 className。 */
  className?: string;
}

/**
 * 文件 / 文件夹通用图标。
 *
 * - 文件夹：实色文件夹图标；
 * - 图片：优先用缩略图，否则用通用图片图标；
 * - 其他文件：按扩展名查 ICON_MAP，命中则按颜色显示。
 */
export function FileIcon({ isDir, extension, thumbnailUrl, size = 36, className }: FileIconProps) {
  if (isDir) {
    return <Folder size={size} className={cn("text-amber-400", className)} />;
  }
  if (thumbnailUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={thumbnailUrl}
        alt="缩略图"
        loading="lazy"
        className={cn("rounded object-cover", className)}
        style={{ width: size, height: size }}
      />
    );
  }
  const entry = (extension ? ICON_MAP[extension.toLowerCase()] : undefined) ?? DEFAULT;
  const Icon = entry.Icon;
  return <Icon size={size} className={cn(entry.color, className)} />;
}
