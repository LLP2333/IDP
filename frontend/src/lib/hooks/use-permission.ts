"use client";

import { useAuthStore } from "~/lib/store/auth-store";

/**
 * 当前登录用户权限相关的 Hook。
 *
 * 与后端 {@code com.qvqw.idp.permission.annotation.HasPermission} 对齐：
 * - {@code admin} 角色直通；
 * - 普通用户根据已绑定的按钮级权限码判断。
 *
 * 使用示例：
 * ```tsx
 * const { hasPermission } = usePermission();
 * if (!hasPermission("system:user:add")) return null;
 * ```
 *
 * @returns 三个判断函数
 */
export function usePermission() {
  const user = useAuthStore((s) => s.user);
  const roles = user?.roles ?? [];
  const permissions = user?.permissions ?? [];
  const isAdmin = roles.includes("admin");

  /** 是否拥有指定权限码（admin 直通）。 */
  function hasPermission(code: string): boolean {
    if (isAdmin) return true;
    return permissions.includes(code);
  }

  /** 是否拥有数组中任意一个权限码（OR 逻辑，与后端 Mode.OR 对齐）。 */
  function hasAnyPermission(codes: string[]): boolean {
    if (codes.length === 0) return true;
    if (isAdmin) return true;
    return codes.some((c) => permissions.includes(c));
  }

  /** 是否拥有数组中全部权限码（AND 逻辑，与后端 Mode.AND 对齐）。 */
  function hasAllPermissions(codes: string[]): boolean {
    if (codes.length === 0) return true;
    if (isAdmin) return true;
    return codes.every((c) => permissions.includes(c));
  }

  return { hasPermission, hasAnyPermission, hasAllPermissions, isAdmin };
}
