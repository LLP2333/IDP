"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Download,
  Eye,
  FolderPlus,
  Grid,
  HardDrive,
  Home,
  List,
  Pencil,
  Search,
  Trash2,
  Upload,
} from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Breadcrumb } from "~/components/ui/breadcrumb";
import { Button } from "~/components/ui/button";
import { ContextMenu, useContextMenu } from "~/components/ui/context-menu";
import { Empty } from "~/components/ui/empty";
import { Input } from "~/components/ui/input";
import { Pagination } from "~/components/ui/pagination";
import { deleteFile, pageFile, uploadFile } from "~/lib/api/file";
import { HttpError } from "~/lib/api/http";
import type { FileResp, FileType } from "~/lib/api/types";
import { usePermission } from "~/lib/hooks/use-permission";
import { cn, downloadByUrl } from "~/lib/utils";

import { FileCreateDirModal } from "./file-create-dir-modal";
import { FileDetailModal } from "./file-detail-modal";
import { FileGrid } from "./file-grid";
import { FileList } from "./file-list";
import { FileRecycleModal } from "./file-recycle-modal";
import { FileRenameModal } from "./file-rename-modal";
import { AudioPreview } from "./file-preview/audio-preview";
import { ImagePreview } from "./file-preview/image-preview";
import { getOfficePreviewKind, OfficePreview } from "./file-preview/office-preview";
import { VideoPreview } from "./file-preview/video-preview";
import { MultipartUploaderModal } from "./multipart-uploader";

/**
 * `FileMain` Props。
 */
export interface FileMainProps {
  /** 左侧分类筛选 type，undefined 表示不限。 */
  type: number | undefined;
}

/**
 * 把 path 解析为面包屑链路。
 */
function splitPath(path: string): { label: string; path: string }[] {
  const segments = path.split("/").filter(Boolean);
  const result: { label: string; path: string }[] = [];
  let prefix = "";
  for (const seg of segments) {
    prefix += `/${seg}`;
    result.push({ label: seg, path: prefix });
  }
  return result;
}


/**
 * 文件管理右侧主区。
 *
 * - 面包屑、关键字搜索；
 * - 普通上传、分片上传、新建文件夹、批量删除、回收站按钮；
 * - grid / list 视图切换；
 * - 选中状态、右键菜单、详情 / 预览 / 重命名。
 */
