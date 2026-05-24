import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * 拼接 API URL：把相对路径与 base 合并。
 *
 * @example
 * apiUrl("/api/projects", "http://localhost:8080") => "http://localhost:8080/api/projects"
 */
export function apiUrl(path: string, baseUrl: string): string {
  const trimmedBase = baseUrl.replace(/\/+$/, "");
  const trimmedPath = path.startsWith("/") ? path : `/${path}`;
  return `${trimmedBase}${trimmedPath}`;
}

/**
 * Tailwind 类名合并工具：clsx + tailwind-merge。
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
