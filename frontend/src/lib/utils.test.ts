import { describe, expect, it } from "vitest";

import { apiUrl, cn } from "./utils";

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