export function FileMain({ type }: FileMainProps) {
  const queryClient = useQueryClient();
  const { hasPermission } = usePermission();
  const [parentPath, setParentPath] = useState("/");
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [view, setView] = useState<"grid" | "list">("grid");
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  const [renameTarget, setRenameTarget] = useState<FileResp | null>(null);
  const [detailTarget, setDetailTarget] = useState<FileResp | null>(null);
  const [createDirOpen, setCreateDirOpen] = useState(false);
  const [recycleOpen, setRecycleOpen] = useState(false);
  const [multipartOpen, setMultipartOpen] = useState(false);
  const [imagePreview, setImagePreview] = useState<{
    images: FileResp[];
    index: number;
  } | null>(null);
  const [videoPreview, setVideoPreview] = useState<FileResp | null>(null);
  const [audioPreview, setAudioPreview] = useState<FileResp | null>(null);
  const [officePreview, setOfficePreview] = useState<FileResp | null>(null);

  const ctx = useContextMenu();

  const listQuery = useQuery({
    queryKey: ["file", "page", { parentPath, type, keyword, page, size }],
    queryFn: () =>
      pageFile({
        parentPath,
        type: type as FileType | undefined,
        originalName: keyword || undefined,
        page,
        size,
      }),
    staleTime: 0,
  });

  const deleteMutation = useMutation({
    mutationFn: (ids: number[]) => deleteFile(ids),
    onSuccess: (_d, ids) => {
      toast.success(`已处理 ${ids.length} 项`);
      void queryClient.invalidateQueries({ queryKey: ["file", "page"] });
      void queryClient.invalidateQueries({ queryKey: ["file", "statistics"] });
      setSelectedIds(new Set());
    },
    onError: (err: unknown) => toast.error(err instanceof HttpError ? err.message : "删除失败"),
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadFile(file, parentPath),
    onSuccess: () => {
      toast.success("上传成功");
      void queryClient.invalidateQueries({ queryKey: ["file", "page"] });
      void queryClient.invalidateQueries({ queryKey: ["file", "statistics"] });
    },
    onError: (err: unknown) => toast.error(err instanceof HttpError ? err.message : "上传失败"),
  });

  const data = listQuery.data;
  const items = data?.list ?? [];

  const allSelected = items.length > 0 && items.every((it) => selectedIds.has(it.id));
  const breadcrumbItems = useMemo(() => {
    const parts = splitPath(parentPath);
    return [
      {
        label: "根目录",
        icon: <Home size={14} />,
        onClick: () => {
          setParentPath("/");
          setPage(1);
          setSelectedIds(new Set());
        },
      },
      ...parts.map((p) => ({
        label: p.label,
        onClick: () => {
          setParentPath(p.path);
          setPage(1);
          setSelectedIds(new Set());
        },
      })),
    ];
  }, [parentPath]);

  const handleToggleSelect = (id: number, _shift?: boolean) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };
  const handleToggleAll = () => {
    if (allSelected) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(items.map((it) => it.id)));
    }
  };

  const handleOpenItem = (item: FileResp) => {
    if (item.type === 0) {
      setParentPath(item.path);
      setPage(1);
      setSelectedIds(new Set());
      return;
    }
    if (item.type === 2) {
      const images = items.filter((it) => it.type === 2 && it.url);
      const index = images.findIndex((it) => it.id === item.id);
      setImagePreview({
        images,
        index: index < 0 ? 0 : index,
      });
      return;
    }
    if (item.type === 4) {
      setVideoPreview(item);
      return;
    }
    if (item.type === 5) {
      setAudioPreview(item);
      return;
    }
    if (getOfficePreviewKind(item.extension) !== null) {
      setOfficePreview(item);
      return;
    }
    setDetailTarget(item);
  };

  const handleContextMenu = (e: React.MouseEvent, item: FileResp) => {
    const isFile = item.type !== 0;
    ctx.open(e, [
      {
        key: "detail",
        label: "详情",
        icon: <Eye size={14} />,
        onSelect: () => setDetailTarget(item),
      },
      ...(hasPermission("system:file:update")
        ? [
            {
              key: "rename",
              label: "重命名",
              icon: <Pencil size={14} />,
              onSelect: () => setRenameTarget(item),
            },
          ]
        : []),
      ...(isFile && item.url
        ? [
            {
              key: "download",
              label: "下载",
              icon: <Download size={14} />,
              onSelect: () => {
                void downloadByUrl(item.url!, item.originalName);
              },
            },
          ]
        : []),
      { key: "div", label: "", divider: true },
      ...(hasPermission("system:file:delete")
        ? [
            {
              key: "delete",
              label: "删除",
              icon: <Trash2 size={14} />,
              danger: true,
              onSelect: () => {
                if (window.confirm(`确认删除「${item.originalName}」？`)) {
                  deleteMutation.mutate([item.id]);
                }
              },
            },
          ]
        : []),
    ]);
  };

  const onUploadInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    Array.from(files).forEach((f) => uploadMutation.mutate(f));
    e.target.value = "";
  };

  return (
    <section className="flex min-h-0 min-w-0 flex-1 flex-col gap-3 overflow-hidden p-4">
      <Breadcrumb items={breadcrumbItems} />

      <div className="flex flex-wrap items-center gap-2">
        <Input
          className="!w-56"
          placeholder="按名称搜索"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              setKeyword(keywordInput);
              setPage(1);
            }
          }}
        />
        <Button
          size="sm"
          variant="outline"
          onClick={() => {
            setKeyword(keywordInput);
            setPage(1);
          }}
        >
          <Search size={14} /> 搜索
        </Button>
        <span className="ml-auto flex flex-wrap items-center gap-2">
          {hasPermission("system:file:upload") ? (
            <label className="inline-flex h-9 cursor-pointer items-center gap-1.5 rounded-md bg-blue-600 px-4 text-sm font-medium text-white transition-colors hover:bg-blue-700">
              <Upload size={14} /> 普通上传
              <input
                type="file"
                multiple
                className="hidden"
                aria-label="普通上传"
                onChange={onUploadInput}
              />
            </label>
          ) : null}
          {hasPermission("system:file:upload") ? (
            <Button variant="outline" onClick={() => setMultipartOpen(true)}>
              <HardDrive size={14} /> 分片上传
            </Button>
          ) : null}
          {hasPermission("system:file:createDir") ? (
            <Button variant="outline" onClick={() => setCreateDirOpen(true)}>
              <FolderPlus size={14} /> 新建文件夹
            </Button>
          ) : null}
          {hasPermission("system:file:delete") ? (
            <Button
              variant="outline"
              disabled={selectedIds.size === 0}
              onClick={() => {
                if (selectedIds.size === 0) return;
                if (window.confirm(`确认删除选中的 ${selectedIds.size} 项？`)) {
                  deleteMutation.mutate(Array.from(selectedIds));
                }
              }}
            >
              <Trash2 size={14} /> 批量删除
            </Button>
          ) : null}
          {hasPermission("system:fileRecycle:list") ? (
            <Button variant="outline" onClick={() => setRecycleOpen(true)}>
              <Trash2 size={14} /> 回收站
            </Button>
          ) : null}
          <div className="ml-2 flex overflow-hidden rounded-md border border-zinc-200">
            <button
              type="button"
              onClick={() => setView("grid")}
              className={cn(
                "flex h-9 w-9 items-center justify-center text-zinc-500",
                view === "grid" ? "bg-blue-50 text-blue-600" : "hover:bg-zinc-50",
              )}
              aria-label="网格视图"
            >
              <Grid size={14} />
            </button>
            <button
              type="button"
              onClick={() => setView("list")}
              className={cn(
                "flex h-9 w-9 items-center justify-center border-l border-zinc-200 text-zinc-500",
                view === "list" ? "bg-blue-50 text-blue-600" : "hover:bg-zinc-50",
              )}
              aria-label="列表视图"
            >
              <List size={14} />
            </button>
          </div>
        </span>
      </div>

      <div className="min-h-0 flex-1 overflow-auto">
        {listQuery.isLoading ? (
          <p className="py-8 text-center text-sm text-zinc-400">加载中…</p>
        ) : items.length === 0 ? (
          <Empty
            title="当前目录为空"
            description="拖拽或点击 “普通上传” 开始管理你的文件"
          />
        ) : view === "grid" ? (
          <FileGrid
            items={items}
            selectedIds={selectedIds}
            onToggleSelect={handleToggleSelect}
            onOpen={handleOpenItem}
            onContextMenu={handleContextMenu}
          />
        ) : (
          <FileList
            items={items}
            selectedIds={selectedIds}
            onToggleSelect={handleToggleSelect}
            onToggleAll={handleToggleAll}
            allSelected={allSelected}
            onOpen={handleOpenItem}
            onContextMenu={handleContextMenu}
          />
        )}
      </div>

      {data ? (
        <Pagination
          page={data.page}
          size={data.size}
          total={data.total}
          onPageChange={setPage}
          onSizeChange={(s) => {
            setSize(s);
            setPage(1);
          }}
        />
      ) : null}

      <ContextMenu state={ctx.state} onClose={ctx.close} />
      <FileRenameModal target={renameTarget} onClose={() => setRenameTarget(null)} />
      <FileDetailModal target={detailTarget} onClose={() => setDetailTarget(null)} />
      <FileCreateDirModal
        open={createDirOpen}
        parentPath={parentPath}
        onClose={() => setCreateDirOpen(false)}
      />
      <FileRecycleModal open={recycleOpen} onClose={() => setRecycleOpen(false)} />
      <MultipartUploaderModal
        open={multipartOpen}
        parentPath={parentPath}
        onClose={() => setMultipartOpen(false)}
      />
      <ImagePreview
        images={
          imagePreview?.images.map((it) => ({
            id: it.id,
            url: it.url ?? "",
            name: it.originalName,
          })) ?? null
        }
        initialIndex={imagePreview?.index ?? 0}
        onClose={() => setImagePreview(null)}
      />
      <VideoPreview
        src={videoPreview?.url ?? null}
        name={videoPreview?.originalName}
        contentType={videoPreview?.contentType}
        onClose={() => setVideoPreview(null)}
      />
      <AudioPreview
        src={audioPreview?.url ?? null}
        name={audioPreview?.originalName}
        contentType={audioPreview?.contentType}
        onClose={() => setAudioPreview(null)}
      />
      <OfficePreview
        url={officePreview?.url ?? null}
        name={officePreview?.originalName}
        extension={officePreview?.extension}
        onClose={() => setOfficePreview(null)}
      />
    </section>
  );
}
