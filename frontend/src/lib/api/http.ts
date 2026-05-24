import { type ApiResponse } from "./types";

/** 401 未登录回调（由 auth-store 注入，避免直接耦合 zustand） */
type UnauthorizedHandler = () => void;
let onUnauthorized: UnauthorizedHandler | null = null;
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  onUnauthorized = handler;
}

/** Token 提供者（由 auth-store 注入） */
type TokenProvider = () => string | null;
let getToken: TokenProvider = () => null;
export function setTokenProvider(provider: TokenProvider) {
  getToken = provider;
}

export class HttpError extends Error {
  readonly code: number;
  readonly status: number;

  constructor(code: number, message: string, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export interface RequestOptions extends Omit<RequestInit, "body"> {
  /** Query 参数会被序列化拼到 URL 后 */
  query?: Record<string, unknown>;
  /** body 会以 JSON 序列化 */
  body?: unknown;
  /** 是否跳过 401 自动跳转登录 */
  skipUnauthorizedHandler?: boolean;
}

function toQueryString(val: unknown): string | null {
  if (val === undefined || val === null || val === "") return null;
  if (typeof val === "string") return val;
  if (typeof val === "number" || typeof val === "boolean") return String(val);
  return null;
}

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

function getBaseUrl(): string {
  return (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
  ).replace(/\/+$/, "");
}

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

export const http = {
  get: <T>(path: string, query?: Record<string, unknown>, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "GET", query }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PATCH", body }),
  del: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "DELETE", body }),
};
