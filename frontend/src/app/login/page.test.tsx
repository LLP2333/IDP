import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
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
}));

vi.mock("sonner", () => ({
  toast: { success: toastSuccess, error: toastError },
  Toaster: () => null,
}));

vi.mock("~/lib/api/auth", () => ({
  login: vi.fn(),
  getUserInfo: vi.fn(),
  logout: vi.fn(),
}));

import { getUserInfo, login } from "~/lib/api/auth";
import { HttpError } from "~/lib/api/http";
import { useAuthStore } from "~/lib/store/auth-store";

import LoginPage from "./page";

function renderWithClient() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <LoginPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  replace.mockClear();
  toastSuccess.mockClear();
  toastError.mockClear();
  (login as unknown as Mock).mockReset();
  (getUserInfo as unknown as Mock).mockReset();
  useAuthStore.setState({ token: null, user: null, hydrated: true });
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("LoginPage", () => {
  it("登录失败时弹出 toast 错误", async () => {
    (login as unknown as Mock).mockRejectedValue(
      new HttpError(500, "用户名或密码错误", 400),
    );

    const user = userEvent.setup();
    renderWithClient();

    await user.click(screen.getByRole("button", { name: /登/ }));

    await vi.waitFor(() => {
      expect(toastError).toHaveBeenCalledWith("用户名或密码错误");
    });
  });

  it("登录成功后跳转 /admin 并设置用户", async () => {
    (login as unknown as Mock).mockResolvedValue({ token: "abc", expires: 3600 });
    (getUserInfo as unknown as Mock).mockResolvedValue({
      id: 1,
      username: "admin",
      nickname: "管理员",
      avatar: null,
      email: null,
      phone: null,
      roles: ["admin"],
      permissions: [],
    });

    const user = userEvent.setup();
    renderWithClient();

    await user.click(screen.getByRole("button", { name: /登/ }));

    await vi.waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/admin");
    });
    expect(useAuthStore.getState().token).toBe("abc");
    expect(useAuthStore.getState().user?.username).toBe("admin");
  });
});
