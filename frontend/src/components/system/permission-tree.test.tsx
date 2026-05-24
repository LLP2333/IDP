import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";

import type { PermissionResp } from "~/lib/api/types";

import { PermissionTree } from "./permission-tree";

const TREE: PermissionResp[] = [
  {
    id: 1,
    code: "system",
    name: "系统",
    type: 1,
    parentId: 0,
    sort: 1,
    status: 1,
    isSystem: true,
    description: null,
    children: [
      {
        id: 2,
        code: "system:user:list",
        name: "用户列表",
        type: 2,
        parentId: 1,
        sort: 1,
        status: 1,
        isSystem: true,
        description: null,
      },
      {
        id: 3,
        code: "system:user:add",
        name: "新增用户",
        type: 2,
        parentId: 1,
        sort: 2,
        status: 1,
        isSystem: true,
        description: null,
      },
    ],
  },
];

function Wrapper() {
  const [v, setV] = useState<number[]>([]);
  return <PermissionTree data={TREE} value={v} onChange={setV} />;
}

describe("PermissionTree", () => {
  it("勾选父节点会联动选中所有子节点", async () => {
    const user = userEvent.setup();
    render(<Wrapper />);
    const checkboxes = screen.getAllByRole("checkbox");
    await user.click(checkboxes[0]!);
    for (const cb of checkboxes) {
      expect((cb as HTMLInputElement).checked).toBe(true);
    }
  });

  it("取消勾选父节点会清空所有子节点", async () => {
    const user = userEvent.setup();
    render(<Wrapper />);
    const checkboxes = screen.getAllByRole("checkbox");
    await user.click(checkboxes[0]!);
    await user.click(checkboxes[0]!);
    for (const cb of checkboxes) {
      expect((cb as HTMLInputElement).checked).toBe(false);
    }
  });
});
