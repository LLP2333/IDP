"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AtSign,
  CheckCircle2,
  KeyRound,
  Mail,
  Pencil,
  Phone,
  ShieldAlert,
  ShieldCheck,
  User as UserIcon,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import {
  ProfileBasicInfoForm,
  type ProfileBasicInfoValues,
} from "~/components/system/profile-basic-info-form";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import {
  changeCurrentPassword,
  getUserInfo,
  logout,
  updateCurrentUserBasicInfo,
} from "~/lib/api/auth";
import { HttpError } from "~/lib/api/http";
import { useAuthStore } from "~/lib/store/auth-store";

const BASIC_INFO_FORM_ID = "profile-basic-info-form";
const PWD_FORM_ID = "profile-password-form";

const GENDER_LABEL: Record<number, string> = {
  0: "未知",
  1: "男",
  2: "女",
};

/**
 * 个人中心页 `/admin/profile`。
 *
 * 布局对齐 continew-admin 的 `views/user/profile`：
 * - 左侧：基本信息卡片，展示用户名 / 邮箱 / 手机 / 部门(暂无) / 角色，并提供编辑入口；
 * - 右侧上方：密码安全卡片，展示密码状态并提供 “修改密码” 入口；
 * - 编辑基本信息 / 修改密码均通过 Modal 弹窗承载。
 *
 * 修改基本信息后会调用 `getUserInfo` 重新拉取 zustand store；
 * 修改密码成功后会主动登出并跳转登录页，强制使用新密码重新登录。
 */
