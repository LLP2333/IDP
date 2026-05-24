import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { OptionResp } from "~/lib/api/types";

import { SiteConfigForm } from "./site-config-form";

vi.mock("~/lib/api/option", () => ({
  updateOption: vi.fn().mockResolvedValue(undefined),
  uploadOptionImage: vi.fn(),
}));
import { updateOption } from "~/lib/api/option";

const mockedUpdate = updateOption as unknown as ReturnType<typeof vi.fn>;

function opt(code: string, id: number, value: string | null = ""): OptionResp {
  return {
    id,
    category: "SITE",
    name: code,
    code,
    value,
    description: null,
  };
}

const FULL: OptionResp[] = [
  opt("SITE_TITLE", 1, "IDP"),
  opt("SITE_COPYRIGHT", 2, ""),
  opt("SITE_DESCRIPTION", 3, ""),
  opt("SITE_LOGO", 4, null),
  opt("SITE_FAVICON", 5, null),
];

beforeEach(() => mockedUpdate.mockClear());
afterEach(() => vi.restoreAllMocks());

describe("SiteConfigForm", () => {
  it("修改 title + 保存调用 updateOption，仅包含已知字段", async () => {
    const user = userEvent.setup();
    render(<SiteConfigForm options={FULL} />);
    const titleInput = screen.getByDisplayValue("IDP");
    await user.clear(titleInput);
    await user.type(titleInput, "ACME");
    await user.click(screen.getByText("保存"));
    expect(mockedUpdate).toHaveBeenCalled();
    const arg = mockedUpdate.mock.calls[0]![0] as Array<{
      id: number;
      code: string;
      value: string | null;
    }>;
    const title = arg.find((it) => it.id === 1);
    expect(title?.value).toBe("ACME");
    expect(title?.code).toBe("SITE_TITLE");
    for (const it of arg) {
      expect(typeof it.code).toBe("string");
      expect(it.code.length).toBeGreaterThan(0);
    }
  });
});
