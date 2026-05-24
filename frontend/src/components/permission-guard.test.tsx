import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { useAuthStore } from "~/lib/store/auth-store";

import { PermissionGuard } from "./permission-guard";

function login(roles: string[], permissions: string[]) {
  useAuthStore.setState({
    token: "tok",
    user: {
      id: 1,
      username: "u",
      nickname: null,
      avatar: null,
      email: null,
      phone: null,
      gender: 0,
      roles,
      permissions,
    },
    hydrated: true,
  });
}

beforeEach(() => {
  useAuthStore.setState({ token: null, user: null, hydrated: true });
});

describe("PermissionGuard", () => {
  it("拥有权限时渲染子节点", () => {
    login(["user"], ["system:user:add"]);
    render(
      <PermissionGuard codes={["system:user:add"]}>
        <span>btn</span>
      </PermissionGuard>,
    );
    expect(screen.getByText("btn")).toBeInTheDocument();
  });

  it("无权限时渲染 fallback", () => {
    login(["user"], []);
    render(
      <PermissionGuard codes={["system:user:add"]} fallback={<span>no</span>}>
        <span>btn</span>
      </PermissionGuard>,
    );
    expect(screen.queryByText("btn")).not.toBeInTheDocument();
    expect(screen.getByText("no")).toBeInTheDocument();
  });

  it("mode=all 时需要全部权限码", () => {
    login(["user"], ["a"]);
    render(
      <PermissionGuard codes={["a", "b"]} mode="all" fallback={<span>no</span>}>
        <span>btn</span>
      </PermissionGuard>,
    );
    expect(screen.getByText("no")).toBeInTheDocument();
  });
});
