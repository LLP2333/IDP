"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Select } from "~/components/ui/select";
import type { UserInfo } from "~/lib/api/types";

/**
 * 个人中心 “基本信息” 表单的 Zod Schema。
 *
 * - 与后端 {@code UserBasicInfoUpdateReq} 的 Bean Validation 保持一致；
 * - {@code email} / {@code phone} 允许空字符串，表示用户主动清除该字段，由调用方
 *   在提交前决定是发送 `""`（清空）还是不发送（保持原值）。
 */
const schema = z.object({
  nickname: z
    .string()
    .min(1, "请输入昵称")
    .max(64, "昵称长度不能超过 64"),
  email: z
    .string()
    .max(128, "邮箱长度不能超过 128")
    .refine(
      (v) => v === "" || /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v),
      "邮箱格式不合法",
    ),
  phone: z
    .string()
    .regex(/^$|^1[3-9]\d{9}$/, "手机号格式不合法"),
  gender: z.union([z.literal(0), z.literal(1), z.literal(2)]),
});

/** 由 schema 推导出的表单值类型。 */
export type ProfileBasicInfoValues = z.infer<typeof schema>;

interface ProfileBasicInfoFormProps {
  /** form 元素的 id，用于在 Modal footer 触发 `type="submit"`。 */
  formId: string;
  /** 当前登录用户的最新信息，用于回填初值。 */
  initial: UserInfo | null | undefined;
  /** 校验通过后的提交回调；调用方应据此发起 PUT /system/user/profile。 */
  onSubmit: (values: ProfileBasicInfoValues) => void;
}

/**
 * 个人中心 “修改基本信息” 表单。
 *
 * 字段集合刻意比 {@link import('./user-form').UserForm} 更窄：仅包含昵称、邮箱、
 * 手机、性别。状态、角色、备注等敏感字段需走管理员侧的用户管理接口。
 *
 * - 由 `react-hook-form` + `zod` 校验；
 * - `initial` 变化时通过 `reset` 同步回填，避免用户切换账号后看到旧数据。
 */
export function ProfileBasicInfoForm({
  formId,
  initial,
  onSubmit,
}: ProfileBasicInfoFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProfileBasicInfoValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      nickname: initial?.nickname ?? "",
      email: initial?.email ?? "",
      phone: initial?.phone ?? "",
      gender: ((initial?.gender ?? 0) as 0 | 1 | 2),
    },
  });

  useEffect(() => {
    reset({
      nickname: initial?.nickname ?? "",
      email: initial?.email ?? "",
      phone: initial?.phone ?? "",
      gender: ((initial?.gender ?? 0) as 0 | 1 | 2),
    });
  }, [initial, reset]);

  return (
    <form
      id={formId}
      className="grid grid-cols-1 gap-4 sm:grid-cols-2"
      onSubmit={handleSubmit(onSubmit)}
    >
      <FormField label="昵称" required error={errors.nickname?.message}>
        <Input {...register("nickname")} invalid={!!errors.nickname} />
      </FormField>
      <FormField label="性别" error={errors.gender?.message}>
        <Select {...register("gender", { valueAsNumber: true })}>
          <option value={0}>未知</option>
          <option value={1}>男</option>
          <option value={2}>女</option>
        </Select>
      </FormField>
      <FormField label="邮箱" error={errors.email?.message} hint="留空表示清除">
        <Input type="email" {...register("email")} invalid={!!errors.email} />
      </FormField>
      <FormField label="手机号" error={errors.phone?.message} hint="留空表示清除">
        <Input {...register("phone")} invalid={!!errors.phone} />
      </FormField>
    </form>
  );
}
