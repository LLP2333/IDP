import { describe, expect, it } from "vitest";

import { formatBytes } from "./file-aside-statistics";

describe("formatBytes", () => {
  it("0 / 边界值", () => {
    expect(formatBytes(0)).toBe("0 B");
    expect(formatBytes(-1)).toBe("0 B");
  });

  it("小于 1 KB 时输出 B 不带小数", () => {
    expect(formatBytes(512)).toBe("512 B");
  });

  it("KB / MB / GB 都按 2 位小数", () => {
    expect(formatBytes(1536)).toBe("1.50 KB");
    expect(formatBytes(1024 * 1024 * 3)).toBe("3.00 MB");
    expect(formatBytes(1024 * 1024 * 1024 * 1.25)).toBe("1.25 GB");
  });
});
