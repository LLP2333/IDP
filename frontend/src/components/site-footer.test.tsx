import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { SiteFooter } from "./site-footer";

vi.mock("~/lib/api/option", () => ({
  getSiteConfigPublic: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/test",
}));

import { getSiteConfigPublic } from "~/lib/api/option";

const mocked = getSiteConfigPublic as unknown as ReturnType<typeof vi.fn>;

function wrap(children: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

beforeEach(() => mocked.mockReset());

describe("SiteFooter", () => {
  it("copyright 与 beian 均为空时不渲染", async () => {
    mocked.mockResolvedValue({
      title: "T",
      copyright: "",
      beian: null,
      description: null,
      logo: null,
      favicon: null,
    });
    const { container } = render(wrap(<SiteFooter />));
    await waitFor(() => expect(container.querySelector("footer")).toBeNull());
  });

  it("有 copyright 时显示在 footer", async () => {
    mocked.mockResolvedValue({
      title: null,
      copyright: "Copyright © IDP",
      beian: null,
      description: null,
      logo: null,
      favicon: null,
    });
    render(wrap(<SiteFooter />));
    expect(await screen.findByText("Copyright © IDP")).toBeInTheDocument();
  });

  it("beian 与 copyright 同时存在时同一行显示", async () => {
    mocked.mockResolvedValue({
      title: null,
      copyright: "Copyright © IDP",
      beian: "粤 ICP 备 1 号",
      description: null,
      logo: null,
      favicon: null,
    });
    render(wrap(<SiteFooter />));
    expect(await screen.findByText("Copyright © IDP")).toBeInTheDocument();
    expect(await screen.findByText("粤 ICP 备 1 号")).toBeInTheDocument();
  });
});
