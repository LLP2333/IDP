"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Select } from "~/components/ui/select";
import type { RoleResp } from "~/lib/api/types";

const schema = z.object({
  name: z.string().min(1, "请输入角色名称").max(64),
  code: z
    .string()
    .min(1, "请输入角色编码")
    .regex(/^[a-zA-Z][a-zA-Z0-9_]{1,63}$/, "必须以字母开头，仅可包含字母、数字、下划线"),
  description: z.string().max(255).optional().or(z.literal("")),
  sort: z
    .number({ message: "排序必须为数字" })
    .int()
    .min(0)
    .max(9999),
  status: z.union([z.literal(0), z.literal(1)]),
});

/**
 * 由 zod schema 推导出的角色表单值类型。
 */
export type RoleFormValues = z.infer<typeof schema>;

interface RoleFormProps {
  /** form 元素的 id，用于在外层 Modal 的 footer 中通过 `form={formId}` 触发提交。 */
  formId: string;
  /** 编辑模式下的初始值；新增时传 `null` 或不传。 */
  initial?: RoleResp | null;
  /** 校验通过后的回调，参数即标准化后的表单值。 */
  onSubmit: (values: RoleFormValues) => void;
}

/**
 * 角色 新增 / 编辑 表单。
 *
 * - 系统内置角色的 code 字段会被禁用，避免误改；
 * - 当 `initial` 变更时，会通过 `reset` 同步表单值。
 */
export function RoleForm({ formId, initial, onSubmit }: RoleFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<RoleFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: "",
      code: "",
      description: "",
      sort: 999,
      status: 1,
    },
  });

  useEffect(() => {
    if (initial) {
      reset({
        name: initial.name,
        code: initial.code,
        description: initial.description ?? "",
        sort: initial.sort,
        status: initial.status === 0 ? 0 : 1,
      });
    } else {
      reset({ name: "", code: "", description: "", sort: 999, status: 1 });
    }
  }, [initial, reset]);

  return (
    <form
      id={formId}
      className="grid grid-cols-1 gap-4 sm:grid-cols-2"
      onSubmit={handleSubmit(onSubmit)}
    >
      <FormField label="角色名称" required error={errors.name?.message}>
        <Input {...register("name")} invalid={!!errors.name} />
      </FormField>
      <FormField label="角色编码" required error={errors.code?.message}>
        <Input
          {...register("code")}
          disabled={!!initial?.isSystem}
          invalid={!!errors.code}
          placeholder="如 admin、user"
        />
      </FormField>
      <FormField label="排序" error={errors.sort?.message}>
        <Input
          type="number"
          {...register("sort", { valueAsNumber: true })}
          invalid={!!errors.sort}
        />
      </FormField>
      <FormField label="状态" error={errors.status?.message}>
        <Select {...register("status", { valueAsNumber: true })}>
          <option value={1}>启用</option>
          <option value={0}>禁用</option>
        </Select>
      </FormField>
      <FormField
        label="描述"
        error={errors.description?.message}
        className="sm:col-span-2"
      >
        <Input {...register("description")} invalid={!!errors.description} />
      </FormField>
    </form>
  );
}
