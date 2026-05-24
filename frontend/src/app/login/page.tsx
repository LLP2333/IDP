"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { SiteFooter } from "~/components/site-footer";
import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { getCaptcha, getUserInfo, login } from "~/lib/api/auth";
import { HttpError } from "~/lib/api/http";
import { getLoginConfigPublic } from "~/lib/api/option";
import { useSiteConfig } from "~/lib/hooks/use-site-config";
import { useAuthStore } from "~/lib/store/auth-store";

const schema = z.object({
  username: z.string().min(1, "用户名不能为空"),
  password: z.string().min(1, "密码不能为空"),
  captcha: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

/**
 * 登录页。
 *
 * 启动时会：
 * - 读 {@code /system/option/site} 渲染站点标题 / Logo（无需登录）；
 * - 读 {@code /system/option/login} 判断是否需要验证码；
 * - 启用验证码时拉取 SVG。
 *
 * 登录成功后若返回 {@code passwordExpired=true}，将提示用户强制改密。
 */
export default function LoginPage() {
  const router = useRouter();
  const setToken = useAuthStore((s) => s.setToken);
  const setUser = useAuthStore((s) => s.setUser);
  const token = useAuthStore((s) => s.token);
  const hydrated = useAuthStore((s) => s.hydrated);

  const [captchaId, setCaptchaId] = useState<string | null>(null);
  const [captchaImage, setCaptchaImage] = useState<string | null>(null);

  const siteQuery = useSiteConfig();
  const loginConfigQuery = useQuery({
    queryKey: ["public", "login"],
    queryFn: getLoginConfigPublic,
    retry: false,
  });
  const captchaEnabled = loginConfigQuery.data?.captchaEnabled ?? false;

  async function refreshCaptcha() {
    try {
      const c = await getCaptcha();
      setCaptchaId(c.captchaId);
      setCaptchaImage(c.image);
    } catch (err) {
      toast.error(err instanceof HttpError ? err.message : "验证码加载失败");
    }
  }

  useEffect(() => {
    if (captchaEnabled) void refreshCaptcha();
  }, [captchaEnabled]);

  const {
    register,
    handleSubmit,
    formState: { errors },
    resetField,
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: "admin", password: "123456", captcha: "" },
  });

  useEffect(() => {
    if (hydrated && token) {
      router.replace("/admin");
    }
  }, [hydrated, token, router]);

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const resp = await login({
        username: values.username,
        password: values.password,
        captchaId: captchaEnabled ? captchaId ?? undefined : undefined,
        captcha: captchaEnabled ? values.captcha ?? undefined : undefined,
      });
      setToken(resp.token);
      const info = await getUserInfo();
      setUser(info);
      return { info, resp };
    },
    onSuccess: ({ info, resp }) => {
      toast.success(`欢迎回来，${info.nickname ?? info.username}`);
      if (resp.passwordExpired) {
        toast.warning("您的密码已过期，请立即修改");
        router.replace("/admin/profile/password");
        return;
      }
      if (resp.passwordWarning) {
        toast.warning(`密码将在 ${resp.passwordExpiresInDays ?? "?"} 天后到期`);
      }
      router.replace("/admin");
    },
    onError: (err: unknown) => {
      const msg = err instanceof HttpError ? err.message : "登录失败";
      toast.error(msg);
      if (captchaEnabled) {
        resetField("captcha");
        void refreshCaptcha();
      }
    },
  });

  const title = siteQuery.data?.title ?? "IDP 管理系统";
  const logo = siteQuery.data?.logo ?? null;
  const description = siteQuery.data?.description ?? "请使用账号密码登录";

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-blue-50 to-zinc-100 p-4">
      <div className="w-full max-w-sm rounded-xl bg-white p-8 shadow-md">
        <div className="mb-8 flex flex-col items-center text-center">
          {logo ? (
            <img src={logo} alt={title} className="mb-3 h-12" />
          ) : null}
          <h1 className="text-2xl font-bold text-zinc-900">{title}</h1>
          <p className="mt-1 text-sm text-zinc-500">{description}</p>
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
          {captchaEnabled ? (
            <FormField label="验证码" required error={errors.captcha?.message}>
              <div className="flex items-center gap-2">
                <Input
                  placeholder="请输入图中字符"
                  {...register("captcha", { required: "验证码不能为空" })}
                  invalid={!!errors.captcha}
                />
                <button
                  type="button"
                  onClick={refreshCaptcha}
                  className="h-9 w-28 flex-shrink-0 overflow-hidden rounded border border-zinc-200 bg-white"
                  aria-label="点击刷新验证码"
                >
                  {captchaImage ? (
                    <img src={captchaImage} alt="验证码" className="h-full w-full object-contain" />
                  ) : (
                    <span className="text-xs text-zinc-400">点击加载</span>
                  )}
                </button>
              </div>
            </FormField>
          ) : null}
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
      <SiteFooter className="mt-6" />
    </main>
  );
}
