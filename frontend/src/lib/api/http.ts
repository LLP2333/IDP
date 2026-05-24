import { type ApiResponse } from "./types";

/** 401 未登录回调（由 auth-store 注入，避免直接耦合 zustand） */
type UnauthorizedHandler = () => void;
let onUnauthorized: UnauthorizedHandler | null = null;

/**
 * 注册全局 401 回调。
 *
 * 当任意请求返回 401，HTTP 层会调用该回调，通常由 `auth-store` 注入：
 * 清空登录态 → 跳转 `/login`。传入 `null` 可移除回调。
 *
 * @param handler 401 时执行的副作用函数
 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  onUnauthorized = handler;
}

/** Token 提供者（由 auth-store 注入） */
type TokenProvider = () => string | null;
let getToken: TokenProvider = () => null;

/**
 * 注册一个 Token 提供者。
 *
 * HTTP 层在发送请求前会调用 provider 获取最新 token 并自动写入
 * `Authorization: Bearer <token>` 头，避免组件层每次手动拼接。
 *
 * @param provider 返回当前 JWT 或 `null` 的函数
 */
export function setTokenProvider(provider: TokenProvider) {
  getToken = provider;
}

/**
 * 业务/网络错误。
 *
 * 凡是 HTTP 非 2xx，或后端业务 `code !== 0`，都会被封装为 `HttpError` 抛出。
 *
 * - `code`：业务码，0 表示成功，401/403 表示认证/鉴权失败，其余由业务自定义。
 * - `status`：原始 HTTP 状态码。
 */
export class HttpError extends Error {
  readonly code: number;
  readonly status: number;

