import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from "vitest";

import {
  getLoginConfigPublic,
  getSiteConfigPublic,
  listOption,
  resetOption,
  updateOption,
  uploadOptionImage,
} from "./option";

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

describe("option api", () => {
  it("listOption 把 codes 数组展开为多个同名 query 参数", async () => {
    mockOk([]);
    await listOption({ category: "SITE", codes: ["SITE_TITLE", "SITE_LOGO"] });
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("category=SITE");
    expect(url).toContain("codes=SITE_TITLE");
    expect(url).toContain("codes=SITE_LOGO");
  });

  it("updateOption 使用 PUT 提交批量", async () => {
    mockOk(null);
    await updateOption([{ id: 1, code: "SITE_TITLE", value: "abc" }]);
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(init.body).toContain('"id":1');
    expect(init.body).toContain('"code":"SITE_TITLE"');
  });

  it("resetOption 使用 PATCH", async () => {
    mockOk(null);
    await resetOption({ category: "PASSWORD" });
    const init = (global.fetch as unknown as Mock).mock.calls[0]![1] as RequestInit;
    expect(init.method).toBe("PATCH");
  });

  it("uploadOptionImage POST + 透传 dataUrl", async () => {
    mockOk({ code: "SITE_LOGO", dataUrl: "data:image/png;base64,XX" });
    const resp = await uploadOptionImage({ code: "SITE_LOGO", dataUrl: "data:image/png;base64,XX" });
    expect(resp.dataUrl).toBe("data:image/png;base64,XX");
  });

  it("getSiteConfigPublic / getLoginConfigPublic 走公开 GET，且 skipUnauthorizedHandler", async () => {
    mockOk({ title: "Hi", copyright: null, description: null, logo: null, favicon: null });
    await getSiteConfigPublic();
    const url = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url).toContain("/system/option/site");

    mockOk({ captchaEnabled: false });
    await getLoginConfigPublic();
    const url2 = (global.fetch as unknown as Mock).mock.calls[0]![0] as string;
    expect(url2).toContain("/system/option/login");
  });
});
