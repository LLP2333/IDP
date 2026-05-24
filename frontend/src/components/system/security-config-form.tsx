"use client";

import { useEffect, useState } from "react";

import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { updateOption } from "~/lib/api/option";
import type { OptionResp } from "~/lib/api/types";

/** SecurityConfigForm props。 */
export interface SecurityConfigFormProps {
  /** PASSWORD 类别下的所有 option。 */
  options: OptionResp[];
  onSaved?: () => void;
  readonly?: boolean;
}

/** 表单字段定义。与后端 {@code PasswordPolicy} 枚举一一对应。 */
interface PolicyField {
  code: string;
  label: string;
  hint?: string;
  unit?: string;
}

const FIELDS: PolicyField[] = [
  { code: "PASSWORD_ERROR_LOCK_COUNT", label: "失败锁定次数", hint: "连续输错多少次后临时锁定账号", unit: "次" },
  { code: "PASSWORD_ERROR_LOCK_MINUTES", label: "锁定时长", hint: "达到失败上限后被锁定的时长", unit: "分钟" },
  { code: "PASSWORD_EXPIRATION_DAYS", label: "密码有效期", hint: "0 表示永不过期", unit: "天" },
  { code: "PASSWORD_EXPIRATION_WARNING_DAYS", label: "到期前提醒", hint: "到期前提前多少天提醒用户", unit: "天" },
  { code: "PASSWORD_REPETITION_TIMES", label: "禁止重复次数", hint: "新密码不能与近 N 次旧密码相同", unit: "次" },
  { code: "PASSWORD_MIN_LENGTH", label: "最小长度", unit: "位" },
  { code: "PASSWORD_REQUIRE_SYMBOLS", label: "包含特殊字符", hint: "1=必须，0=可选" },
  { code: "PASSWORD_ALLOW_CONTAIN_USERNAME", label: "允许包含用户名", hint: "1=允许，0=禁止" },
];

function valueOf(options: OptionResp[], code: string): string {
  return options.find((o) => o.code === code)?.value ?? "";
}

/**
 * 安全配置（密码策略）表单。
 *
 * 字段语义与校验由后端 {@code PasswordPolicy} 统一定义；前端只做基本范围 + 必填校验。
 * 保存前会做一个轻量交叉校验：警告天数 ≤ 有效期。
 */
export function SecurityConfigForm({ options, onSaved, readonly }: SecurityConfigFormProps) {
  const [form, setForm] = useState<Record<string, string>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [globalError, setGlobalError] = useState<string | null>(null);

  useEffect(() => {
    const init: Record<string, string> = {};
    for (const f of FIELDS) init[f.code] = valueOf(options, f.code);
    setForm(init);
    setErrors({});
  }, [options]);

  function validate(): boolean {
    const next: Record<string, string> = {};
    for (const f of FIELDS) {
      const v = form[f.code]?.trim() ?? "";
      if (v === "") {
        next[f.code] = "不能为空";
        continue;
      }
      if (!/^-?\d+$/.test(v)) {
        next[f.code] = "请输入整数";
      }
    }
    const exp = Number(form.PASSWORD_EXPIRATION_DAYS ?? "0");
    const warn = Number(form.PASSWORD_EXPIRATION_WARNING_DAYS ?? "0");
    if (exp > 0 && warn > 0 && warn > exp) {
      next.PASSWORD_EXPIRATION_WARNING_DAYS = "提醒天数不能大于有效期";
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit() {
    if (!validate()) return;
    setSaving(true);
    setGlobalError(null);
    try {
      const reqs = FIELDS.map((f) => {
        const opt = options.find((o) => o.code === f.code);
        return opt ? { id: opt.id, code: f.code, value: form[f.code] ?? "" } : null;
      }).filter(
        (it): it is { id: number; code: string; value: string } => it !== null,
      );
      await updateOption(reqs);
      setSavedAt(Date.now());
      onSaved?.();
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "保存失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="grid max-w-3xl grid-cols-1 gap-4 md:grid-cols-2">
      {FIELDS.map((f) => (
        <FormField
          key={f.code}
          label={f.label}
          required
          hint={f.unit ? `${f.hint ?? ""} 单位：${f.unit}`.trim() : f.hint}
          error={errors[f.code]}
        >
          <Input
            value={form[f.code] ?? ""}
            disabled={readonly}
            onChange={(e) => setForm({ ...form, [f.code]: e.target.value })}
            invalid={!!errors[f.code]}
          />
        </FormField>
      ))}
      {globalError ? (
        <span className="col-span-full text-sm text-red-600">{globalError}</span>
      ) : null}
      {savedAt ? (
        <span className="col-span-full text-sm text-emerald-600">
          已保存（{new Date(savedAt).toLocaleTimeString()}）
        </span>
      ) : null}
      <div className="col-span-full">
        <Button onClick={handleSubmit} loading={saving} disabled={readonly}>
          保存
        </Button>
      </div>
    </div>
  );
}
