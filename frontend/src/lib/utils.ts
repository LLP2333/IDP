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
