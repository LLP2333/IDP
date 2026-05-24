"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { UserMultiSelect } from "~/components/system/user-multi-select";
import { Button } from "~/components/ui/button";
import { FormField } from "~/components/ui/form-field";
import { Input } from "~/components/ui/input";
import { Select } from "~/components/ui/select";
import { Switch } from "~/components/ui/switch";
import { HttpError } from "~/lib/api/http";
import { addNotice, getNotice, updateNotice } from "~/lib/api/notice";
import type {
  NoticeDetailResp,
  NoticeMethod,
  NoticeReq,
  NoticeScope,
} from "~/lib/api/types";
import { useDict } from "~/lib/hooks/use-dict";

/**
 * 校验规则：定时发布时强制要求 publishTime。
 *
 * <p>{@code react-hook-form} 在 {@code superRefine} 中可以基于联动字段抛错，比单字段约束更适合
 * 这种 “是否定时 + 发布时间” 的联动校验。</p>
 */
const schema = z
  .object({
    title: z.string().min(1, "请输入标题").max(150),
    content: z.string().min(1, "请输入正文"),
    type: z.string().min(1, "请选择分类"),
    noticeScope: z.union([z.literal(1), z.literal(2)]),
    noticeUsers: z.array(z.number()).optional(),
    noticeMethods: z.array(z.union([z.literal(1), z.literal(2)])).optional(),
    isTiming: z.boolean(),
    publishTime: z.string().optional().or(z.literal("")),
    isTop: z.boolean(),
    status: z.union([z.literal(1), z.literal(3)]),
  })
  .superRefine((data, ctx) => {
    if (data.noticeScope === 2 && (!data.noticeUsers || data.noticeUsers.length === 0)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["noticeUsers"],
        message: "请选择通知用户",
      });
    }
    if (data.status === 3 && data.isTiming && !data.publishTime) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["publishTime"],
        message: "定时发布时间不能为空",
      });
    }
  });

type FormValues = z.infer<typeof schema>;

/**
 * 把表单值转成后端 {@link NoticeReq}：处理 publishTime 为空、noticeUsers 仅 USER 范围保留等。
 */
function toReq(values: FormValues): NoticeReq {
  return {
    title: values.title,
    content: values.content,
    type: values.type,
    noticeScope: values.noticeScope,
    noticeUsers: values.noticeScope === 2 ? values.noticeUsers ?? [] : undefined,
    noticeMethods: values.noticeMethods,
    isTiming: values.isTiming,
    publishTime: values.isTiming ? values.publishTime ?? null : null,
    isTop: values.isTop,
    status: values.status,
  };
}

/**
 * 把后端返回的详情回填到表单。
 */
function fromDetail(detail: NoticeDetailResp): FormValues {
  return {
    title: detail.title,
    content: detail.content,
    type: detail.type,
    noticeScope: detail.noticeScope,
    noticeUsers: detail.noticeUsers ?? [],
    noticeMethods: detail.noticeMethods ?? [],
    isTiming: !!detail.isTiming,
    publishTime: detail.publishTime ?? "",
    isTop: !!detail.isTop,
    // 编辑场景下默认按当前状态：PENDING 视为 “发布”
    status: detail.status === 1 ? 1 : 3,
  };
}

/**
 * 公告新增 / 编辑页。
 *
 * <p>路由查询参数 {@code id} 存在即视为编辑：先 {@code GET /system/notice/{id}}
 * 回填表单，再走 PUT 接口。</p>
 */
