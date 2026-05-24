import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { MenuResp } from "~/lib/api/types";

import { MenuTree } from "./menu-tree";

function makeMenu(
  id: number,
  title: string,
  type: MenuResp["type"],
  parentId = 0,
  children: MenuResp[] = [],
  permission: string | null = null,
): MenuResp {
  return {
    id,
    title,
    parentId,
    type,
    path: type === 3 ? null : `/p${id}`,
    name: null,
    component: null,
    redirect: null,
    icon: null,
    isExternal: false,
    isCache: false,
    isHidden: false,
    permission,
    sort: 1,
    status: 1,
    isSystem: false,
    description: null,
    createdAt: "2024-01-01T00:00:00",
    updatedAt: null,
    children,
  };
}

const sample: MenuResp[] = [
  makeMenu(1, "系统", 1, 0, [
    makeMenu(2, "用户管理", 2, 1, [
      makeMenu(3, "新增用户", 3, 2, [], "system:user:add"),
      makeMenu(4, "删除用户", 3, 2, [], "system:user:delete"),
    ]),
  ]),
];

describe("MenuTree", () => {
  it("展示标题，展开后能看到按钮权限码", () => {
    render(<MenuTree data={sample} value={[]} onChange={() => undefined} />);
    expect(screen.getByText("系统")).toBeInTheDocument();
    expect(screen.getByText("用户管理")).toBeInTheDocument();
    // 默认只展开顶级节点，需要点击 “用户管理” 的展开按钮才能看到按钮
    fireEvent.click(screen.getByLabelText("展开"));
    expect(screen.getByText("system:user:add")).toBeInTheDocument();
  });

  it("勾选父节点会联动所有子节点 ID", () => {
    const handle = vi.fn();
    render(<MenuTree data={sample} value={[]} onChange={handle} />);
    // 第一个 checkbox 对应 “系统”
    const checkboxes = screen.getAllByRole("checkbox");
    fireEvent.click(checkboxes[0]!);
    expect(handle).toHaveBeenCalledTimes(1);
    const ids = (handle.mock.calls[0]![0] as number[]).sort((a, b) => a - b);
    expect(ids).toEqual([1, 2, 3, 4]);
  });

  it("取消父节点会同步移除所有子节点 ID", () => {
    const handle = vi.fn();
    render(
      <MenuTree
        data={sample}
        value={[1, 2, 3, 4]}
        onChange={handle}
      />,
    );
    const checkboxes = screen.getAllByRole("checkbox");
    // 父节点当前是 checked，再次点击会触发取消
    fireEvent.click(checkboxes[0]!);
    expect(handle).toHaveBeenCalled();
    const ids = handle.mock.calls[0]![0] as number[];
    expect(ids).not.toContain(2);
    expect(ids).not.toContain(3);
    expect(ids).not.toContain(4);
  });

  it("空数据时显示占位文案", () => {
    render(<MenuTree data={[]} value={[]} onChange={() => undefined} />);
    expect(screen.getByText("暂无菜单数据")).toBeInTheDocument();
  });
});
