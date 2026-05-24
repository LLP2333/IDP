"use client";

import { useRef, useState } from "react";

import { uploadOptionImage } from "~/lib/api/option";
import { cn } from "~/lib/utils";

import { Button } from "./button";

/** UploadImage 组件 props。 */
export interface UploadImageProps {
  /** 对应后端 option code（如 SITE_LOGO / SITE_FAVICON）。 */
  code: string;
  /** 当前图片 Data URL；为空时展示占位。 */
  value: string | null;
  /** 上传完成后回调，把新 Data URL 写回上层表单。 */
  onChange: (dataUrl: string | null) => void;
  /** 接受的 MIME 类型，默认 png / jpeg / svg。 */
  accept?: string;
  /** 最大体积（字节），默认 1MB（与后端校验对齐）。 */
  maxSize?: number;
  className?: string;
}

/**
 * 图片上传组件。
 *
 * 内部把文件转为 base64 Data URL 后提交到 {@code /system/option/image}，
 * 后端会校验 mime / 大小并写入对应 option，再把 Data URL 回写到本组件状态。
 */
export function UploadImage({
  code,
  value,
  onChange,
  accept = "image/png,image/jpeg,image/svg+xml",
  maxSize = 1024 * 1024,
  className,
}: UploadImageProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function readAsDataUrl(file: File): Promise<string> {
    return await new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result;
        if (typeof result === "string") {
          resolve(result);
        } else {
          reject(new Error("文件读取结果格式异常"));
        }
      };
      reader.onerror = () => reject(reader.error ?? new Error("读取文件失败"));
      reader.readAsDataURL(file);
    });
  }

  async function handleFile(file: File) {
    setError(null);
    if (file.size > maxSize) {
      setError(`文件超过 ${(maxSize / 1024).toFixed(0)}KB`);
      return;
    }
    setLoading(true);
    try {
      const dataUrl = await readAsDataUrl(file);
      const resp = await uploadOptionImage({ code, dataUrl });
      onChange(resp.dataUrl);
    } catch (err) {
      setError(err instanceof Error ? err.message : "上传失败");
    } finally {
      setLoading(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  }

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <div className="flex items-center gap-3">
        <div className="flex h-16 w-16 items-center justify-center overflow-hidden rounded border border-dashed border-zinc-300 bg-zinc-50">
          {value ? (
            <img src={value} alt={code} className="max-h-full max-w-full" />
          ) : (
            <span className="text-xs text-zinc-400">未上传</span>
          )}
        </div>
        <div className="flex flex-col gap-1">
          <input
            ref={inputRef}
            type="file"
            accept={accept}
            className="hidden"
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (file) await handleFile(file);
            }}
          />
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              loading={loading}
              onClick={() => inputRef.current?.click()}
            >
              选择图片
            </Button>
            {value ? (
              <Button size="sm" variant="ghost" onClick={() => onChange(null)}>
                清空
              </Button>
            ) : null}
          </div>
          <span className="text-xs text-zinc-500">支持 PNG / JPEG / SVG，单文件 ≤ {(maxSize / 1024).toFixed(0)}KB</span>
        </div>
      </div>
      {error ? <span className="text-xs text-red-600">{error}</span> : null}
    </div>
  );
}