export default function NoticeAddPage() {
  const router = useRouter();
  const params = useSearchParams();
  const id = params.get("id");
  const queryClient = useQueryClient();

  const typeDict = useDict("notice_type");
  const scopeDict = useDict("notice_scope_enum");
  const methodDict = useDict("notice_method_enum");

  const detailQuery = useQuery({
    queryKey: ["notice", "detail", id],
    queryFn: () => getNotice(id!),
    enabled: !!id,
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: "",
      content: "",
      type: "",
      noticeScope: 1 as NoticeScope,
      noticeUsers: [],
      noticeMethods: [],
      isTiming: false,
      publishTime: "",
      isTop: false,
      status: 3,
    },
  });

  useEffect(() => {
    if (detailQuery.data) {
      reset(fromDetail(detailQuery.data));
    }
  }, [detailQuery.data, reset]);

  const watchScope = watch("noticeScope");
  const watchTiming = watch("isTiming");
  const watchMethods = watch("noticeMethods");

  const toggleMethod = (m: NoticeMethod) => {
    const cur: NoticeMethod[] = watchMethods ?? [];
    const next = cur.includes(m) ? cur.filter((x) => x !== m) : [...cur, m];
    return next;
  };

  const createMutation = useMutation({
    mutationFn: (values: FormValues) => addNotice(toReq(values)),
    onSuccess: () => {
      toast.success("已新增公告");
      void queryClient.invalidateQueries({ queryKey: ["notice", "list"] });
      router.push("/admin/system/notice");
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const updateMutation = useMutation({
    mutationFn: (values: FormValues) => updateNotice(id!, toReq(values)),
    onSuccess: () => {
      toast.success("已更新公告");
      void queryClient.invalidateQueries({ queryKey: ["notice", "list"] });
      void queryClient.invalidateQueries({ queryKey: ["notice", "detail", id] });
      router.push("/admin/system/notice");
    },
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "操作失败"),
  });

  const onSubmitDraft = handleSubmit((values) => {
    const next = { ...values, status: 1 as 1 | 3, isTiming: false };
    if (id) {
      updateMutation.mutate(next);
    } else {
      createMutation.mutate(next);
    }
  });

  const onSubmitPublish = handleSubmit((values) => {
    const next = { ...values, status: 3 as 1 | 3 };
    if (id) {
      updateMutation.mutate(next);
    } else {
      createMutation.mutate(next);
    }
  });

  const pending = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">{id ? "编辑公告" : "新增公告"}</h2>
          <p className="mt-1 text-sm text-zinc-500">
            填写公告内容，可保存为草稿或直接发布；勾选 “定时发布” 时需填发布时间。
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => router.push("/admin/system/notice")}>
            返回
          </Button>
          <Button variant="secondary" loading={pending} onClick={onSubmitDraft}>
            保存草稿
          </Button>
          <Button loading={pending} onClick={onSubmitPublish}>
            {watch("isTiming") ? "定时发布" : "立即发布"}
          </Button>
        </div>
      </div>

      <form className="grid grid-cols-1 gap-4 rounded-md border border-zinc-200 bg-white p-5 sm:grid-cols-2">
        <FormField label="标题" required error={errors.title?.message} className="sm:col-span-2">
          <Input {...register("title")} invalid={!!errors.title} placeholder="请输入标题" />
        </FormField>

        <FormField label="分类" required error={errors.type?.message}>
          <Select {...register("type")} invalid={!!errors.type}>
            <option value="">请选择</option>
            {typeDict.items.map((it) => (
              <option key={it.id} value={it.value}>
                {it.label}
              </option>
            ))}
          </Select>
        </FormField>

        <FormField label="通知范围" required error={errors.noticeScope?.message}>
          <Controller
            control={control}
            name="noticeScope"
            render={({ field }) => (
              <Select
                value={field.value}
                onChange={(e) => field.onChange(Number(e.target.value))}
                invalid={!!errors.noticeScope}
              >
                {scopeDict.items.map((it) => (
                  <option key={it.id} value={it.value}>
                    {it.label}
                  </option>
                ))}
              </Select>
            )}
          />
        </FormField>

        {watchScope === 2 ? (
          <FormField
            label="通知用户"
            required
            error={errors.noticeUsers?.message}
            className="sm:col-span-2"
          >
            <Controller
              control={control}
              name="noticeUsers"
              render={({ field }) => (
                <UserMultiSelect
                  value={field.value ?? []}
                  onChange={(v) => field.onChange(v)}
                  invalid={!!errors.noticeUsers}
                />
              )}
            />
          </FormField>
        ) : null}

        <FormField label="通知方式" error={errors.noticeMethods?.message} className="sm:col-span-2">
          <Controller
            control={control}
            name="noticeMethods"
            render={({ field }) => (
              <div className="flex flex-wrap gap-3 pt-1.5 text-sm">
                {methodDict.items.map((it) => {
                  const v = Number(it.value) as NoticeMethod;
                  const checked = (field.value ?? []).includes(v);
                  return (
                    <label key={it.id} className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => field.onChange(toggleMethod(v))}
                      />
                      {it.label}
                    </label>
                  );
                })}
              </div>
            )}
          />
        </FormField>

        <FormField label="是否置顶">
          <Controller
            control={control}
            name="isTop"
            render={({ field }) => (
              <Switch checked={!!field.value} onChange={field.onChange} />
            )}
          />
        </FormField>

        <FormField label="是否定时发布">
          <Controller
            control={control}
            name="isTiming"
            render={({ field }) => (
              <Switch checked={!!field.value} onChange={field.onChange} />
            )}
          />
        </FormField>

        {watchTiming ? (
          <FormField label="定时发布时间" required error={errors.publishTime?.message}>
            <Input
              type="datetime-local"
              {...register("publishTime")}
              invalid={!!errors.publishTime}
            />
          </FormField>
        ) : null}

        <FormField label="正文" required error={errors.content?.message} className="sm:col-span-2">
          <textarea
            {...register("content")}
            rows={16}
            className="block w-full resize-y rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm shadow-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            placeholder="支持纯文本 / Markdown；保存后展示时会保留原始换行。"
          />
        </FormField>
      </form>
    </div>
  );
}
