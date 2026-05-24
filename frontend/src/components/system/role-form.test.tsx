import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { RoleForm, type RoleFormValues } from "./role-form";

describe("RoleForm", () => {
  it("校验失败时不会提交", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <>
        <RoleForm formId="t" onSubmit={onSubmit} />
        <button type="submit" form="t">submit</button>
      </>,
    );

    await user.click(screen.getByText("submit"));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(await screen.findByText(/请输入角色名称/)).toBeInTheDocument();
  });

  it("有效表单可正常提交", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <>
        <RoleForm formId="t" onSubmit={onSubmit} />
        <button type="submit" form="t">submit</button>
      </>,
    );

    const inputs = screen.getAllByRole("textbox");
    await user.type(inputs[0]!, "测试角色");
    await user.type(inputs[1]!, "test_role");
    await user.click(screen.getByText("submit"));

    await vi.waitFor(() => {
      expect(onSubmit).toHaveBeenCalledOnce();
    });
    const values = onSubmit.mock.calls[0]![0] as RoleFormValues;
    expect(values.code).toBe("test_role");
    expect(values.name).toBe("测试角色");
  });
});
