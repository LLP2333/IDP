import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { SITE_CONFIG_QUERY_KEY, useSiteConfig } from "./use-site-config";

vi.mock("~/lib/api/option", () => ({
  getSiteConfigPublic: vi.fn(),
}));

import { getSiteConfigPublic } from "~/lib/api/option";

const mocked = getSiteConfigPublic as unknown as ReturnType<typeof vi.fn>;

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

/** 测试用的探针组件，只为了触发 hook 执行。 */
function Probe() {
  useSiteConfig();
  return null;
}

beforeEach(() => {
  mocked.mockReset();
  document.title = "init";
  document.head.querySelectorAll("link[rel~='icon']").forEach((n) => n.remove());
});

afterEach(() => vi.restoreAllMocks());

describe("useSiteConfig", () => {
  it("拉到 title 后同步 document.title", async () => {
    mocked.mockResolvedValue({
      title: "MyTitle",
      description: null,
      logo: null,
      favicon: null,
      copyright: null,
    });
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<Probe />, { wrapper: wrapper(client) });
    await waitFor(() => expect(document.title).toBe("MyTitle"));
  });

  it("拉到 favicon 后注入或更新 <link rel=icon>", async () => {
    mocked.mockResolvedValue({
      title: null,
      description: null,
      logo: null,
      favicon: "data:image/png;base64,AAA",
      copyright: null,
    });
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<Probe />, { wrapper: wrapper(client) });
    await waitFor(() => {
      const link = document.head.querySelector<HTMLLinkElement>("link[rel~='icon']");
      expect(link?.href).toBe("data:image/png;base64,AAA");
    });
  });

  it("暴露的 query key 与缓存共享", () => {
    expect(SITE_CONFIG_QUERY_KEY).toEqual(["public", "site"]);
  });
});
