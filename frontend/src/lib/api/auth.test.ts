import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import {
  changeCurrentPassword,
  getCaptcha,
  getUserInfo,
  login,
  logout,
  updateCurrentUserBasicInfo,
} from "./auth";

const ORIGINAL = process.env.NEXT_PUBLIC_API_BASE_URL;

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.test";
  vi.restoreAllMocks();
});

afterEach(() => {
  process.env.NEXT_PUBLIC_API_BASE_URL = ORIGINAL;
});

function mockOk(body: unknown) {
  global.fetch = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    headers: new Headers({ "content-type": "application/json" }),
    json: async () => ({ code: 0, msg: "ok", data: body, timestamp: 0 }),
  });
}

describe("auth api", () => {
  it("login 命中 /auth/login 且使用 POST", async () => {
    mockOk({ token: "t", expires: 60 });
    await login({ username: "admin", password: "123456" });
    const calls = (global.fetch as unknown as Mock).mock.calls[0]!;
    const url = calls[0] as string;
    const init = calls[1] as RequestInit;
    expect(url).toContain("/auth/login");
    expect(init.method).toBe("POST");
    expect(init.body).toContain('"username":"admin"');
  });

  it("logout 命中 /auth/logout 且使用 POST", async () => {
    mockOk(null);
    await logout();
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("POST");
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/auth/logout");
  });

  it("getUserInfo 命中 /auth/user/info 且使用 GET", async () => {
    mockOk({});
    await getUserInfo();
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("GET");
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/auth/user/info");
  });

  it("getCaptcha 命中 /auth/captcha", async () => {
    mockOk({ captchaId: "x", image: "data:image/svg+xml,abc", expiresIn: 120 });
    await getCaptcha();
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/auth/captcha");
  });

  it("changeCurrentPassword 走 POST /system/user/password", async () => {
    mockOk(null);
    await changeCurrentPassword({ oldPassword: "old", newPassword: "new" });
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("POST");
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/user/password");
    expect(init.body).toContain('"oldPassword":"old"');
  });

  it("updateCurrentUserBasicInfo 走 PUT /system/user/profile", async () => {
    mockOk(null);
    await updateCurrentUserBasicInfo({ nickname: "新名", gender: 1 });
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("PUT");
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/user/profile");
    expect(init.body).toContain('"nickname":"新名"');
    expect(init.body).toContain('"gender":1');
  });
});
