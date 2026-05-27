import { afterEach, describe, expect, it, vi } from "vitest";

import { apiUrl, cn, downloadByUrl, formatDateTime } from "./utils";

describe("apiUrl", () => {
  it("拼接相对路径与 base", () => {
    expect(apiUrl("/api/projects", "http://localhost:8080")).toBe(
      "http://localhost:8080/api/projects",
    );
  });

  it("处理 base 结尾多余的斜杠", () => {
    expect(apiUrl("/api/projects", "http://localhost:8080///")).toBe(
      "http://localhost:8080/api/projects",
    );
  });

  it("自动给 path 补斜杠", () => {
    expect(apiUrl("api/projects", "http://localhost:8080")).toBe(
      "http://localhost:8080/api/projects",
    );
  });
});

describe("cn", () => {
  it("合并 Tailwind 类名并处理冲突", () => {
    expect(cn("px-2", "px-4")).toBe("px-4");
  });

  it("过滤掉 falsy 值", () => {
    expect(cn("a", false && "b", null, undefined, "c")).toBe("a c");
  });
});

describe("formatDateTime", () => {
  it("把带微秒的 ISO 串截断到秒并把 T 替换为空格", () => {
    expect(formatDateTime("2026-05-27T08:52:24.674107")).toBe("2026-05-27 08:52:24");
  });

  it("ISO 不带毫秒也能正确格式化", () => {
    expect(formatDateTime("2026-05-27T08:52:24")).toBe("2026-05-27 08:52:24");
  });

  it("已经是空格分隔的形式按原样标准化", () => {
    expect(formatDateTime("2026-05-27 08:52:24")).toBe("2026-05-27 08:52:24");
  });

  it("仅有分钟时秒位补 00", () => {
    expect(formatDateTime("2026-05-27T08:52")).toBe("2026-05-27 08:52:00");
  });

  it("仅日期时只返回日期部分", () => {
    expect(formatDateTime("2026-05-27")).toBe("2026-05-27");
  });

  it("空值返回默认占位 —", () => {
    expect(formatDateTime(null)).toBe("—");
    expect(formatDateTime(undefined)).toBe("—");
    expect(formatDateTime("")).toBe("—");
    expect(formatDateTime("   ")).toBe("—");
  });

  it("无法解析时按原样返回 trim 后字符串", () => {
    expect(formatDateTime("not-a-date")).toBe("not-a-date");
  });

  it("支持自定义占位文案", () => {
    expect(formatDateTime(null, "-")).toBe("-");
  });
});

describe("downloadByUrl", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("成功 fetch 时用 blob URL 触发下载,文件名为传入的 originalName", async () => {
    const blob = new Blob(["hello"], { type: "text/plain" });
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(new Response(blob, { status: 200 }));
    const createObjectURL = vi.fn(() => "blob:mock-id");
    const revokeObjectURL = vi.fn();
    Object.assign(URL, { createObjectURL, revokeObjectURL });
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(vi.fn());

    await downloadByUrl("http://other-domain/file/abc123.png", "封面.png");

    expect(fetchMock).toHaveBeenCalledWith("http://other-domain/file/abc123.png", {
      credentials: "omit",
    });
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(clickSpy).toHaveBeenCalledTimes(1);

    const anchor = clickSpy.mock.contexts[0] as HTMLAnchorElement;
    expect(anchor.download).toBe("封面.png");
    expect(anchor.getAttribute("href")).toBe("blob:mock-id");
  });

  it("fetch 失败时回退到 a download + _blank", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("CORS"));
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(vi.fn());

    await downloadByUrl("https://cdn.example.com/abc.png", "封面.png");

    const anchor = clickSpy.mock.contexts[0] as HTMLAnchorElement;
    expect(anchor.download).toBe("封面.png");
    expect(anchor.target).toBe("_blank");
    expect(anchor.getAttribute("href")).toBe("https://cdn.example.com/abc.png");
  });
});
