import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
  type Mock,
} from "vitest";

import {
  HttpError,
  request,
  setTokenProvider,
  setUnauthorizedHandler,
} from "./http";

const ORIGINAL_ENV = process.env.NEXT_PUBLIC_API_BASE_URL;

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  vi.restoreAllMocks();
  setTokenProvider(() => null);
  setUnauthorizedHandler(null);
});

afterEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = ORIGINAL_ENV;
});

function mockFetchOk(body: unknown, status = 200) {
  global.fetch = vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ "content-type": "application/json" }),
    json: async () => body,
  });
}

function mockFetchStatus(status: number, body: unknown) {
  global.fetch = vi.fn().mockResolvedValue({
    ok: false,
    status,
    headers: new Headers({ "content-type": "application/json" }),
    json: async () => body,
  });
}

describe("http.request", () => {
  it("注入 Authorization 头并解包成功响应", async () => {
    setTokenProvider(() => "tok-1");
    mockFetchOk({ code: 0, msg: "ok", data: { hello: "world" }, timestamp: 0 });
    const data = await request<{ hello: string }>("/foo", { method: "GET" });
    expect(data).toEqual({ hello: "world" });
    const fetchMock = global.fetch as unknown as Mock;
    const call = fetchMock.mock.calls[0]!;
    const init = call[1] as RequestInit;
    const headers = init.headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer tok-1");
  });

  it("根据 query 拼接 URL，过滤 undefined/null/空串", async () => {
    mockFetchOk({ code: 0, msg: "ok", data: null, timestamp: 0 });
    await request("/system/user", {
      method: "GET",
      query: {
        page: 1,
        username: "admin",
        empty: "",
        none: undefined,
        zero: 0,
      },
    });
    const fetchMock = global.fetch as unknown as Mock;
    const calledUrl = fetchMock.mock.calls[0]![0] as string;
    expect(calledUrl).toContain("page=1");
    expect(calledUrl).toContain("username=admin");
    expect(calledUrl).toContain("zero=0");
    expect(calledUrl).not.toContain("empty=");
    expect(calledUrl).not.toContain("none=");
  });

  it("401 时调用 unauthorized 回调并抛出 HttpError", async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    mockFetchStatus(401, { code: 401, msg: "unauthenticated", data: null });

    await expect(request("/foo")).rejects.toBeInstanceOf(HttpError);
    expect(handler).toHaveBeenCalledOnce();
  });

  it("skipUnauthorizedHandler 跳过 401 回调", async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    mockFetchStatus(401, { code: 401, msg: "unauthenticated", data: null });

    await expect(
      request("/foo", { skipUnauthorizedHandler: true }),
    ).rejects.toBeInstanceOf(HttpError);
    expect(handler).not.toHaveBeenCalled();
  });

  it("业务错误码非 0 抛出 HttpError 携带后端 msg", async () => {
    mockFetchOk({ code: 500, msg: "用户名已存在", data: null, timestamp: 0 });
    await expect(request("/foo")).rejects.toMatchObject({
      code: 500,
      message: "用户名已存在",
    });
  });
});
