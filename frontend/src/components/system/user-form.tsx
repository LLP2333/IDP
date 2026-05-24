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

export type UserCreateValues = z.infer<typeof createSchema>;
export type UserUpdateValues = z.infer<typeof updateSchema>;

interface UserFormProps {
  formId: string;
  mode: "create" | "update";
  initial?: UserDetailResp | null;
  roles: RoleResp[];
  onCreate?: (values: UserCreateValues) => void;
  onUpdate?: (values: UserUpdateValues) => void;
}

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

  const toggleRole = (roleId: number) => {
    const current = new Set(selectedRoleIds);
    if (current.has(roleId)) current.delete(roleId);
    else current.add(roleId);
    setValue("roleIds", Array.from(current));
  };

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
