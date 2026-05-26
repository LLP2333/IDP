"use client";

import { Download, ExternalLink, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { cn, downloadByUrl } from "~/lib/utils";

/**
 * 当前支持的文档预览类型。
 *
 * - {@code pdf}:浏览器原生 PDF 阅读器(iframe);
 * - {@code docx}:基于 {@code docx-preview} 本地渲染;
 * - {@code xlsx}:基于 {@code xlsx}(SheetJS)解析并渲染 HTML table;
 * - {@code unsupported}:不支持,展示下载与新窗口打开按钮。
 */
export type OfficePreviewKind = "pdf" | "docx" | "xlsx" | "unsupported";

/**
 * 根据扩展名判断 OfficePreview 能力。
 *
 * @param extension 文件扩展名(无前导 `.`),大小写均可
 * @returns 命中的预览类型;命中失败返回 null,调用方应回退到其他预览或详情
 */
export function getOfficePreviewKind(
  extension: string | null | undefined,
): OfficePreviewKind | null {
  if (!extension) return null;
  const ext = extension.toLowerCase();
  if (ext === "pdf") return "pdf";
  if (ext === "docx" || ext === "doc") return "docx";
  if (ext === "xlsx" || ext === "xls") return "xlsx";
  if (ext === "ppt" || ext === "pptx") return "unsupported";
  return null;
}

/**
 * `OfficePreview` Props。
 */
export interface OfficePreviewProps {
  /** 文件直链 URL;null 表示关闭。 */
  url: string | null;
  /** 文件原始名(仅用于标题展示与下载文件名)。 */
  name?: string;
  /** 文件扩展名,不带前导 `.`,大小写均可。 */
  extension?: string | null;
  /** 关闭回调。 */
  onClose: () => void;
}

/**
 * 文档预览弹层。
 *
 * <p>统一处理 PDF / DOCX / XLSX 这三类常见 Office 文档的浏览器内预览,其余扩展名兜底为
 * “暂不支持 + 下载 + 在新标签页打开” 的 UX。所有解析在浏览器本地完成,不依赖任何外部
 * 在线服务,适合生产环境数据安全要求。</p>
 */
export function OfficePreview({ url, name, extension, onClose }: OfficePreviewProps) {
  const kind = getOfficePreviewKind(extension);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!url) return;
    function handleEscape(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, [url, onClose]);

  if (!mounted || !url) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-60 flex flex-col bg-black/70"
      onClick={onClose}
      role="dialog"
      aria-modal
    >
      <div
        className="mx-auto mt-6 flex h-[90vh] w-[min(1200px,95vw)] flex-col overflow-hidden rounded-lg bg-white shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-2">
          <span className="truncate text-sm font-medium text-zinc-800" title={name}>
            {name ?? "文档预览"}
          </span>
          <div className="flex items-center gap-1 text-zinc-500">
            <a
              href={url}
              target="_blank"
              rel="noreferrer"
              className="rounded p-1 hover:bg-zinc-100"
              aria-label="新标签页打开"
              title="新标签页打开"
            >
              <ExternalLink size={16} />
            </a>
            <button
              type="button"
              onClick={() => {
                void downloadByUrl(url, name ?? "download");
              }}
              className="rounded p-1 hover:bg-zinc-100"
              aria-label="下载"
              title="下载"
            >
              <Download size={16} />
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded p-1 hover:bg-zinc-100"
              aria-label="关闭"
            >
              <X size={16} />
            </button>
          </div>
        </div>
        <div className="flex-1 overflow-auto bg-zinc-50">
          {kind === "pdf" ? (
            <PdfBody url={url} name={name} />
          ) : kind === "docx" ? (
            <DocxBody url={url} />
          ) : kind === "xlsx" ? (
            <XlsxBody url={url} />
          ) : (
            <UnsupportedBody url={url} name={name} extension={extension} />
          )}
        </div>
      </div>
    </div>,
    document.body,
  );
}

/**
 * 用浏览器内置 PDF 阅读器渲染。
 */
function PdfBody({ url, name }: { url: string; name?: string }) {
  return (
    <iframe
      title={name ?? "PDF 预览"}
      src={url}
      className="h-full w-full border-0"
    />
  );
}

/**
 * 基于 {@code docx-preview} 的本地 DOCX 渲染。
 */