  constructor(code: number, message: string, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

/**
 * `request` 的扩展选项，在原生 `RequestInit` 上加了 JSON / 401 处理相关字段。
 */
export interface RequestOptions extends Omit<RequestInit, "body"> {
  /** Query 参数会被序列化拼到 URL 后 */
  query?: Record<string, unknown>;
  /** body 会以 JSON 序列化 */
  body?: unknown;
  /** 是否跳过 401 自动跳转登录 */
  skipUnauthorizedHandler?: boolean;
}

/** 文件下载请求选项。 */
export interface DownloadOptions extends Omit<RequestInit, "body"> {
  /** Query 参数会被序列化拼到 URL 后 */
  query?: Record<string, unknown>;
  /** 默认下载文件名，服务端未返回 Content-Disposition 时使用 */
  filename?: string;
}

/**
 * 将单个原始值转为 query string 片段；空值返回 `null` 表示丢弃该字段。
 *
 * @param val 原始值
 * @returns 字符串或 `null`
 */
function toQueryString(val: unknown): string | null {
  if (val === undefined || val === null || val === "") return null;
  if (typeof val === "string") return val;
  if (typeof val === "number" || typeof val === "boolean") return String(val);
  return null;
}

/**
 * 把 base + path + 可选 query 拼接成最终 URL。
 *
 * - 自动去掉 base 末尾的多余 `/`；
 * - `path` 不以 `/` 开头时会自动补；
 * - `query` 中数组会展开成多个同名 key（如 `ids=1&ids=2`）。
 *
 * @param baseUrl 基地址，如 `http://localhost:8080`
 * @param path    业务路径，如 `/system/user`
 * @param query   可选 query 参数
 * @returns 完整 URL
 */
function buildUrl(baseUrl: string, path: string, query?: Record<string, unknown>): string {
  const trimmedBase = baseUrl.replace(/\/+$/, "");
  const trimmedPath = path.startsWith("/") ? path : `/${path}`;
  let url = `${trimmedBase}${trimmedPath}`;
  if (query) {
    const params = new URLSearchParams();
    for (const [key, val] of Object.entries(query)) {
      if (Array.isArray(val)) {
        for (const v of val) {
          const s = toQueryString(v);
          if (s !== null) params.append(key, s);
        }
      } else {
        const s = toQueryString(val);
        if (s !== null) params.append(key, s);
      }
    }
    const qs = params.toString();
    if (qs) url += (url.includes("?") ? "&" : "?") + qs;
  }
  return url;
}

/**
 * 解析后端基地址。
 *
 * 优先读取 `process.env.NEXT_PUBLIC_API_BASE_URL`，未设置时回退到 `http://localhost:8080`，
 * 保证在 SSR / Vitest / 客户端三种环境下都能可靠取值。
 *
 * @returns 已去掉末尾 `/` 的基地址
 */
function getBaseUrl(): string {
  return (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
  ).replace(/\/+$/, "");
}

/**
 * 通用 HTTP 请求。
 *
 * 内置：
 * - 自动注入 `Authorization: Bearer <token>` 与 `Accept: application/json`；
 * - body 自动 JSON 序列化；
 * - 401 触发 unauthorized 回调（可通过 `skipUnauthorizedHandler` 关掉）；
 * - 解析后端 `R<T>` 结构，仅当 `code === 0` 返回 `data`，否则抛出 `HttpError`。
 *
 * @template T 期望的返回数据类型
 * @param path    业务路径
 * @param options 请求选项
 * @returns 后端 `data` 字段
 * @throws {HttpError} 状态码非 2xx、业务码非 0、或响应为非 JSON 时
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { query, body, headers, skipUnauthorizedHandler, ...rest } = options;
  const url = buildUrl(getBaseUrl(), path, query);

  const finalHeaders = new Headers(headers ?? {});
  if (!finalHeaders.has("Content-Type") && body !== undefined) {
    finalHeaders.set("Content-Type", "application/json");
  }
  finalHeaders.set("Accept", "application/json");
  const token = getToken();
  if (token && !finalHeaders.has("Authorization")) {
    finalHeaders.set("Authorization", `Bearer ${token}`);
  }

  const init: RequestInit = {
    ...rest,
    headers: finalHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  };

  const response = await fetch(url, init);

  if (response.status === 401) {
    if (!skipUnauthorizedHandler && onUnauthorized) onUnauthorized();
    throw new HttpError(401, "未登录或登录已过期", 401);
  }

  let json: ApiResponse<T> | undefined;
  if (response.headers.get("content-type")?.includes("application/json")) {
    json = (await response.json()) as ApiResponse<T>;
  }

  if (!response.ok) {
    const msg = json?.msg ?? `请求失败: ${response.status}`;
    throw new HttpError(json?.code ?? response.status, msg, response.status);
  }

  if (!json) {
    return undefined as T;
  }

  if (json.code !== 0) {
    throw new HttpError(json.code, json.msg, response.status);
  }

  return json.data;
}

function resolveDownloadFilename(response: Response, fallback: string): string {
  const disposition = response.headers.get("content-disposition");
  if (!disposition) return fallback;
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
  if (encoded) return decodeURIComponent(encoded);
  const plain = /filename="?([^";]+)"?/i.exec(disposition)?.[1];
  return plain ? decodeURIComponent(plain) : fallback;
}

/**
 * 下载文件并触发浏览器保存。
 *
 * 复用普通请求的鉴权与 401 处理，但不解析 `R<T>` JSON 包装，适用于后端直接返回
 * Excel / CSV / 二进制流的导出接口。
 */
export async function download(path: string, options: DownloadOptions = {}): Promise<void> {
  const { query, headers, filename = "download", ...rest } = options;
  const url = buildUrl(getBaseUrl(), path, query);

  const finalHeaders = new Headers(headers ?? {});
  const token = getToken();
  if (token && !finalHeaders.has("Authorization")) {
    finalHeaders.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(url, {
    ...rest,
    method: rest.method ?? "GET",
    headers: finalHeaders,
  });

  if (response.status === 401) {
    if (onUnauthorized) onUnauthorized();
    throw new HttpError(401, "未登录或登录已过期", 401);
  }

  if (!response.ok) {
    throw new HttpError(response.status, `下载失败: ${response.status}`, response.status);
  }

  const blob = await response.blob();
  if (typeof window === "undefined") return;
  const href = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = href;
  link.download = resolveDownloadFilename(response, filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(href);
}

/**
 * 各种动词的便捷封装。
 *
 * 示例：
 * ```ts
 * const list = await http.get<UserResp[]>("/system/user", { page: 1, size: 10 });
 * await http.post("/system/user", { username: "zhangsan" });
 * ```
 */
export const http = {
  /** GET 请求；第二个参数会被拼到 URL query 上。 */
  get: <T>(path: string, query?: Record<string, unknown>, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "GET", query }),
  /** POST 请求；`body` 会被 JSON 序列化。 */
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body }),
  /** PUT 请求；`body` 会被 JSON 序列化。 */
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body }),
  /** PATCH 请求；`body` 会被 JSON 序列化。 */
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PATCH", body }),
  /** DELETE 请求；`body` 会被 JSON 序列化（后端批量删除接口约定使用 JSON body）。 */
  del: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "DELETE", body }),
  /** 文件下载请求。 */
  download,
};
