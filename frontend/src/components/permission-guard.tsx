"use client";

import type { ReactNode } from "react";

import { usePermission } from "~/lib/hooks/use-permission";

/** PermissionGuard 组件 props。 */
export interface PermissionGuardProps {
  /** 需要校验的权限码集合。 */
  codes: string[];
  /** 校验模式：any=任一即可（默认），all=必须全部拥有。 */
  mode?: "any" | "all";
  /** 校验失败时渲染的内容，默认不渲染。 */
  fallback?: ReactNode;
  children: ReactNode;
}

/**
 * 按权限码包裹子节点的守卫组件。
 *
 * 使用示例：
 * ```tsx
 * <PermissionGuard codes={["system:user:add"]}>
 *   <Button>新增用户</Button>
 * </PermissionGuard>
 * ```
 *
 * 与后端 {@code @HasPermission} 行为一致；admin 角色直通。
 */
export function PermissionGuard({ codes, mode = "any", fallback = null, children }: PermissionGuardProps) {
  const { hasAnyPermission, hasAllPermissions } = usePermission();
  const allowed = mode === "all" ? hasAllPermissions(codes) : hasAnyPermission(codes);
  return <>{allowed ? children : fallback}</>;
}