export default function ProfilePage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const clearAuth = useAuthStore((s) => s.logout);

  const [basicOpen, setBasicOpen] = useState(false);
  const [pwdOpen, setPwdOpen] = useState(false);

  const updateBasicMutation = useMutation({
    mutationFn: async (values: ProfileBasicInfoValues) => {
      // 邮箱 / 手机为 "" 时显式发送给后端，表示用户主动清除（落库为 null）。
      await updateCurrentUserBasicInfo({
        nickname: values.nickname,
        email: values.email,
        phone: values.phone,
        gender: values.gender,
      });
      const fresh = await getUserInfo();
      setUser(fresh);
      void queryClient.invalidateQueries({ queryKey: ["auth", "user-info"] });
      return fresh;
    },
    onSuccess: () => {
      toast.success("已更新基本信息");
      setBasicOpen(false);
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "更新失败"),
  });

  const changePwdMutation = useMutation({
    mutationFn: async (values: { oldPassword: string; newPassword: string }) => {
      await changeCurrentPassword(values);
    },
    onSuccess: async () => {
      toast.success("密码已修改，请重新登录");
      try {
        await logout();
      } catch {
        // 忽略后端登出失败，本地仍然清理
      }
      clearAuth();
      router.replace("/login");
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "修改失败"),
  });

  if (!user) {
    return (
      <div className="text-sm text-zinc-500">正在加载用户信息…</div>
    );
  }

  const genderLabel = GENDER_LABEL[user.gender ?? 0] ?? "未知";
  const hasPhone = !!user.phone;
  const hasEmail = !!user.email;

  return (
    <div className="flex flex-col gap-4">
      <header>
        <h2 className="text-xl font-semibold">个人中心</h2>
        <p className="mt-1 text-sm text-zinc-500">
          维护当前账号的基本信息与密码安全设置。
        </p>
      </header>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <section className="rounded-lg border border-zinc-200 bg-white p-5 lg:col-span-5">
          <div className="flex items-center justify-between border-b border-zinc-100 pb-3">
            <h3 className="text-base font-semibold text-zinc-900">基本信息</h3>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => setBasicOpen(true)}
            >
              <Pencil size={14} /> 编辑
            </Button>
          </div>

          <div className="flex flex-col items-center gap-2 py-6">
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-blue-50 text-3xl font-semibold text-blue-700">
              {(user.nickname ?? user.username ?? "?").trim().slice(0, 1).toUpperCase()}
            </div>
            <div className="text-lg font-semibold text-zinc-900">
              {user.nickname ?? user.username}
            </div>
            <div className="text-xs text-zinc-400">ID：{user.id}</div>
          </div>

          <dl className="grid grid-cols-1 gap-3 border-t border-zinc-100 pt-4 text-sm">
            <ProfileItem
              icon={<UserIcon size={16} />}
              label="用户名"
              value={user.username}
            />
            <ProfileItem
              icon={<AtSign size={16} />}
              label="昵称"
              value={user.nickname ?? "—"}
            />
            <ProfileItem
              icon={<Mail size={16} />}
              label="邮箱"
              value={user.email ?? "暂无"}
            />
            <ProfileItem
              icon={<Phone size={16} />}
              label="手机号"
              value={user.phone ?? "暂无"}
            />
            <ProfileItem
              icon={<UserIcon size={16} />}
              label="性别"
              value={genderLabel}
            />
            <ProfileItem
              icon={<ShieldCheck size={16} />}
              label="角色"
              value={
                user.roles.length > 0 ? (
                  <div className="flex flex-wrap gap-1">
                    {user.roles.map((r) => (
                      <Badge key={r} tone="info">
                        {r}
                      </Badge>
                    ))}
                  </div>
                ) : (
                  "—"
                )
              }
            />
          </dl>
        </section>

        <section className="flex flex-col gap-4 lg:col-span-7">
          <div className="rounded-lg border border-zinc-200 bg-white p-5">
            <div className="flex items-center justify-between border-b border-zinc-100 pb-3">
              <h3 className="text-base font-semibold text-zinc-900">安全设置</h3>
            </div>
            <SecurityRow
              icon={<KeyRound size={20} className="text-blue-600" />}
              title="登录密码"
              status={true}
              statusLabel="已设置"
              description="为了您的账号安全，建议定期修改密码"
              actionLabel="修改"
              onAction={() => setPwdOpen(true)}
            />
            <SecurityRow
              icon={<Mail size={20} className="text-amber-600" />}
              title="安全邮箱"
              status={hasEmail}
              statusLabel={hasEmail ? "已绑定" : "未绑定"}
              description={
                hasEmail
                  ? user.email!
                  : "邮箱可用于登录、身份验证、密码找回、通知接收"
              }
              actionLabel={hasEmail ? "修改" : "绑定"}
              onAction={() => setBasicOpen(true)}
            />
            <SecurityRow
              icon={<Phone size={20} className="text-emerald-600" />}
              title="安全手机"
              status={hasPhone}
              statusLabel={hasPhone ? "已绑定" : "未绑定"}
              description={
                hasPhone
                  ? user.phone!
                  : "手机号可用于登录、身份验证、密码找回、通知接收"
              }
              actionLabel={hasPhone ? "修改" : "绑定"}
              onAction={() => setBasicOpen(true)}
            />
          </div>
        </section>
      </div>

      <Modal
        open={basicOpen}
        onClose={() => setBasicOpen(false)}
        title="修改基本信息"
        size="lg"
        footer={
          <>
            <Button variant="outline" onClick={() => setBasicOpen(false)}>
              取消
            </Button>
            <Button
              type="submit"
              form={BASIC_INFO_FORM_ID}
              loading={updateBasicMutation.isPending}
            >
              保存
            </Button>
          </>
        }
      >
        <ProfileBasicInfoForm
          formId={BASIC_INFO_FORM_ID}
          initial={user}
          onSubmit={(values) => updateBasicMutation.mutate(values)}
        />
      </Modal>

      <Modal
        open={pwdOpen}
        onClose={() => setPwdOpen(false)}
        title="修改登录密码"
        footer={
          <>
            <Button variant="outline" onClick={() => setPwdOpen(false)}>
              取消
            </Button>
            <Button
              type="submit"
              form={PWD_FORM_ID}
              loading={changePwdMutation.isPending}
            >
              保存
            </Button>
          </>
        }
      >
        <ProfilePasswordChangeForm
          formId={PWD_FORM_ID}
          onSubmit={(v) => changePwdMutation.mutate(v)}
        />
      </Modal>
    </div>
  );
}

