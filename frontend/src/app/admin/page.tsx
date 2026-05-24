"use client";

import { ShieldCheck, Users } from "lucide-react";
import Link from "next/link";

import { useAuthStore } from "~/lib/store/auth-store";

/**
 * 后台首页 / 概览页：展示当前账户信息与业务模块入口。
 */
export default function AdminHomePage() {
  const user = useAuthStore((s) => s.user);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold text-zinc-900">
          欢迎回来，{user?.nickname ?? user?.username ?? "用户"}
        </h2>
        <p className="mt-1 text-sm text-zinc-500">
          这里是 IDP 管理系统的后台首页。可以从左侧菜单进入业务模块。
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Link
          href="/admin/system/user"
          className="rounded-lg border border-zinc-200 bg-white p-5 transition-shadow hover:shadow-sm"
        >
          <Users size={20} className="text-blue-600" />
          <h3 className="mt-3 text-base font-semibold">用户管理</h3>
          <p className="mt-1 text-sm text-zinc-500">
            维护系统用户、分配角色、重置密码。
          </p>
        </Link>
        <Link
          href="/admin/system/role"
          className="rounded-lg border border-zinc-200 bg-white p-5 transition-shadow hover:shadow-sm"
        >
          <ShieldCheck size={20} className="text-emerald-600" />
          <h3 className="mt-3 text-base font-semibold">角色管理</h3>
          <p className="mt-1 text-sm text-zinc-500">
            管理系统角色与编码，作为后续权限控制基础。
          </p>
        </Link>
      </div>

      <div className="rounded-lg border border-zinc-200 bg-white p-5">
        <h3 className="text-sm font-semibold text-zinc-700">当前账户</h3>
        <dl className="mt-3 grid grid-cols-1 gap-y-2 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-zinc-400">用户名</dt>
            <dd className="text-zinc-800">{user?.username ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-zinc-400">昵称</dt>
            <dd className="text-zinc-800">{user?.nickname ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-zinc-400">角色</dt>
            <dd className="text-zinc-800">
              {user?.roles?.length ? user.roles.join(", ") : "—"}
            </dd>
          </div>
          <div>
            <dt className="text-zinc-400">邮箱</dt>
            <dd className="text-zinc-800">{user?.email ?? "—"}</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}
