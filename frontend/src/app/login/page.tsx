"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { getUserInfo, login } from "~/lib/api/auth";
import { HttpError } from "~/lib/api/http";
import { useAuthStore } from "~/lib/store/auth-store";

const schema = z.object({
  username: z.string().min(1, "用户名不能为空"),
  password: z.string().min(1, "密码不能为空"),
});

type FormValues = z.infer<typeof schema>;

/**
 * 登录页。
 *
 * 流程：账号密码 → 调 `/auth/login` 拿 token → 调 `/auth/user/info` 拿用户信息 →
 * 写入 `auth-store` → 跳转 `/admin`。
 * 若已登录（store 中存在 token），直接跳转后台。
 */
export default function LoginPage() {
  const router = useRouter();
  const setToken = useAuthStore((s) => s.setToken);
  const setUser = useAuthStore((s) => s.setUser);
  const token = useAuthStore((s) => s.token);
  const hydrated = useAuthStore((s) => s.hydrated);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: "admin", password: "123456" },
  });

  useEffect(() => {
    if (hydrated && token) {
      router.replace("/admin");
    }
  }, [hydrated, token, router]);

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const resp = await login(values);
      setToken(resp.token);
      const info = await getUserInfo();
      setUser(info);
      return info;
    },
    onSuccess: (info) => {
      toast.success(`欢迎回来，${info.nickname ?? info.username}`);
      router.replace("/admin");
    },
    onError: (err: unknown) => {
      const msg = err instanceof HttpError ? err.message : "登录失败";
      toast.error(msg);
    },
  });

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-zinc-100 p-4">
      <div className="w-full max-w-sm rounded-xl bg-white p-8 shadow-md">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-bold text-zinc-900">IDP 管理系统</h1>
          <p className="mt-1 text-sm text-zinc-500">请使用账号密码登录</p>
        </div>
        <form
          className="flex flex-col gap-4"
          onSubmit={handleSubmit((values) => mutation.mutate(values))}
        >
          <FormField label="用户名" required error={errors.username?.message}>
            <Input
              autoFocus
              autoComplete="username"
              placeholder="admin"
              {...register("username")}
              invalid={!!errors.username}
            />
          </FormField>
          <FormField label="密码" required error={errors.password?.message}>
            <Input
              type="password"
              autoComplete="current-password"
              placeholder="••••••"
              {...register("password")}
              invalid={!!errors.password}
            />
          </FormField>
          <Button
            type="submit"
            size="lg"
            className="mt-2 w-full"
            loading={mutation.isPending}
          >
            登 录
          </Button>
          <p className="mt-2 text-center text-xs text-zinc-400">
            默认账号：admin / 123456
          </p>
        </form>
      </div>
    </main>
  );
}