function DocxBody({ url }: { url: string }) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    setLoading(true);
    void (async () => {
      try {
        const [{ renderAsync }, response] = await Promise.all([
          import("docx-preview"),
          fetch(url),
        ]);
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        const blob = await response.blob();
        if (cancelled) return;
        const container = containerRef.current;
        if (!container) return;
        container.innerHTML = "";
        await renderAsync(blob, container, undefined, {
          className: "docx",
          inWrapper: true,
        });
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : "渲染失败");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [url]);

  return (
    <div className="h-full w-full overflow-auto p-6">
      {loading ? <p className="text-sm text-zinc-400">解析中…</p> : null}
      {error ? <p className="text-sm text-red-500">DOCX 渲染失败:{error}</p> : null}
      <div ref={containerRef} className="docx-preview-container" />
    </div>
  );
}

/**
 * 基于 SheetJS 的 XLSX 解析与渲染。
 *
 * <p>遍历每个 sheet,把第一行作为表头(若为空白则用列字母),其余行渲染为标准 HTML
 * table;不依赖任何 react table 组件,加载最轻量。</p>
 */
function XlsxBody({ url }: { url: string }) {
  const [sheets, setSheets] = useState<{ name: string; rows: string[][] }[]>([]);
  const [active, setActive] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    setLoading(true);
    void (async () => {
      try {
        const [xlsxMod, response] = await Promise.all([import("xlsx"), fetch(url)]);
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        const buffer = await response.arrayBuffer();
        if (cancelled) return;
        const workbook = xlsxMod.read(buffer, { type: "array" });
        const list = workbook.SheetNames.map((name) => {
          const sheet = workbook.Sheets[name];
          const rows = sheet
            ? xlsxMod.utils.sheet_to_json<string[]>(sheet, {
                header: 1,
                defval: "",
                blankrows: false,
              })
            : [];
          return { name, rows };
        });
        if (!cancelled) {
          setSheets(list);
          setActive(0);
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : "解析失败");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [url]);

  if (loading) {
    return <p className="p-6 text-sm text-zinc-400">解析中…</p>;
  }
  if (error) {
    return <p className="p-6 text-sm text-red-500">XLSX 解析失败:{error}</p>;
  }
  if (sheets.length === 0) {
    return <p className="p-6 text-sm text-zinc-400">表格为空</p>;
  }
  const current = sheets[active] ?? sheets[0];
  if (!current) {
    return <p className="p-6 text-sm text-zinc-400">表格为空</p>;
  }

  return (
    <div className="flex h-full w-full flex-col">
      {sheets.length > 1 ? (
        <div className="flex shrink-0 gap-1 border-b border-zinc-200 bg-white px-3 py-1">
          {sheets.map((s, i) => (
            <button
              key={s.name}
              type="button"
              onClick={() => setActive(i)}
              className={cn(
                "rounded px-3 py-1 text-xs",
                i === active
                  ? "bg-blue-50 text-blue-600"
                  : "text-zinc-600 hover:bg-zinc-100",
              )}
            >
              {s.name}
            </button>
          ))}
        </div>
      ) : null}
      <div className="flex-1 overflow-auto">
        <table className="w-max min-w-full border-collapse text-xs">
          <tbody>
            {current.rows.map((row, ri) => (
              <tr key={ri} className={ri === 0 ? "bg-zinc-100 font-medium" : undefined}>
                {row.map((cell, ci) => (
                  <td
                    key={ci}
                    className="whitespace-nowrap border border-zinc-200 px-2 py-1 text-zinc-700"
                  >
                    {String(cell ?? "")}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/**
 * 不支持时的兜底视图。
 */
function UnsupportedBody({
  url,
  name,
  extension,
}: {
  url: string;
  name?: string;
  extension?: string | null;
}) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
      <p className="text-sm text-zinc-600">
        {extension ? `.${extension.toLowerCase()} ` : ""}文件暂不支持在线预览
      </p>
      <div className="flex gap-2">
        <a
          href={url}
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-1 rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-600 hover:bg-zinc-100"
        >
          <ExternalLink size={14} /> 新标签页打开
        </a>
        <button
          type="button"
          onClick={() => {
            void downloadByUrl(url, name ?? "download");
          }}
          className="inline-flex items-center gap-1 rounded-md bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700"
        >
          <Download size={14} /> 下载
        </button>
      </div>
    </div>
  );
}
