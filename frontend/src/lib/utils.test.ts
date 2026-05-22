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
  it("过滤掉 falsy 值", () => {
    expect(cn("a", false, null, undefined, "b")).toBe("a b");
  });

  it("没有有效值时返回空字符串", () => {
    expect(cn(false, null, undefined)).toBe("");
  });
});
