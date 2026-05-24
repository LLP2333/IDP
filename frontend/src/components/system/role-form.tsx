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

export type RoleFormValues = z.infer<typeof schema>;

interface RoleFormProps {
  formId: string;
  initial?: RoleResp | null;
  onSubmit: (values: RoleFormValues) => void;
}

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
