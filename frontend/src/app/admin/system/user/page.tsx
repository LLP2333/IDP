"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { KeyRound, Pencil, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import {
  UserForm,
  type UserCreateValues,
  type UserUpdateValues,
} from "~/components/system/user-form";
import { UserPasswordForm } from "~/components/system/user-password-form";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { DataTable, Pagination, type ColumnDef } from "~/components/ui/data-table";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { Select } from "~/components/ui/select";
import { HttpError } from "~/lib/api/http";
import { listAllRole } from "~/lib/api/role";
import {
  addUser,
  deleteUser,
  getUser,
  listUser,
  resetUserPassword,
  updateUser,
} from "~/lib/api/user";
import type { UserDetailResp, UserResp } from "~/lib/api/types";

const FORM_ID = "user-form";
const PWD_FORM_ID = "user-password-form";

/** 把空字符串转为 undefined，避免后端校验报错。 */
function emptyToUndefined(v: string | undefined): string | undefined {
  if (v === undefined) return undefined;
  if (v.trim() === "") return undefined;
  return v;
}

export default function UserPage() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(1);
  const [usernameInput, setUsernameInput] = useState("");
  const [username, setUsername] = useState("");
  const [status, setStatus] = useState<number | "">("");

  const [open, setOpen] = useState(false);
  const [pwdOpen, setPwdOpen] = useState(false);
  const [editing, setEditing] = useState<UserDetailResp | null>(null);
  const [pwdTarget, setPwdTarget] = useState<UserResp | null>(null);

  const listQuery = useQuery({
    queryKey: ["user", "list", { page, username, status }],
    queryFn: () =>
      listUser({
        page,
        size: 10,
        username: username || undefined,
        status: status === "" ? undefined : status,
      }),
  });

  const rolesQuery = useQuery({
    queryKey: ["role", "list-all"],
    queryFn: () => listAllRole(),
  });

  const createMutation = useMutation({
    mutationFn: (values: UserCreateValues) =>
      addUser({
        username: values.username,
        password: values.password,
        nickname: emptyToUndefined(values.nickname),
        email: emptyToUndefined(values.email),
        phone: emptyToUndefined(values.phone),
        gender: values.gender,
        description: emptyToUndefined(values.description),
        status: values.status,
        roleIds: values.roleIds,
      }),
    onSuccess: () => {
      toast.success("已新增用户");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["user", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, values }: { id: number; values: UserUpdateValues }) =>
      updateUser(id, {
        nickname: emptyToUndefined(values.nickname),
        email: emptyToUndefined(values.email),
        phone: emptyToUndefined(values.phone),
        gender: values.gender,
        description: emptyToUndefined(values.description),
        status: values.status,
        roleIds: values.roleIds,
      }),
    onSuccess: () => {
      toast.success("已更新用户");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["user", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteUser([id]),
    onSuccess: () => {
      toast.success("已删除");
      void queryClient.invalidateQueries({ queryKey: ["user", "list"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const resetPwdMutation = useMutation({
    mutationFn: ({ id, newPassword }: { id: number; newPassword: string }) =>
      resetUserPassword(id, { newPassword }),
    onSuccess: () => {
      toast.success("已重置密码");
      setPwdOpen(false);
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const openEdit = async (id: number) => {
    try {
      const detail = await getUser(id);
      setEditing(detail);
      setOpen(true);
    } catch (err) {
      toast.error(err instanceof HttpError ? err.message : "加载失败");
    }
  };

  const columns: ColumnDef<UserResp>[] = [
    { key: "id", title: "ID", width: "60px" },
    { key: "username", title: "用户名" },
    { key: "nickname", title: "昵称", render: (r) => r.nickname ?? "—" },
    { key: "email", title: "邮箱", render: (r) => r.email ?? "—" },
    { key: "phone", title: "手机号", render: (r) => r.phone ?? "—" },
    {
      key: "status",
      title: "状态",
      width: "80px",
      render: (row) =>
        row.status === 1 ? (
          <Badge tone="success">启用</Badge>
        ) : (
          <Badge tone="danger">禁用</Badge>
        ),
    },
    {
      key: "isSystem",
      title: "类型",
      width: "80px",
      render: (row) =>
        row.isSystem ? (
          <Badge tone="info">系统</Badge>
        ) : (
          <Badge tone="default">普通</Badge>
        ),
    },
    {
      key: "actions",
      title: "操作",
      width: "260px",
      align: "right",
      render: (row) => (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="ghost" onClick={() => void openEdit(row.id)}>
            <Pencil size={14} /> 编辑
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              setPwdTarget(row);
              setPwdOpen(true);
            }}
          >
            <KeyRound size={14} /> 重置密码
          </Button>
          <Button
            size="sm"
            variant="ghost"
            disabled={row.isSystem}
            onClick={() => {
              if (window.confirm(`确认删除用户「${row.username}」？`)) {
                deleteMutation.mutate(row.id);
              }
            }}
            className="!text-red-600 hover:!bg-red-50"
          >
            <Trash2 size={14} /> 删除
          </Button>
        </div>
      ),
    },
  ];

  const data = listQuery.data;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">用户管理</h2>
          <p className="mt-1 text-sm text-zinc-500">
            维护系统用户信息、角色分配与密码重置。
          </p>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setOpen(true);
          }}
        >
          <Plus size={14} />
          新增用户
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-2 rounded-md border border-zinc-200 bg-white p-3">
        <Input
          className="!w-56"
          placeholder="按用户名搜索"
          value={usernameInput}
          onChange={(e) => setUsernameInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              setPage(1);
              setUsername(usernameInput);
            }
          }}
        />
        <Select
          className="!w-32"
          value={status}
          onChange={(e) => {
            const v = e.target.value;
            setStatus(v === "" ? "" : Number(v));
            setPage(1);
          }}
        >
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </Select>
        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            setPage(1);
            setUsername(usernameInput);
          }}
        >
          搜索
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => {
            setUsername("");
            setUsernameInput("");
            setStatus("");
            setPage(1);
          }}
        >
          重置
        </Button>
      </div>

      <DataTable<UserResp>
        columns={columns}
        data={data?.list ?? []}
        rowKey={(row) => row.id}
        loading={listQuery.isLoading}
      />

      {data ? (
        <Pagination
          page={data.page}
          size={data.size}
          total={data.total}
          onPageChange={setPage}
        />
      ) : null}

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? "编辑用户" : "新增用户"}
        size="lg"
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button
              type="submit"
              form={FORM_ID}
              loading={createMutation.isPending || updateMutation.isPending}
            >
              保存
            </Button>
          </>
        }
      >
        <UserForm
          formId={FORM_ID}
          mode={editing ? "update" : "create"}
          initial={editing}
          roles={rolesQuery.data ?? []}
          onCreate={(values) => createMutation.mutate(values)}
          onUpdate={(values) => {
            if (editing) updateMutation.mutate({ id: editing.id, values });
          }}
        />
      </Modal>

      <Modal
        open={pwdOpen}
        onClose={() => setPwdOpen(false)}
        title={`重置密码 - ${pwdTarget?.username ?? ""}`}
        footer={
          <>
            <Button variant="outline" onClick={() => setPwdOpen(false)}>
              取消
            </Button>
            <Button
              type="submit"
              form={PWD_FORM_ID}
              loading={resetPwdMutation.isPending}
            >
              确认
            </Button>
          </>
        }
      >
        <UserPasswordForm
          formId={PWD_FORM_ID}
          onSubmit={({ newPassword }) => {
            if (pwdTarget) {
              resetPwdMutation.mutate({ id: pwdTarget.id, newPassword });
            }
          }}
        />
      </Modal>
    </div>
  );
}
