"use client";

import { useEffect, useState } from "react";

import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Switch } from "~/components/ui/switch";
import { updateOption } from "~/lib/api/option";
import type { OptionResp } from "~/lib/api/types";

/** LoginConfigForm props。 */
export interface LoginConfigFormProps {
  /** LOGIN 类别下的所有 option。 */
  options: OptionResp[];
  onSaved?: () => void;
  readonly?: boolean;
}

const CODE_CAPTCHA = "LOGIN_CAPTCHA_ENABLED";

/**
 * 登录配置表单。
 *
 * 当前 LOGIN 类别仅有 “是否启用验证码” 一项；后续如新增登录策略，按相同模式扩展即可。
 */
export function LoginConfigForm({ options, onSaved, readonly }: LoginConfigFormProps) {
  const [captchaEnabled, setCaptchaEnabled] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const v = options.find((o) => o.code === CODE_CAPTCHA)?.value ?? "0";
    setCaptchaEnabled(v === "1");
  }, [options]);

  async function handleSubmit() {
    setSaving(true);
    setError(null);
    try {
      const opt = options.find((o) => o.code === CODE_CAPTCHA);
      if (!opt) return;
      await updateOption([
        { id: opt.id, code: CODE_CAPTCHA, value: captchaEnabled ? "1" : "0" },
      ]);
      setSavedAt(Date.now());
      onSaved?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex max-w-xl flex-col gap-4">
      <FormField label="启用登录验证码" hint="开启后，登录页将要求输入图形验证码。">
        <Switch
          checked={captchaEnabled}
          onChange={setCaptchaEnabled}
          disabled={readonly}
          label="启用登录验证码"
        />
      </FormField>
      {error ? <span className="text-sm text-red-600">{error}</span> : null}
      {savedAt ? (
        <span className="text-sm text-emerald-600">
          已保存（{new Date(savedAt).toLocaleTimeString()}）
        </span>
      ) : null}
      <div>
        <Button onClick={handleSubmit} loading={saving} disabled={readonly}>
          保存
        </Button>
      </div>
    </div>
  );
}
