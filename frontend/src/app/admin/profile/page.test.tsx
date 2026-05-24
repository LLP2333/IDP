import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
  type Mock,
} from "vitest";

const { replace, toastSuccess, toastError } = vi.hoisted(() => ({
  replace: vi.fn(),
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn(), refresh: vi.fn() }),
  usePathname: () => "/admin/profile",
}));

vi.mock("sonner", () => ({
  toast: { success: toastSuccess, error: toastError },
  Toaster: () => null,
}));

vi.mock("~/lib/api/auth", () => ({
  updateCurrentUserBasicInfo: vi.fn(),
  changeCurrentPassword: vi.fn(),
  getUserInfo: vi.fn(),
  logout: vi.fn(),
}));

import {
  changeCurrentPassword,
  getUserInfo,
  logout,
  updateCurrentUserBasicInfo,
} from "~/lib/api/auth";
import type { UserInfo } from "~/lib/api/types";
import { useAuthStore } from "~/lib/store/auth-store";

import ProfilePage from "./page";

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

function renderWithClient() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <ProfilePage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  replace.mockClear();
  toastSuccess.mockClear();
  toastError.mockClear();
  (updateCurrentUserBasicInfo as unknown as Mock).mockReset();
  (changeCurrentPassword as unknown as Mock).mockReset();
  (getUserInfo as unknown as Mock).mockReset();
  (logout as unknown as Mock).mockReset();
});

afterEach(() => {
  vi.restoreAllMocks();
  useAuthStore.setState({ token: null, user: null, hydrated: true });
});

describe("ProfilePage", () => {
  it("无 user 时展示加载中", () => {
    useAuthStore.setState({ token: "t", user: null, hydrated: true });
    renderWithClient();
    expect(screen.getByText(/正在加载用户信息/)).toBeInTheDocument();
  });

  it("展示当前登录用户的基本信息", () => {
    useAuthStore.setState({ token: "t", user: buildUser(), hydrated: true });
    renderWithClient();
    expect(screen.getByRole("heading", { name: "个人中心" })).toBeInTheDocument();
    // “admin” 既是用户名又是角色，至少出现两次
    expect(screen.getAllByText("admin").length).toBeGreaterThanOrEqual(2);
    // 邮箱 / 手机在基本信息卡 + 安全设置卡都会出现
    expect(screen.getAllByText("admin@example.com").length).toBeGreaterThan(0);
    expect(screen.getAllByText("13800000000").length).toBeGreaterThan(0);
    expect(screen.getByText("男")).toBeInTheDocument();
  });

  it("点击编辑按钮后打开基本信息 Modal 并提交修改", async () => {
    useAuthStore.setState({ token: "t", user: buildUser(), hydrated: true });
    (updateCurrentUserBasicInfo as unknown as Mock).mockResolvedValue(undefined);
    (getUserInfo as unknown as Mock).mockResolvedValue(
      buildUser({ nickname: "新昵称" }),
    );

    const user = userEvent.setup();
    renderWithClient();
    await user.click(screen.getByRole("button", { name: /编辑/ }));

    expect(
      screen.getByRole("heading", { name: "修改基本信息" }),
    ).toBeInTheDocument();

    const nickname = screen.getByDisplayValue("管理员");
    fireEvent.change(nickname, { target: { value: "新昵称" } });
    await user.click(screen.getByRole("button", { name: "保存" }));

    await vi.waitFor(() => {
      expect(updateCurrentUserBasicInfo).toHaveBeenCalled();
    });
    const arg = (updateCurrentUserBasicInfo as unknown as Mock).mock.calls[0]![0] as {
      nickname: string;
    };
    expect(arg.nickname).toBe("新昵称");
    await vi.waitFor(() => {
      expect(toastSuccess).toHaveBeenCalledWith("已更新基本信息");
    });
  });

  it("修改密码成功后登出并跳转到登录页", async () => {
    useAuthStore.setState({ token: "t", user: buildUser(), hydrated: true });
    (changeCurrentPassword as unknown as Mock).mockResolvedValue(undefined);
    (logout as unknown as Mock).mockResolvedValue(undefined);

    const user = userEvent.setup();
    renderWithClient();
    // 第一个 “修改” 按钮属于 “登录密码” 行
    const modifyBtns = screen.getAllByRole("button", { name: "修改" });
    await user.click(modifyBtns[0]!);

    expect(
      screen.getByRole("heading", { name: "修改登录密码" }),
    ).toBeInTheDocument();

    const passwordInputs = document.querySelectorAll('input[type="password"]');
    expect(passwordInputs).toHaveLength(3);
    fireEvent.change(passwordInputs[0]!, { target: { value: "OldPwd#123" } });
    fireEvent.change(passwordInputs[1]!, { target: { value: "NewPwd#234" } });
    fireEvent.change(passwordInputs[2]!, { target: { value: "NewPwd#234" } });

    await user.click(screen.getByRole("button", { name: "保存" }));

    await vi.waitFor(() => {
      expect(changeCurrentPassword).toHaveBeenCalledWith({
        oldPassword: "OldPwd#123",
        newPassword: "NewPwd#234",
      });
    });
    await vi.waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/login");
    });
  });
});
