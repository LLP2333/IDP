import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { OptionResp } from "~/lib/api/types";

import { LoginConfigForm } from "./login-config-form";

vi.mock("~/lib/api/option", () => ({
  updateOption: vi.fn().mockResolvedValue(undefined),
}));
import { updateOption } from "~/lib/api/option";

const mockedUpdate = updateOption as unknown as ReturnType<typeof vi.fn>;

const OPTIONS: OptionResp[] = [
  {
    id: 10,
    category: "LOGIN",
    name: "是否启用验证码",
    code: "LOGIN_CAPTCHA_ENABLED",
    value: "0",
    description: null,
  },
];

beforeEach(() => mockedUpdate.mockClear());
afterEach(() => vi.restoreAllMocks());

describe("LoginConfigForm", () => {
  it("切换 switch + 保存会触发 updateOption", async () => {
    const user = userEvent.setup();
    render(<LoginConfigForm options={OPTIONS} />);
    await user.click(screen.getByRole("switch"));
    await user.click(screen.getByText("保存"));
    expect(mockedUpdate).toHaveBeenCalledWith([
      { id: 10, code: "LOGIN_CAPTCHA_ENABLED", value: "1" },
    ]);
  });

  it("readonly 时 switch 与按钮均禁用", () => {
    render(<LoginConfigForm options={OPTIONS} readonly />);
    expect(screen.getByRole("switch")).toBeDisabled();
    expect(screen.getByText("保存")).toBeDisabled();
  });
});
