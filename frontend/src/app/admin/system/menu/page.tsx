"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Banknote,
  BarChart3,
  Bell,
  Boxes,
  ChevronDown,
  ChevronRight,
  Database,
  ExternalLink,
  FileText,
  Folder,
  Globe,
  KeyRound,
  LayoutDashboard,
  type LucideIcon,
  LogIn,
  Menu as MenuIcon,
  Pencil,
  Plus,
  Settings,
  Shield,
  ShieldCheck,
  Trash2,
  Users,
} from "lucide-react";
import { Fragment, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";

import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { Select } from "~/components/ui/select";
import { HttpError } from "~/lib/api/http";
import {
  createMenu,
  deleteMenu,
  getMenuTree,
  updateMenu,
} from "~/lib/api/menu";
import type { MenuReq, MenuResp, MenuType } from "~/lib/api/types";
import { usePermission } from "~/lib/hooks/use-permission";
import { cn } from "~/lib/utils";

/**
 * 与侧边栏共享一份的 lucide 图标映射；未命中时退回 {@link Folder}，
 * 字段为空则不渲染图标占位（让标题更靠左对齐）。
 */
const ICON_MAP: Record<string, LucideIcon> = {
  users: Users,
  user: Users,
  "shield-check": ShieldCheck,
  shield: Shield,
  menu: MenuIcon,
  settings: Settings,
  setting: Settings,
  "key-round": KeyRound,
  key: KeyRound,
  "log-in": LogIn,
  login: LogIn,
  dashboard: LayoutDashboard,
  layout: LayoutDashboard,
  folder: Folder,
  boxes: Boxes,
  database: Database,
  globe: Globe,
  bell: Bell,
  file: FileText,
  banknote: Banknote,
  chart: BarChart3,
  external: ExternalLink,
};

function resolveIcon(icon: string | null | undefined): LucideIcon | null {
  if (!icon) return null;
  return ICON_MAP[icon.trim().toLowerCase()] ?? Folder;
}

/**
 * 菜单管理弹窗内部表单结构（受控字段）。
 *
 * 字段联动：
 * - `type=1` 目录：强制 `component=Layout`，隐藏 keep-alive 字段；
 * - `type=2` 菜单：必填 `component`；可勾选外链；
 * - `type=3` 按钮：隐藏路由 / 组件相关字段，必填 `permission`。
 */
interface FormValues {
  title: string;
  parentId: number;
  type: MenuType;
  path: string;
  name: string;
  component: string;
  redirect: string;
  icon: string;
  isExternal: boolean;
  isCache: boolean;
  isHidden: boolean;
  permission: string;
  sort: number;
  status: number;
  description: string;
}

const DEFAULT_FORM: FormValues = {
  title: "",
  parentId: 0,
  type: 2,
  path: "",
  name: "",
  component: "",
  redirect: "",
  icon: "",
  isExternal: false,
  isCache: false,
  isHidden: false,
  permission: "",
  sort: 999,
  status: 1,
  description: "",
};

function typeLabel(type: MenuType) {
  switch (type) {
    case 1:
      return "目录";
    case 2:
      return "菜单";
    case 3:
      return "按钮";
  }
}

function typeTone(type: MenuType): "info" | "default" | "success" {
  switch (type) {
    case 1:
      return "info";
    case 2:
      return "success";
    case 3:
      return "default";
  }
}

/**
 * 按搜索条件过滤菜单树（标题 / 路径 / 权限码任一命中）。
 *
 * 命中的节点会保留全部祖先链；未命中且无命中子节点的节点会被丢弃。
 */
function filterTree(
  nodes: MenuResp[],
  keyword: string,
  field: "title" | "path" | "permission",
): MenuResp[] {
  if (!keyword) return nodes;
  const lower = keyword.toLowerCase();
  function walk(node: MenuResp): MenuResp | null {
    const value = (node[field] as string | null | undefined) ?? "";
    const selfHit = value.toLowerCase().includes(lower);
    const matched = (node.children ?? [])
      .map(walk)
      .filter((c): c is MenuResp => c !== null);
    if (selfHit || matched.length > 0) {
      return { ...node, children: matched };
    }
    return null;
  }
  return nodes.map(walk).filter((n): n is MenuResp => n !== null);
}

/**
 * 菜单管理页：树形表格 + 新增 / 编辑弹窗。
 *
 * 数据流：
 * - 列表：`useQuery(["menu","tree"], getMenuTree)`；
 * - 写操作：`useMutation` + 失败时弹 toast 并按业务码失败回滚；
 * - 增删改成功后 invalidate 缓存触发自动重拉。
 */
export default function MenuPage() {
  const { hasPermission } = usePermission();
  const queryClient = useQueryClient();
  const queryKey = ["menu", "tree"] as const;

  const treeQuery = useQuery({ queryKey, queryFn: () => getMenuTree() });

  const [searchField, setSearchField] = useState<"title" | "path" | "permission">(
    "title",
  );
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<MenuResp | null>(null);
  const [form, setForm] = useState<FormValues>(DEFAULT_FORM);

  const [expanded, setExpanded] = useState<Set<number>>(new Set());

  useEffect(() => {
    if (editing) {
      setForm({
        title: editing.title,
        parentId: editing.parentId,
        type: editing.type,
        path: editing.path ?? "",
        name: editing.name ?? "",
        component: editing.component ?? "",
        redirect: editing.redirect ?? "",
        icon: editing.icon ?? "",
        isExternal: editing.isExternal,
        isCache: editing.isCache,
        isHidden: editing.isHidden,
        permission: editing.permission ?? "",
        sort: editing.sort,
        status: editing.status,
        description: editing.description ?? "",
      });
    } else {
      setForm(DEFAULT_FORM);
    }
  }, [editing]);

  // 默认展开所有顶级节点，方便看到二级菜单
  useEffect(() => {
    if (treeQuery.data) {
      const tree = treeQuery.data;
      setExpanded((prev) => {
        if (prev.size > 0) return prev;
        const next = new Set<number>();
        tree.forEach((n) => next.add(n.id));
        return next;
      });
    }
  }, [treeQuery.data]);

  const filteredTree = useMemo(
    () => filterTree(treeQuery.data ?? [], keyword, searchField),
    [treeQuery.data, keyword, searchField],
  );

  const createMutation = useMutation({
    mutationFn: (req: MenuReq) => createMenu(req),
    onSuccess: () => {
      toast.success("已新增菜单");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, req }: { id: number; req: MenuReq }) => updateMenu(id, req),
    onSuccess: () => {
      toast.success("已更新菜单");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });
  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteMenu([id]),
    onSuccess: () => {
      toast.success("已删除");
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) => toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  /**
   * 把表单值映射为后端请求体；按 type 清理无意义字段，避免反向影响后端校验。
   */
  function buildReq(): MenuReq {
    const isButton = form.type === 3;
    return {
      title: form.title.trim(),
      parentId: form.parentId,
      type: form.type,
      path: isButton ? null : form.path.trim() || null,
      name: isButton ? null : form.name.trim() || null,
      component: isButton ? null : form.component.trim() || null,
      redirect: isButton ? null : form.redirect.trim() || null,
      icon: isButton ? null : form.icon.trim() || null,
      isExternal: !isButton && form.isExternal,
      isCache: !isButton && form.isCache,
      isHidden: !isButton && form.isHidden,
      permission: form.permission.trim() || null,
      sort: form.sort,
      status: form.status,
      description: form.description.trim() || null,
    };
  }

  function handleSubmit() {
    const req = buildReq();
    if (editing) updateMutation.mutate({ id: editing.id, req });
    else createMutation.mutate(req);
  }

  /** 递归渲染一行（树形）。使用 {@link Fragment} + 显式 key，避免 React 报缺 key 警告导致行复用错乱。 */
  function renderRow(node: MenuResp, depth: number): React.ReactNode {
    const hasChildren = (node.children?.length ?? 0) > 0;
    const isOpen = expanded.has(node.id);
    const canAddChild = node.type !== 3;
    const Icon = resolveIcon(node.icon);
    return (
      <Fragment key={node.id}>
        <tr className="group border-t border-zinc-100 hover:bg-zinc-50/60">
          <td className="px-3 py-2 align-middle">
            <div
              className="flex items-center gap-2 whitespace-nowrap"
              style={{ paddingLeft: depth * 16 }}
            >
              {hasChildren ? (
                <button
                  type="button"
                  className="text-zinc-500"
                  onClick={() =>
                    setExpanded((prev) => {
                      const next = new Set(prev);
                      if (next.has(node.id)) next.delete(node.id);
                      else next.add(node.id);
                      return next;
                    })
                  }
                  aria-label={isOpen ? "折叠" : "展开"}
                >
                  {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                </button>
              ) : (
                <span className="inline-block w-3.5" />
              )}
              {Icon ? <Icon size={14} className="text-zinc-400" /> : null}
              <span className="font-medium text-zinc-800">{node.title}</span>
              {node.isSystem ? <Badge tone="info">系统</Badge> : null}
            </div>
          </td>
          <td className="px-3 py-2 align-middle">
            <Badge tone={typeTone(node.type)}>{typeLabel(node.type)}</Badge>
          </td>
          <td className="px-3 py-2 align-middle">
            {node.status === 1 ? (
              <Badge tone="success">启用</Badge>
            ) : (
              <Badge tone="danger">禁用</Badge>
            )}
          </td>
          <td className="px-3 py-2 align-middle text-center">{node.sort}</td>
          <td className="px-3 py-2 align-middle text-xs text-zinc-500" title={node.path ?? undefined}>
            <span className="block truncate whitespace-nowrap">{node.path ?? "—"}</span>
          </td>
          <td className="px-3 py-2 align-middle text-xs text-zinc-500" title={node.name ?? undefined}>
            <span className="block truncate whitespace-nowrap">{node.name ?? "—"}</span>
          </td>
          <td className="px-3 py-2 align-middle text-xs text-zinc-500" title={node.component ?? undefined}>
            <span className="block truncate whitespace-nowrap">{node.component ?? "—"}</span>
          </td>
          <td className="px-3 py-2 align-middle text-xs text-zinc-500" title={node.permission ?? undefined}>
            <span className="block truncate whitespace-nowrap">{node.permission ?? "—"}</span>
          </td>
          <td className="px-3 py-2 align-middle">
            <div className="flex gap-1 text-xs text-zinc-500">
              {node.isExternal ? <span>外链</span> : null}
              {node.isHidden ? <span>· 隐藏</span> : null}
              {node.isCache ? <span>· 缓存</span> : null}
              {!node.isExternal && !node.isHidden && !node.isCache ? "—" : null}
            </div>
          </td>
          <td className="sticky right-0 z-10 bg-white px-3 py-2 align-middle shadow-[-8px_0_12px_-12px_rgba(0,0,0,0.35)] group-hover:bg-zinc-50">
            <div className="flex flex-nowrap items-center justify-start gap-1 whitespace-nowrap">
              {hasPermission("system:menu:add") && canAddChild ? (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setEditing(null);
                    setForm({
                      ...DEFAULT_FORM,
                      parentId: node.id,
                      type: node.type === 1 ? 2 : 3,
                    });
                    setOpen(true);
                  }}
                >
                  <Plus size={14} /> 新增下级
                </Button>
              ) : null}
              {hasPermission("system:menu:update") ? (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setEditing(node);
                    setOpen(true);
                  }}
                >
                  <Pencil size={14} /> 编辑
                </Button>
              ) : null}
              {hasPermission("system:menu:delete") ? (
                <Button
                  size="sm"
                  variant="ghost"
                  className="!text-red-600 hover:!bg-red-50"
                  disabled={node.isSystem}
                  onClick={() => {
                    if (window.confirm(`确认删除「${node.title}」？`))
                      deleteMutation.mutate(node.id);
                  }}
                >
                  <Trash2 size={14} /> 删除
                </Button>
              ) : null}
            </div>
          </td>
        </tr>
        {hasChildren && isOpen
          ? node.children!.map((c) => renderRow(c, depth + 1))
          : null}
      </Fragment>
    );
  }

  const isButton = form.type === 3;
  const isDirectory = form.type === 1;

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">菜单管理</h2>
          <p className="mt-1 text-sm text-zinc-500">
            维护目录 / 菜单 / 按钮三层结构，按钮节点的 permission
            字段即为接口鉴权使用的权限码。
          </p>
        </div>
        {hasPermission("system:menu:add") ? (
          <Button
            onClick={() => {
              setEditing(null);
              setForm(DEFAULT_FORM);
              setOpen(true);
            }}
          >
            <Plus size={14} /> 新增菜单
          </Button>
        ) : null}
      </div>

      <div className="flex flex-wrap items-center gap-2 rounded-md border border-zinc-200 bg-white p-3">
        <Select
          className="!w-32"
          value={searchField}
          onChange={(e) =>
            setSearchField(e.target.value as "title" | "path" | "permission")
          }
        >
          <option value="title">按标题</option>
          <option value="path">按路由</option>
          <option value="permission">按权限标识</option>
        </Select>
        <Input
          className="!w-56"
          placeholder="输入关键字回车搜索"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") setKeyword(keywordInput.trim());
          }}
        />
        <Button
          size="sm"
          variant="outline"
          onClick={() => setKeyword(keywordInput.trim())}
        >
          搜索
        </Button>
        <Button
          size="sm"
          variant="ghost"
          onClick={() => {
            setKeyword("");
            setKeywordInput("");
          }}
        >
          重置
        </Button>
      </div>

      <div className="min-h-0 flex-1 overflow-auto rounded-md border border-zinc-200 bg-white">
        <table className="w-full min-w-[1610px] table-fixed text-sm">
          <colgroup>
            <col className="w-[240px]" />
            <col className="w-[80px]" />
            <col className="w-[80px]" />
            <col className="w-[70px]" />
            <col className="w-[160px]" />
            <col className="w-[140px]" />
            <col className="w-[200px]" />
            <col className="w-[260px]" />
            <col className="w-[120px]" />
            <col className="w-[260px]" />
          </colgroup>
          <thead className="sticky top-0 z-20 bg-zinc-50 text-zinc-600">
            <tr>
              <th
                className="px-3 py-2 text-left font-medium whitespace-nowrap"
                style={{ minWidth: 220 }}
              >
                菜单标题
              </th>
              <th className="px-3 py-2 text-left font-medium whitespace-nowrap" style={{ width: 80 }}>
                类型
              </th>
              <th className="px-3 py-2 text-left font-medium whitespace-nowrap" style={{ width: 80 }}>
                状态
              </th>
              <th
                className="px-3 py-2 text-center font-medium whitespace-nowrap"
                style={{ width: 70 }}
              >
                排序
              </th>
              <th className="px-3 py-2 text-left font-medium whitespace-nowrap">路由地址</th>
              <th className="px-3 py-2 text-left font-medium whitespace-nowrap">组件名称</th>
              <th className="px-3 py-2 text-left font-medium whitespace-nowrap">组件路径</th>
              <th className="px-3 py-2 text-left font-medium whitespace-nowrap">权限标识</th>
              <th className="px-3 py-2 text-left font-medium whitespace-nowrap" style={{ width: 120 }}>
                标记
              </th>
              <th
                className="sticky right-0 z-30 bg-zinc-50 px-3 py-2 text-left font-medium whitespace-nowrap shadow-[-8px_0_12px_-12px_rgba(0,0,0,0.35)]"
                style={{ width: 260 }}
              >
                操作
              </th>
            </tr>
          </thead>
          <tbody>
            {treeQuery.isLoading ? (
              <tr>
                <td colSpan={10} className="px-3 py-10 text-center text-zinc-400">
                  加载中…
                </td>
              </tr>
            ) : filteredTree.length === 0 ? (
              <tr>
                <td colSpan={10} className="px-3 py-10 text-center text-zinc-400">
                  暂无菜单数据
                </td>
              </tr>
            ) : (
              filteredTree.map((n) => renderRow(n, 0))
            )}
          </tbody>
        </table>
      </div>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? "编辑菜单" : "新增菜单"}
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button
              loading={createMutation.isPending || updateMutation.isPending}
              onClick={handleSubmit}
            >
              保存
            </Button>
          </>
        }
      >
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          <FormField label="标题" required>
            <Input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
          </FormField>
          <FormField label="父节点 ID（0 表示顶级）">
            <Input
              type="number"
              value={form.parentId}
              onChange={(e) =>
                setForm({ ...form, parentId: Number(e.target.value) || 0 })
              }
            />
          </FormField>
          <FormField label="类型" required>
            <Select
              value={String(form.type)}
              onChange={(e) => {
                const nextType = Number(e.target.value) as MenuType;
                setForm((prev) => ({
                  ...prev,
                  type: nextType,
                  ...(nextType === 1 ? { component: "Layout" } : {}),
                  ...(nextType === 3
                    ? {
                        path: "",
                        name: "",
                        component: "",
                        redirect: "",
                        icon: "",
                        isExternal: false,
                        isCache: false,
                        isHidden: false,
                      }
                    : {}),
                }));
              }}
            >
              <option value="1">目录</option>
              <option value="2">菜单</option>
              <option value="3">按钮</option>
            </Select>
          </FormField>
          <FormField label="排序">
            <Input
              type="number"
              value={form.sort}
              onChange={(e) =>
                setForm({ ...form, sort: Number(e.target.value) || 0 })
              }
            />
          </FormField>
          {!isButton ? (
            <>
              <FormField label="是否外链">
                <Select
                  value={form.isExternal ? "1" : "0"}
                  onChange={(e) =>
                    setForm({ ...form, isExternal: e.target.value === "1" })
                  }
                >
                  <option value="0">否</option>
                  <option value="1">是（path 必须以 http(s) 开头）</option>
                </Select>
              </FormField>
              <FormField label="路由地址" required>
                <Input
                  value={form.path}
                  placeholder={form.isExternal ? "https://…" : "/system/user"}
                  onChange={(e) => setForm({ ...form, path: e.target.value })}
                />
              </FormField>
              <FormField label="组件名称">
                <Input
                  value={form.name}
                  placeholder="User"
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                />
              </FormField>
              <FormField
                label={isDirectory ? "组件路径（目录默认 Layout）" : "组件路径"}
                required={!isDirectory && !form.isExternal}
              >
                <Input
                  value={form.component}
                  placeholder="system/user/index"
                  onChange={(e) => setForm({ ...form, component: e.target.value })}
                  disabled={isDirectory}
                />
              </FormField>
              <FormField label="重定向地址">
                <Input
                  value={form.redirect}
                  placeholder="/system/user"
                  onChange={(e) => setForm({ ...form, redirect: e.target.value })}
                />
              </FormField>
              <FormField label="图标标识">
                <Input
                  value={form.icon}
                  placeholder="users / settings / shield-check"
                  onChange={(e) => setForm({ ...form, icon: e.target.value })}
                />
              </FormField>
              <FormField label="是否隐藏">
                <Select
                  value={form.isHidden ? "1" : "0"}
                  onChange={(e) =>
                    setForm({ ...form, isHidden: e.target.value === "1" })
                  }
                >
                  <option value="0">否</option>
                  <option value="1">是</option>
                </Select>
              </FormField>
              <FormField label="启用 keep-alive">
                <Select
                  value={form.isCache ? "1" : "0"}
                  onChange={(e) =>
                    setForm({ ...form, isCache: e.target.value === "1" })
                  }
                >
                  <option value="0">否</option>
                  <option value="1">是</option>
                </Select>
              </FormField>
            </>
          ) : null}
          <FormField
            label="权限标识"
            required={isButton}
            className={cn(!isButton && "md:col-span-1")}
          >
            <Input
              value={form.permission}
              placeholder="system:user:add"
              onChange={(e) => setForm({ ...form, permission: e.target.value })}
            />
          </FormField>
          <FormField label="状态">
            <Select
              value={String(form.status)}
              onChange={(e) =>
                setForm({ ...form, status: Number(e.target.value) })
              }
            >
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </Select>
          </FormField>
          <FormField label="描述" className="md:col-span-2">
            <Input
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </FormField>
        </div>
      </Modal>
    </div>
  );
}
