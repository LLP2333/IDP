import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { OptionResp } from "~/lib/api/types";

import { SecurityConfigForm } from "./security-config-form";

vi.mock("~/lib/api/option", () => ({
  updateOption: vi.fn().mockResolvedValue(undefined),
}));
import { updateOption } from "~/lib/api/option";

const mockedUpdate = updateOption as unknown as ReturnType<typeof vi.fn>;

function opt(code: string, id: number, value: string): OptionResp {
  return {
    id,
    category: "PASSWORD",
    name: code,
    code,
    value,
    description: null,
  };
}

const FULL: OptionResp[] = [
  opt("PASSWORD_ERROR_LOCK_COUNT", 1, "5"),
  opt("PASSWORD_ERROR_LOCK_MINUTES", 2, "10"),
  opt("PASSWORD_EXPIRATION_DAYS", 3, "90"),
  opt("PASSWORD_EXPIRATION_WARNING_DAYS", 4, "7"),
  opt("PASSWORD_REPETITION_TIMES", 5, "3"),
  opt("PASSWORD_MIN_LENGTH", 6, "8"),
  opt("PASSWORD_REQUIRE_SYMBOLS", 7, "0"),
  opt("PASSWORD_ALLOW_CONTAIN_USERNAME", 8, "0"),
];

beforeEach(() => mockedUpdate.mockClear());
afterEach(() => vi.restoreAllMocks());

describe("SecurityConfigForm", () => {
  it("提交时把所有字段转为 OptionReq[]", async () => {
    const user = userEvent.setup();
    render(<SecurityConfigForm options={FULL} />);
    await user.click(screen.getByText("保存"));
    expect(mockedUpdate).toHaveBeenCalledTimes(1);
    const arg = mockedUpdate.mock.calls[0]![0] as Array<{
      id: number;
      code: string;
      value: string;
    }>;
    expect(arg.length).toBe(8);
    for (const it of arg) {
      expect(typeof it.id).toBe("number");
      expect(typeof it.code).toBe("string");
      expect(it.code.length).toBeGreaterThan(0);
    }
  });

  it("提醒天数 > 有效期 时给出错误且阻止提交", async () => {
    const user = userEvent.setup();
    const bad = FULL.map((o) =>
      o.code === "PASSWORD_EXPIRATION_WARNING_DAYS" ? { ...o, value: "999" } : o,
    );
    render(<SecurityConfigForm options={bad} />);
    await user.click(screen.getByText("保存"));
    expect(await screen.findByText(/提醒天数不能大于有效期/)).toBeInTheDocument();
    expect(mockedUpdate).not.toHaveBeenCalled();
  });
});
