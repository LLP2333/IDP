"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";

const schema = z.object({
  newPassword: z.string().min(6, "密码长度需在 6-32 之间").max(32),
  confirm: z.string(),
}).refine((v) => v.newPassword === v.confirm, {
  message: "两次输入的密码不一致",
  path: ["confirm"],
});

export type PasswordResetValues = z.infer<typeof schema>;

export function UserPasswordForm({
  formId,
  onSubmit,
}: {
  formId: string;
  onSubmit: (values: { newPassword: string }) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<PasswordResetValues>({
    resolver: zodResolver(schema),
    defaultValues: { newPassword: "", confirm: "" },
  });

  return (
    <form
      id={formId}
      className="flex flex-col gap-4"
      onSubmit={handleSubmit((v) => onSubmit({ newPassword: v.newPassword }))}
    >
      <FormField label="新密码" required error={errors.newPassword?.message}>
        <Input
          type="password"
          autoComplete="new-password"
          {...register("newPassword")}
          invalid={!!errors.newPassword}
        />
      </FormField>
      <FormField label="确认密码" required error={errors.confirm?.message}>
        <Input
          type="password"
          autoComplete="new-password"
          {...register("confirm")}
          invalid={!!errors.confirm}
        />
      </FormField>
    </form>
  );
}
