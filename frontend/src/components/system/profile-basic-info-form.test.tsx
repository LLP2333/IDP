import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import type { UserInfo } from "~/lib/api/types";

import { ProfileBasicInfoForm } from "./profile-basic-info-form";

const FORM_ID = "test-form";

function buildUser(overrides: Partial<UserInfo> = {}): UserInfo {
  return {
    id: 1,
    username: "admin",
    nickname: "管理员",
    avatar: null,
    email: "admin@example.com",
    phone: "13800000000",
    gender: 1,
    roles: ["admin"],
    permissions: [],
    ...overrides,
  };
}

describe("ProfileBasicInfoForm", () => {
  it("用 initial 回填字段", () => {
    render(
      <ProfileBasicInfoForm
        formId={FORM_ID}
        initial={buildUser()}
        onSubmit={() => undefined}
      />,
    );
    expect(screen.getByDisplayValue("管理员")).toBeInTheDocument();
    expect(screen.getByDisplayValue("admin@example.com")).toBeInTheDocument();
    expect(screen.getByDisplayValue("13800000000")).toBeInTheDocument();
  });

  it("非法邮箱时阻止提交", async () => {
    const onSubmit = vi.fn();
    render(
      <ProfileBasicInfoForm
        formId={FORM_ID}
        initial={buildUser({ email: "" })}
        onSubmit={onSubmit}
      />,
    );

    const emailInput = document.querySelector<HTMLInputElement>(
      'input[type="email"]',
    )!;
    fireEvent.change(emailInput, { target: { value: "not-an-email" } });

    const form = document.getElementById(FORM_ID) as HTMLFormElement;
    fireEvent.submit(form);
    await screen.findByText(/邮箱格式不合法/);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("合法表单提交时透传归一化值", async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(
      <ProfileBasicInfoForm
        formId={FORM_ID}
        initial={buildUser()}
        onSubmit={onSubmit}
      />,
    );
    const nickname = screen.getByDisplayValue("管理员");
    await user.clear(nickname);
    await user.type(nickname, "新昵称");

    const form = document.getElementById(FORM_ID) as HTMLFormElement;
    form.requestSubmit();
    await vi.waitFor(() => expect(onSubmit).toHaveBeenCalled());
    const submitted = onSubmit.mock.calls[0]![0] as {
      nickname: string;
      gender: number;
    };
    expect(submitted.nickname).toBe("新昵称");
    expect(submitted.gender).toBe(1);
  });

  it("昵称为空时阻止提交", async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(
      <ProfileBasicInfoForm
        formId={FORM_ID}
        initial={buildUser()}
        onSubmit={onSubmit}
      />,
    );
    const nickname = screen.getByDisplayValue("管理员");
    await user.clear(nickname);

    const form = document.getElementById(FORM_ID) as HTMLFormElement;
    form.requestSubmit();
    await screen.findByText(/请输入昵称/);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
