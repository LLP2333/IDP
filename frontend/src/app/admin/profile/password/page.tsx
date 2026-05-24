"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { changeCurrentPassword, logout } from "~/lib/api/auth";
import { HttpError } from "~/lib/api/http";
import { useAuthStore } from "~/lib/store/auth-store";

/** 当前登录用户自助修改密码页面。 */
export default function ProfilePasswordPage() {
  const router = useRouter();
  const clearAuth = useAuthStore((s) => s.logout);

  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit() {
    setError(null);
    if (!oldPassword || !newPassword) {
      setError("旧密码与新密码均必填");
      return;
    }
    if (newPassword !== confirm) {
      setError("两次输入的新密码不一致");
      return;
    }
    setSaving(true);
    try {
      await changeCurrentPassword({ oldPassword, newPassword });
      toast.success("密码已修改，请重新登录");
      try {
        await logout();
      } catch {
        // 登出失败不阻塞跳转
      }
      clearAuth();
      router.replace("/login");
    } catch (err) {
      setError(err instanceof HttpError ? err.message : "修改失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex max-w-md flex-col gap-4">
      <header>
        <h2 className="text-lg font-bold text-zinc-900">修改密码</h2>
        <p className="text-xs text-zinc-500">密码强度由后端 PasswordPolicy 校验。</p>
      </header>
      <FormField label="当前密码" required>
        <Input
          type="password"
          autoComplete="current-password"
          value={oldPassword}
          onChange={(e) => setOldPassword(e.target.value)}
        />
      </FormField>
      <FormField label="新密码" required>
        <Input
          type="password"
          autoComplete="new-password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
        />
      </FormField>
      <FormField label="确认新密码" required>
        <Input
          type="password"
          autoComplete="new-password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
        />
      </FormField>
      {error ? <span className="text-sm text-red-600">{error}</span> : null}
      <div>
        <Button onClick={handleSubmit} loading={saving}>
          保存
        </Button>
      </div>
    </div>
  );
}
