"use client";

import {
  ChevronLeft,
  ChevronRight,
  Maximize2,
  RotateCw,
  X,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { createPortal } from "react-dom";

/**
 * 单张图片描述。
 */
export interface ImagePreviewItem {
  /** 唯一标识(用作 key)。 */
  id: number | string;
  /** 图片访问 URL。 */
  url: string;
  /** 文件原始名(标题展示)。 */
  name?: string;
}

/**
 * `ImagePreview` Props。
 */
export interface ImagePreviewProps {
  /** 图片列表;为空数组或 null 表示关闭。 */
  images: ImagePreviewItem[] | null;
  /** 初始展示的图片索引。 */
  initialIndex?: number;
  /** 关闭回调。 */
  onClose: () => void;
}

/**
 * 原生 `lightbox` 风格的图片预览。
 *
 * <p>能力:</p>
 * <ul>
 *   <li>缩放、旋转、重置;</li>
 *   <li>列表轮播,左右箭头与键盘 Left/Right 切换;</li>
 *   <li>Esc 关闭。</li>
 * </ul>
 */
export function ImagePreview({ images, initialIndex = 0, onClose }: ImagePreviewProps) {
  const [zoom, setZoom] = useState(1);
  const [rotate, setRotate] = useState(0);
  const [index, setIndex] = useState(initialIndex);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    setIndex(initialIndex);
  }, [initialIndex, images]);

  useEffect(() => {
    setZoom(1);
    setRotate(0);
  }, [index]);

  const total = images?.length ?? 0;
  const goPrev = useCallback(() => {
    if (total <= 1) return;
    setIndex((i) => (i - 1 + total) % total);
  }, [total]);
  const goNext = useCallback(() => {
    if (total <= 1) return;
    setIndex((i) => (i + 1) % total);
  }, [total]);

  useEffect(() => {
    if (!images || images.length === 0) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
      else if (e.key === "ArrowLeft") goPrev();
      else if (e.key === "ArrowRight") goNext();
    }
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [images, onClose, goPrev, goNext]);

  if (!mounted || !images || images.length === 0) return null;
  const current = images[Math.min(Math.max(index, 0), images.length - 1)];
  if (!current) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-60 flex flex-col bg-black/80"
      onClick={onClose}
      role="dialog"
      aria-modal
    >
      <div
        className="flex items-center justify-between px-4 py-3 text-white"
        onClick={(e) => e.stopPropagation()}
      >
        <span className="truncate text-sm" title={current.name}>
          {current.name ?? "图片预览"}
          {total > 1 ? (
            <span className="ml-2 text-xs text-zinc-300">
              {index + 1} / {total}
            </span>
          ) : null}
        </span>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => setZoom((z) => Math.max(0.1, z - 0.2))}
            className="rounded p-1 hover:bg-white/10"
            aria-label="缩小"
          >
            <ZoomOut size={18} />
          </button>
          <span className="text-xs tabular-nums">{Math.round(zoom * 100)}%</span>
          <button
            type="button"
            onClick={() => setZoom((z) => Math.min(5, z + 0.2))}
            className="rounded p-1 hover:bg-white/10"
            aria-label="放大"
          >
            <ZoomIn size={18} />
          </button>
          <button
            type="button"
            onClick={() => setRotate((r) => r + 90)}
            className="rounded p-1 hover:bg-white/10"
            aria-label="旋转"
          >
            <RotateCw size={18} />
          </button>
          <button
            type="button"
            onClick={() => {
              setZoom(1);
              setRotate(0);
            }}
            className="rounded p-1 hover:bg-white/10"
            aria-label="重置"
          >
            <Maximize2 size={18} />
          </button>
          <button
            type="button"
            onClick={onClose}
            className="ml-2 rounded p-1 hover:bg-white/10"
            aria-label="关闭"
          >
            <X size={18} />
          </button>
        </div>
      </div>
      <div
        className="relative flex flex-1 items-center justify-center overflow-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {total > 1 ? (
          <button
            type="button"
            onClick={goPrev}
            className="absolute left-4 top-1/2 z-10 -translate-y-1/2 rounded-full bg-white/10 p-2 text-white hover:bg-white/20"
            aria-label="上一张"
          >
            <ChevronLeft size={24} />
          </button>
        ) : null}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          key={current.id}
          src={current.url}
          alt={current.name ?? "image"}
          style={{ transform: `scale(${zoom}) rotate(${rotate}deg)` }}
          className="max-h-full max-w-full object-contain transition-transform"
        />
        {total > 1 ? (
          <button
            type="button"
            onClick={goNext}
            className="absolute right-4 top-1/2 z-10 -translate-y-1/2 rounded-full bg-white/10 p-2 text-white hover:bg-white/20"
            aria-label="下一张"
          >
            <ChevronRight size={24} />
          </button>
        ) : null}
      </div>
    </div>,
    document.body,
  );
}
