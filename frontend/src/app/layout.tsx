import "~/styles/globals.css";

import { type Metadata } from "next";
import { Geist } from "next/font/google";

import { QueryProvider } from "~/components/providers/query-provider";
import { Toaster } from "sonner";

export const metadata: Metadata = {
  title: "IDP 管理系统",
  description: "通用企业级后台管理系统",
  icons: [{ rel: "icon", url: "/favicon.ico" }],
};

const geist = Geist({
  subsets: ["latin"],
  variable: "--font-geist-sans",
});

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN" className={geist.variable}>
      <body className="bg-background text-foreground min-h-screen antialiased">
        <QueryProvider>{children}</QueryProvider>
        <Toaster richColors position="top-right" />
      </body>
    </html>
  );
}
