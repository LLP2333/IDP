"use client";

import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";

import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { listUser } from "~/lib/api/user";
import { cn } from "~/lib/utils";

interface UserMultiSelectProps {
  /** 当前选中的 userId 列表。 */
  value: number[];
  /** 选择变化回调。 */
  onChange: (ids: number[]) => void;
  /** 字段是否处于错误态（用于红色边框）。 */
  invalid?: boolean;
  /** 触发按钮的占位文案。 */
  placeholder?: string;
  /** 是否禁用。 */
  disabled?: boolean;
}

/**
 * 通用 “指定用户” 多选下拉。
 *
 * <p>实现方式：触发按钮 + 弹窗内带搜索的复选列表，避免引入额外的虚拟列表组件。
 * 后端用户量大时仍可分页搜索；当前实现拉一页（size=1000）兜底，多数中后台够用。</p>
 *
 * <p>本组件**只负责选择**，不联动其它表单字段；外层表单用 {@code useEffect}
 * 把 {@code value} 同步到 react-hook-form 即可。</p>
 */
export function UserMultiSelect({
  value,
  onChange,
  invalid,
  placeholder = "请选择通知用户",
  disabled,
}: UserMultiSelectProps) {
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [draft, setDraft] = useState<number[]>(value);

  const userQuery = useQuery({
    queryKey: ["notice", "user-pool"],
    queryFn: () => listUser({ page: 1, size: 1000 }),
    enabled: open,
    staleTime: 60 * 1000,
  });

  const filtered = useMemo(() => {
    const users = userQuery.data?.list ?? [];
    const kw = keyword.trim().toLowerCase();
    if (!kw) return users;
    return users.filter((u) => {
      const nick = (u.nickname ?? "").toLowerCase();
      const uname = u.username.toLowerCase();
      return nick.includes(kw) || uname.includes(kw);
    });
  }, [userQuery.data?.list, keyword]);

  const toggle = (id: number) => {
    setDraft((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  const handleOpen = () => {
    if (disabled) return;
    setDraft(value);
    setKeyword("");
    setOpen(true);
  };

  const handleConfirm = () => {
    onChange(draft);
    setOpen(false);
  };

  return (
    <>
      <button
        type="button"
        onClick={handleOpen}
        disabled={disabled}
        className={cn(
          "flex min-h-[36px] w-full items-center justify-between rounded-md border bg-white px-3 py-1.5 text-left text-sm transition-colors",
          invalid ? "border-red-500" : "border-zinc-300 hover:border-zinc-400",
          disabled && "cursor-not-allowed opacity-60",
        )}
      >
        {value.length > 0 ? (
          <span className="text-zinc-700">已选 {value.length} 位用户</span>
        ) : (
          <span className="text-zinc-400">{placeholder}</span>
        )}
        <span className="text-zinc-400">▾</span>
      </button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title="选择通知用户"
        size="md"
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button onClick={handleConfirm}>确定（{draft.length}）</Button>
          </>
        }
      >
        <div className="flex flex-col gap-3">
          <Input
            placeholder="按昵称 / 用户名搜索"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          {userQuery.isLoading ? (
            <div className="text-sm text-zinc-500">加载中…</div>
          ) : (
            <div className="max-h-72 overflow-y-auto rounded-md border border-zinc-200">
              {filtered.length === 0 ? (
                <div className="px-3 py-6 text-center text-sm text-zinc-400">
                  无匹配用户
                </div>
              ) : (
                filtered.map((u) => {
                  const checked = draft.includes(u.id);
                  return (
                    <label
                      key={u.id}
                      className="flex cursor-pointer items-center gap-2 border-b border-zinc-100 px-3 py-2 hover:bg-zinc-50"
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggle(u.id)}
                      />
                      <span className="flex-1 text-sm">
                        {u.nickname ?? u.username}
                        <span className="ml-2 text-xs text-zinc-400">
                          @{u.username}
                        </span>
                      </span>
                      {u.status === 0 ? (
                        <Badge tone="danger">禁用</Badge>
                      ) : null}
                    </label>
                  );
                })
              )}
            </div>
          )}
        </div>
      </Modal>
    </>
  );
}
