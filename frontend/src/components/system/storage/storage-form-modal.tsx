"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { Select } from "~/components/ui/select";
import { addStorage, updateStorage } from "~/lib/api/storage";
import { HttpError } from "~/lib/api/http";
import type { StorageCreateReq, StorageResp, StorageType, StorageUpdateReq } from "~/lib/api/types";

/**
 * `StorageFormModal` Props。
 */
export interface StorageFormModalProps {
  /** 是否可见。 */
  open: boolean;
  /** 编辑对象；null 表示新增。 */
  target: StorageResp | null;
  /** 关闭回调。 */
  onClose: () => void;
}

interface FormState {
  name: string;
  code: string;
  type: StorageType;
  accessKey: string;
  secretKey: string;
  endpoint: string;
  bucketName: string;
  domain: string;
  recycleBinEnabled: boolean;
  recycleBinPath: string;
  description: string;
  sort: number;
  status: 1 | 2;
}

const EMPTY: FormState = {
  name: "",
  code: "",
  type: 1,
  accessKey: "",
  secretKey: "",
  endpoint: "",
  bucketName: "",
  domain: "",
  recycleBinEnabled: false,
  recycleBinPath: "",
  description: "",
  sort: 1,
  status: 1,
};

/**
 * 存储新增 / 编辑模态框。
 *
 * 字段在 LOCAL / S3 之间动态切换：LOCAL 隐藏 accessKey / secretKey / endpoint。
 * SecretKey 在编辑态留空表示不修改。
 */
