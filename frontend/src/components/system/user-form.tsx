"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Select } from "~/components/ui/select";
import type { RoleResp, UserDetailResp } from "~/lib/api/types";

const baseSchema = {
  nickname: z.string().max(64).optional().or(z.literal("")),
  email: z
    .string()
    .max(128)
    .email("邮箱格式不合法")
    .optional()
    .or(z.literal("")),
  phone: z
    .string()
    .regex(/^$|^1[3-9]\d{9}$/, "手机号格式不合法")
    .optional()
    .or(z.literal("")),
  gender: z.union([z.literal(0), z.literal(1), z.literal(2)]),
  description: z.string().max(255).optional().or(z.literal("")),
  status: z.union([z.literal(0), z.literal(1)]),
  roleIds: z.array(z.number()).default([]),
};

const createSchema = z.object({
  username: z
    .string()
    .min(1, "请输入用户名")
    .regex(/^[a-zA-Z][a-zA-Z0-9_]{1,63}$/, "必须以字母开头，仅可包含字母、数字、下划线"),
  password: z.string().min(6, "密码长度需在 6-32 之间").max(32),
  ...baseSchema,
});

const updateSchema = z.object({
  ...baseSchema,
});

/** 新增用户的完整表单值类型（含 username/password）。 */
export type UserCreateValues = z.infer<typeof createSchema>;
/** 修改用户的表单值类型（不含 username/password）。 */
export type UserUpdateValues = z.infer<typeof updateSchema>;

interface UserFormProps {
  /** form 元素的 id，用于通过 `form={formId}` 在外层按钮上触发提交。 */
  formId: string;
  /** 表单模式：`create` 用于新增，`update` 用于编辑。 */
  mode: "create" | "update";
  /** 编辑模式下的初始用户详情；新增模式可不传。 */
  initial?: UserDetailResp | null;
  /** 可选角色列表，用于渲染角色多选 chip。 */
  roles: RoleResp[];
  /** 新增模式的提交回调。 */
  onCreate?: (values: UserCreateValues) => void;
  /** 编辑模式的提交回调。 */
  onUpdate?: (values: UserUpdateValues) => void;
}

/**
 * 用户 新增 / 编辑 表单。
 *
 * - `mode` 决定使用 createSchema 或 updateSchema；
 * - 角色通过 chip 形式多选，状态由 `react-hook-form` 的 `watch/setValue` 维护；
 * - 编辑模式提交时会丢弃 `username/password` 字段，仅回传可修改字段。
 */
export function UserForm({
  formId,
  mode,
  initial,
  roles,
  onCreate,
  onUpdate,
}: UserFormProps) {
  const isCreate = mode === "create";
  const schema = isCreate ? createSchema : updateSchema;

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<UserCreateValues>({
    resolver: zodResolver(schema as unknown as typeof createSchema),
    defaultValues: isCreate
      ? {
          username: "",
          password: "",
          nickname: "",
          email: "",
          phone: "",
          gender: 0,
          description: "",
          status: 1,
          roleIds: [],
        }
      : ({
          nickname: "",
          email: "",
          phone: "",
          gender: 0,
          description: "",
          status: 1,
          roleIds: [],
        } as unknown as UserCreateValues),
  });

  useEffect(() => {
    if (!initial) return;
    if (isCreate) {
      reset({
        username: "",
        password: "",
        nickname: initial.nickname ?? "",
        email: initial.email ?? "",
        phone: initial.phone ?? "",
        gender: (initial.gender ?? 0) as 0 | 1 | 2,
        description: initial.description ?? "",
        status: initial.status === 0 ? 0 : 1,
        roleIds: initial.roleIds ?? [],
      });
    } else {
      reset({
        nickname: initial.nickname ?? "",
        email: initial.email ?? "",
        phone: initial.phone ?? "",
        gender: (initial.gender ?? 0) as 0 | 1 | 2,
        description: initial.description ?? "",
        status: initial.status === 0 ? 0 : 1,
        roleIds: initial.roleIds ?? [],
      } as unknown as UserCreateValues);
    }
  }, [initial, isCreate, reset]);

  const selectedRoleIds = watch("roleIds") ?? [];

  /**
   * 切换某个角色的选中状态：已选则移除，未选则加入。
   * @param roleId 角色 ID
   */
  const toggleRole = (roleId: number) => {
    const current = new Set(selectedRoleIds);
    if (current.has(roleId)) current.delete(roleId);
    else current.add(roleId);
    setValue("roleIds", Array.from(current));
  };

  /**
   * 表单提交分发：根据 `mode` 调不同回调。
   * 编辑模式下显式丢弃 `username/password`，避免被误传给后端 update 接口。
   */
  const submit = (values: UserCreateValues) => {
    if (isCreate) {
      onCreate?.(values);
    } else {
      const {
        username: _u,
        password: _p,
        ...rest
      } = values;
      void _u;
      void _p;
      onUpdate?.(rest);
    }
  };

  return (
    <form
      id={formId}
      className="grid grid-cols-1 gap-4 sm:grid-cols-2"
      onSubmit={handleSubmit(submit)}
    >
      {isCreate ? (
        <>
          <FormField label="用户名" required error={errors.username?.message}>
            <Input {...register("username")} invalid={!!errors.username} />
          </FormField>
          <FormField label="初始密码" required error={errors.password?.message}>
            <Input
              type="password"
              autoComplete="new-password"
              {...register("password")}
              invalid={!!errors.password}
            />
          </FormField>
        </>
      ) : null}
      <FormField label="昵称" error={errors.nickname?.message}>
        <Input {...register("nickname")} invalid={!!errors.nickname} />
      </FormField>
      <FormField label="邮箱" error={errors.email?.message}>
        <Input type="email" {...register("email")} invalid={!!errors.email} />
      </FormField>
      <FormField label="手机号" error={errors.phone?.message}>
        <Input {...register("phone")} invalid={!!errors.phone} />
      </FormField>
      <FormField label="性别" error={errors.gender?.message}>
        <Select {...register("gender", { valueAsNumber: true })}>
          <option value={0}>未知</option>
          <option value={1}>男</option>
          <option value={2}>女</option>
        </Select>
      </FormField>
      <FormField label="状态" error={errors.status?.message}>
        <Select {...register("status", { valueAsNumber: true })}>
          <option value={1}>启用</option>
          <option value={0}>禁用</option>
        </Select>
      </FormField>
      <FormField
        label="备注"
        error={errors.description?.message}
        className="sm:col-span-2"
      >
        <Input {...register("description")} invalid={!!errors.description} />
      </FormField>
      <FormField label="角色" className="sm:col-span-2">
        <div className="flex flex-wrap gap-2">
          {roles.length === 0 ? (
            <span className="text-xs text-zinc-400">暂无可分配角色</span>
          ) : (
            roles.map((role) => {
              const checked = selectedRoleIds.includes(role.id);
              return (
                <button
                  type="button"
                  key={role.id}
                  onClick={() => toggleRole(role.id)}
                  className={
                    "rounded-full border px-3 py-1 text-xs transition-colors " +
                    (checked
                      ? "border-blue-500 bg-blue-50 text-blue-700"
                      : "border-zinc-300 text-zinc-600 hover:border-zinc-400")
                  }
                >
                  {role.name}
                </button>
              );
            })
          )}
        </div>
      </FormField>
    </form>
  );
}