interface ProfileItemProps {
  /** 行首图标。 */
  icon: React.ReactNode;
  /** 字段标签。 */
  label: string;
  /** 字段值，可为 ReactNode 以支持自定义渲染。 */
  value: React.ReactNode;
}

/**
 * 个人信息行：左侧图标 + 标签，右侧值。
 */
function ProfileItem({ icon, label, value }: ProfileItemProps) {
  return (
    <div className="flex items-start gap-2">
      <div className="mt-0.5 text-zinc-400">{icon}</div>
      <div className="flex flex-1 flex-col">
        <span className="text-xs text-zinc-500">{label}</span>
        <div className="text-sm text-zinc-800">{value}</div>
      </div>
    </div>
  );
}

interface SecurityRowProps {
  /** 行首图标。 */
  icon: React.ReactNode;
  /** 标题。 */
  title: string;
  /** 当前状态：true=已设置/已绑定，false=未设置/未绑定。 */
  status: boolean;
  /** 状态文案。 */
  statusLabel: string;
  /** 描述文案 / 当前值。 */
  description: string;
  /** 按钮文案。 */
  actionLabel: string;
  /** 按钮点击回调。 */
  onAction: () => void;
}

/**
 * 安全设置卡片中的单行：图标 + 标题 + 状态徽标 + 描述 + 操作按钮。
 */
function SecurityRow({
  icon,
  title,
  status,
  statusLabel,
  description,
  actionLabel,
  onAction,
}: SecurityRowProps) {
  return (
    <div className="flex items-center gap-4 border-b border-zinc-50 py-4 last:border-b-0">
      <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-md bg-zinc-50">
        {icon}
      </div>
      <div className="flex flex-1 flex-col gap-0.5">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-zinc-900">{title}</span>
          <span
            className={
              "inline-flex items-center gap-1 text-xs " +
              (status ? "text-emerald-600" : "text-amber-600")
            }
          >
            {status ? (
              <CheckCircle2 size={12} />
            ) : (
              <ShieldAlert size={12} />
            )}
            {statusLabel}
          </span>
        </div>
        <span className="text-xs text-zinc-500">{description}</span>
      </div>
      <Button
        size="sm"
        variant={status ? "outline" : "primary"}
        onClick={onAction}
      >
        {actionLabel}
      </Button>
    </div>
  );
}

interface ProfilePasswordChangeFormProps {
  /** form 元素 id，用于 Modal footer 触发 submit。 */
  formId: string;
  /** 校验通过后的提交回调。 */
  onSubmit: (values: { oldPassword: string; newPassword: string }) => void;
}

/**
 * 修改密码表单：原密码 + 新密码 + 确认新密码。
 *
 * - 不复用 {@link UserPasswordForm}，因为后者只有 “新密码 + 确认”，
 *   缺少自助改密所必需的 `oldPassword` 字段；
 * - 提交时只把 `oldPassword / newPassword` 透传给上层，避免后端误收到 `confirm`。
 */
function ProfilePasswordChangeForm({
  formId,
  onSubmit,
}: ProfilePasswordChangeFormProps) {
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);

  return (
    <form
      id={formId}
      className="flex flex-col gap-4"
      onSubmit={(e) => {
        e.preventDefault();
        setError(null);
        if (!oldPassword || !newPassword) {
          setError("旧密码与新密码均必填");
          return;
        }
        if (newPassword.length < 6 || newPassword.length > 32) {
          setError("新密码长度需在 6-32 之间");
          return;
        }
        if (newPassword !== confirm) {
          setError("两次输入的新密码不一致");
          return;
        }
        onSubmit({ oldPassword, newPassword });
      }}
    >
      <FormField label="原密码" required>
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
    </form>
  );
}