export function StorageFormModal({ open, target, onClose }: StorageFormModalProps) {
  const queryClient = useQueryClient();
  const isEdit = !!target;
  const [form, setForm] = useState<FormState>(EMPTY);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});

  useEffect(() => {
    if (!open) return;
    if (target) {
      setForm({
        name: target.name,
        code: target.code,
        type: target.type,
        accessKey: target.accessKey ?? "",
        secretKey: "",
        endpoint: target.endpoint ?? "",
        bucketName: target.bucketName ?? "",
        domain: target.domain ?? "",
        recycleBinEnabled: target.recycleBinEnabled,
        recycleBinPath: target.recycleBinPath ?? "",
        description: target.description ?? "",
        sort: target.sort,
        status: target.status === 2 ? 2 : 1,
      });
    } else {
      setForm(EMPTY);
    }
    setErrors({});
  }, [open, target]);

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const validate = (): boolean => {
    const next: Partial<Record<keyof FormState, string>> = {};
    if (!form.name.trim()) next.name = "名称不能为空";
    if (!isEdit && !form.code.trim()) next.code = "编码不能为空";
    if (!form.bucketName.trim()) {
      next.bucketName = form.type === 1 ? "本地路径不能为空" : "Bucket 不能为空";
    }
    if (form.type === 2) {
      if (!form.endpoint.trim()) next.endpoint = "Endpoint 不能为空";
      if (!form.accessKey.trim()) next.accessKey = "Access Key 不能为空";
      if (!isEdit && !form.secretKey.trim()) next.secretKey = "Secret Key 不能为空";
    }
    if (form.recycleBinEnabled && !form.recycleBinPath.trim()) {
      next.recycleBinPath = "回收站路径不能为空";
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = useMutation({
    mutationFn: async () => {
      if (isEdit && target) {
        const req: StorageUpdateReq = {
          name: form.name.trim(),
          accessKey: form.type === 2 ? form.accessKey.trim() : undefined,
          secretKey: form.type === 2 && form.secretKey ? form.secretKey : undefined,
          endpoint: form.type === 2 ? form.endpoint.trim() : undefined,
          bucketName: form.bucketName.trim(),
          domain: form.domain.trim() || undefined,
          description: form.description.trim() || undefined,
          sort: form.sort,
          status: form.status,
        };
        await updateStorage(target.id, req);
      } else {
        const req: StorageCreateReq = {
          name: form.name.trim(),
          code: form.code.trim(),
          type: form.type,
          accessKey: form.type === 2 ? form.accessKey.trim() : undefined,
          secretKey: form.type === 2 ? form.secretKey : undefined,
          endpoint: form.type === 2 ? form.endpoint.trim() : undefined,
          bucketName: form.bucketName.trim(),
          domain: form.domain.trim() || undefined,
          recycleBinEnabled: form.recycleBinEnabled,
          recycleBinPath: form.recycleBinEnabled ? form.recycleBinPath.trim() : undefined,
          description: form.description.trim() || undefined,
          sort: form.sort,
          status: form.status,
        };
        await addStorage(req);
      }
    },
    onSuccess: () => {
      toast.success(isEdit ? "已保存" : "已新增");
      void queryClient.invalidateQueries({ queryKey: ["storage", "list"] });
      onClose();
    },
    onError: (err: unknown) => {
      toast.error(err instanceof HttpError ? err.message : "保存失败");
    },
  });

  const handleConfirm = () => {
    if (!validate()) return;
    submit.mutate();
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={isEdit ? "编辑存储" : "新增存储"}
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={submit.isPending}>
            取消
          </Button>
          <Button onClick={handleConfirm} loading={submit.isPending}>
            保存
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <FormField label="名称" required error={errors.name}>
          <Input value={form.name} onChange={(e) => set("name", e.target.value)} />
        </FormField>
        <FormField label="编码" required error={errors.code} hint={isEdit ? "编辑模式下不可修改" : undefined}>
          <Input
            value={form.code}
            onChange={(e) => set("code", e.target.value)}
            disabled={isEdit}
            placeholder="例如：local、minio-dev"
          />
        </FormField>
        <FormField label="类型" required>
          <Select
            value={String(form.type)}
            onChange={(e) => set("type", Number(e.target.value) as StorageType)}
            disabled={isEdit}
          >
            <option value="1">本地存储</option>
            <option value="2">S3 对象存储</option>
          </Select>
        </FormField>
        <FormField
          label={form.type === 1 ? "本地路径" : "Bucket"}
          required
          error={errors.bucketName}
        >
          <Input
            value={form.bucketName}
            onChange={(e) => set("bucketName", e.target.value)}
            placeholder={form.type === 1 ? "/var/idp/files" : "idp"}
          />
        </FormField>
        {form.type === 2 ? (
          <>
            <FormField label="Endpoint" required error={errors.endpoint}>
              <Input
                value={form.endpoint}
                onChange={(e) => set("endpoint", e.target.value)}
                placeholder="http://localhost:9000"
              />
            </FormField>
            <FormField label="Access Key" required error={errors.accessKey}>
              <Input value={form.accessKey} onChange={(e) => set("accessKey", e.target.value)} />
            </FormField>
            <FormField
              label="Secret Key"
              required={!isEdit}
              error={errors.secretKey}
              hint={isEdit ? "留空表示不修改原密钥" : undefined}
            >
              <Input
                type="password"
                value={form.secretKey}
                onChange={(e) => set("secretKey", e.target.value)}
                placeholder="******"
              />
            </FormField>
          </>
        ) : null}
        <FormField label="访问域名" hint="可选；本地存储默认走 /file/local/ 静态资源">
          <Input
            value={form.domain}
            onChange={(e) => set("domain", e.target.value)}
            placeholder="http://localhost:8080/file/local/"
          />
        </FormField>
        <FormField label="排序">
          <Input
            type="number"
            value={form.sort}
            onChange={(e) => set("sort", Number(e.target.value) || 1)}
            min={1}
          />
        </FormField>
        <FormField label="状态">
          <Select
            value={String(form.status)}
            onChange={(e) => set("status", Number(e.target.value) === 2 ? 2 : 1)}
          >
            <option value="1">启用</option>
            <option value="2">禁用</option>
          </Select>
        </FormField>
        {!isEdit ? (
          <FormField label="开启回收站">
            <Select
              value={form.recycleBinEnabled ? "1" : "0"}
              onChange={(e) => set("recycleBinEnabled", e.target.value === "1")}
            >
              <option value="0">否</option>
              <option value="1">是</option>
            </Select>
          </FormField>
        ) : null}
        {!isEdit && form.recycleBinEnabled ? (
          <FormField label="回收站路径" required error={errors.recycleBinPath}>
            <Input
              value={form.recycleBinPath}
              onChange={(e) => set("recycleBinPath", e.target.value)}
              placeholder="/.recycle"
            />
          </FormField>
        ) : null}
        <FormField label="描述" className="md:col-span-2">
          <Input value={form.description} onChange={(e) => set("description", e.target.value)} />
        </FormField>
      </div>
    </Modal>
  );
}
