/**
 * 拼接 API URL：把相对路径与 `NEXT_PUBLIC_API_BASE_URL` 合并。
 *
 * @example
 * apiUrl("/api/projects") => "http://localhost:8080/api/projects"
 */
export function apiUrl(path: string, baseUrl: string): string {
  const trimmedBase = baseUrl.replace(/\/+$/, "");
  const trimmedPath = path.startsWith("/") ? path : `/${path}`;
  return `${trimmedBase}${trimmedPath}`;
}

/**
 * 简单的 className 合并工具：过滤掉 falsy 值再用空格连接。
 */
export function cn(
  ...classes: Array<string | false | null | undefined>
): string {
  return classes.filter(Boolean).join(" ");
}
