import "~/styles/globals.css";

import { type Metadata } from "next";
import { Geist } from "next/font/google";
import NextTopLoader from "nextjs-toploader";

import { QueryProvider } from "~/components/providers/query-provider";
import { Toaster } from "sonner";

export const metadata: Metadata = {
  title: "IDP 管理系统",
  description: "通用企业级后台管理系统",
  icons: [
    { rel: "icon", type: "image/png", url: "/logo.png" },
    { rel: "shortcut icon", url: "/favicon.ico" },
  ],
};

const geist = Geist({
  subsets: ["latin"],
  variable: "--font-geist-sans",
});

/**
 * Next.js App Router 根布局。
 *
 * 注入全局依赖：
 * - {@link QueryProvider}：TanStack Query 客户端；
 * - `NextTopLoader`：页面切换时顶部那条由左向右的蓝色进度条，对齐 thirdPart/continew-admin 的 NProgress 体验；
 * - `Toaster`：sonner 的全局 toast 容器，统一顶部居中显示、1.5 秒自动消失。
 */
export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN" className={geist.variable}>
      <body className="bg-background text-foreground min-h-screen antialiased">
        <NextTopLoader
          color="#2563eb"
          height={3}
          shadow="0 0 10px #2563eb,0 0 5px #2563eb"
          showSpinner={false}
          easing="ease"
          speed={300}
          crawlSpeed={150}
        />
        <QueryProvider>{children}</QueryProvider>
        <Toaster richColors position="top-center" duration={1500} />
      </body>
    </html>
  );
}
