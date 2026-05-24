"use client";

import { useEffect, useState } from "react";

import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { UploadImage } from "~/components/ui/upload-image";
import { updateOption } from "~/lib/api/option";
import type { OptionResp } from "~/lib/api/types";

/** SiteConfigForm props。 */
export interface SiteConfigFormProps {
  /** 类别为 SITE 的所有 option 列表。 */
  options: OptionResp[];
  /** 保存成功后回调（用于触发列表刷新）。 */
  onSaved?: () => void;
  /** 是否禁用编辑（无权限时显示只读）。 */
  readonly?: boolean;
}

interface SiteState {
  title: string;
  copyright: string;
  description: string;
  logo: string | null;
  favicon: string | null;
}

const CODE_TITLE = "SITE_TITLE";
const CODE_COPYRIGHT = "SITE_COPYRIGHT";
const CODE_DESCRIPTION = "SITE_DESCRIPTION";
const CODE_LOGO = "SITE_LOGO";
const CODE_FAVICON = "SITE_FAVICON";

function getOptionByCode(options: OptionResp[], code: string) {
  return options.find((o) => o.code === code);
}

function effective(options: OptionResp[], code: string): string {
  return getOptionByCode(options, code)?.value ?? "";
}

/**
 * 网站配置表单。
 *
 * 展示并维护 SITE 类别下的 5 个核心字段：标题 / 版权 / 描述 / Logo / Favicon。
 * 保存时按 option id 调 {@code PUT /system/option}。
 */
export function SiteConfigForm({ options, onSaved, readonly }: SiteConfigFormProps) {
  const [form, setForm] = useState<SiteState>({
    title: effective(options, CODE_TITLE),
    copyright: effective(options, CODE_COPYRIGHT),
    description: effective(options, CODE_DESCRIPTION),
    logo: effective(options, CODE_LOGO) || null,
    favicon: effective(options, CODE_FAVICON) || null,
  });
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setForm({
      title: effective(options, CODE_TITLE),
      copyright: effective(options, CODE_COPYRIGHT),
      description: effective(options, CODE_DESCRIPTION),
      logo: effective(options, CODE_LOGO) || null,
      favicon: effective(options, CODE_FAVICON) || null,
    });
  }, [options]);

  async function handleSubmit() {
    setSaving(true);
    setError(null);
    try {
      const reqs = [
        { code: CODE_TITLE, value: form.title },
        { code: CODE_COPYRIGHT, value: form.copyright },
        { code: CODE_DESCRIPTION, value: form.description },
        { code: CODE_LOGO, value: form.logo },
        { code: CODE_FAVICON, value: form.favicon },
      ]
        .map(({ code, value }) => ({ id: getOptionByCode(options, code)?.id, code, value }))
        .filter(
          (it): it is { id: number; code: string; value: string | null } =>
            typeof it.id === "number",
        );
      await updateOption(reqs);
      setSavedAt(Date.now());
      onSaved?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex max-w-2xl flex-col gap-4">
      <FormField label="网站标题" required>
        <Input
          value={form.title}
          disabled={readonly}
          onChange={(e) => setForm({ ...form, title: e.target.value })}
          maxLength={64}
        />
      </FormField>
      <FormField label="版权信息">
        <Input
          value={form.copyright}
          disabled={readonly}
          onChange={(e) => setForm({ ...form, copyright: e.target.value })}
          maxLength={128}
        />
      </FormField>
      <FormField label="网站描述">
        <Input
          value={form.description}
          disabled={readonly}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
          maxLength={255}
        />
      </FormField>
      <FormField label="Logo">
        <UploadImage
          code={CODE_LOGO}
          value={form.logo}
          onChange={(v) => setForm({ ...form, logo: v })}
        />
      </FormField>
      <FormField label="Favicon">
        <UploadImage
          code={CODE_FAVICON}
          value={form.favicon}
          onChange={(v) => setForm({ ...form, favicon: v })}
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
