import { http } from "./http";
import type { MenuQuery, MenuReq, MenuResp } from "./types";

/** 菜单模块在后端的统一前缀。 */
const BASE_URL = "/system/menu";

/**
 * 查询菜单平铺列表。
 *
 * 对应后端 {@code GET /system/menu}；按 sort 升序，不做分页。前端用作菜单管理表格的数据源。
 */
export function listMenu(query: MenuQuery = {}) {
  return http.get<MenuResp[]>(BASE_URL, {
    title: query.title,
    path: query.path,
    permission: query.permission,
    status: query.status,
    type: query.type,
  });
}

/**
 * 查询菜单树（含父子结构）。
 *
 * 对应后端 {@code GET /system/menu/tree}；用于角色分配菜单弹窗 / 树形管理表格。
 */
export function getMenuTree(query: MenuQuery = {}) {
  return http.get<MenuResp[]>(`${BASE_URL}/tree`, {
    title: query.title,
    path: query.path,
    permission: query.permission,
    status: query.status,
    type: query.type,
  });
}

/** 按 ID 获取单个菜单详情。 */
export function getMenu(id: number) {
  return http.get<MenuResp>(`${BASE_URL}/${id}`);
}

/** 新增菜单。 */
export function createMenu(req: MenuReq) {
  return http.post<number>(BASE_URL, req);
}

/** 更新菜单。 */
export function updateMenu(id: number, req: MenuReq) {
  return http.put<void>(`${BASE_URL}/${id}`, req);
}

/** 批量删除菜单（仅非内置可删；存在子节点时由后端拒绝）。 */
export function deleteMenu(ids: number[]) {
  return http.del<void>(BASE_URL, { ids });
}

/**
 * 获取指定角色已绑定的菜单 ID 列表（含目录 / 菜单 / 按钮，全量）。
 *
 * 对应后端 {@code GET /system/role/{roleId}/menu}。
 */
export function getRoleMenu(roleId: number) {
  return http.get<number[]>(`/system/role/${roleId}/menu`);
}

/**
 * 设置指定角色的菜单（全量覆盖）；admin 角色不允许通过该接口修改。
 *
 * 对应后端 {@code PUT /system/role/{roleId}/menu}。
 */
export function assignRoleMenu(roleId: number, menuIds: number[]) {
  return http.put<void>(`/system/role/${roleId}/menu`, { menuIds });
}

/**
 * 获取当前登录用户可见的菜单树（type=1/2，type=3 按钮已被后端过滤）。
 *
 * 对应后端 {@code GET /auth/user/route}；用于前端动态侧边栏渲染。
 */
export function getUserRoute() {
  return http.get<MenuResp[]>("/auth/user/route");
}
