import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * 拼接 API URL：把相对路径与 base 合并。
 *
 * 自动去掉 base 末尾的 `/`，并保证 path 以 `/` 开头。
 *
 * @example
 * apiUrl("/api/projects", "http://localhost:8080") // => "http://localhost:8080/api/projects"
 *
 * @param path    业务路径
 * @param baseUrl 基地址
 * @returns 拼接后的完整 URL
 */
export function apiUrl(path: string, baseUrl: string): string {
  const trimmedBase = baseUrl.replace(/\/+$/, "");
  const trimmedPath = path.startsWith("/") ? path : `/${path}`;
  return `${trimmedBase}${trimmedPath}`;
}

/**
 * Tailwind 类名合并工具：`clsx` + `tailwind-merge`。
 *
 * 先用 `clsx` 处理条件与数组，再用 `tailwind-merge` 去重 / 合并冲突的 Tailwind 类。
 *
 * @example
 * cn("px-2 py-1", isActive && "bg-blue-500", "px-4") // "py-1 bg-blue-500 px-4"
 *
 * @param inputs 任意可被 `clsx` 接受的类名输入
 * @returns 已合并的最终 className 字符串
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

/**
 * 触发浏览器以 `filename` 为文件名下载指定 URL 的内容。
 *
 * <p>背景:`<a href download>` 在 **跨源** URL 上,浏览器会忽略 `download`
 * 属性,行为退化为 “在新标签页打开”;再加上图片 / PDF 等可被原生预览的资源
 * 类型,默认会被浏览器直接渲染而非下载。两者叠加导致 “点下载实际是预览”。</p>
 *
 * <p>本函数的策略:</p>
 * <ol>
 *   <li>先 {@code fetch} 拉成 {@code Blob},再创建同源的 {@code blob:} URL 配合
 *       {@code <a download>} 触发下载。同源 + Content-Type 任意都能让浏览器走真正的
 *       下载流,文件名按传入的 {@code filename} 来(即 originalName);</li>
 *   <li>fetch 失败(如对象存储未开放 CORS)时,回退到普通 {@code <a href download>}
 *       并打开新标签,由用户 “另存为”。</li>
 * </ol>
 *
 * @param url      文件公开访问 URL(可同源也可跨源)
 * @param filename 下载文件保存到本地时使用的文件名,通常为后端返回的 originalName
 */
export async function downloadByUrl(url: string, filename: string): Promise<void> {
  try {
    const response = await fetch(url, { credentials: "omit" });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const blob = await response.blob();
    const blobUrl = URL.createObjectURL(blob);
    triggerAnchorDownload(blobUrl, filename);
    setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);
  } catch {
    triggerAnchorDownload(url, filename, "_blank");
  }
}

function triggerAnchorDownload(href: string, filename: string, target?: "_blank"): void {
  const a = document.createElement("a");
  a.href = href;
  a.download = filename;
  if (target) {
    a.target = target;
    a.rel = "noopener noreferrer";
  }
  document.body.appendChild(a);
  a.click();
  a.remove();
}
