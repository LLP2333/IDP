"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { Download, Eye, LockKeyhole, RefreshCw, Search, Shuffle } from "lucide-react";
import type { ReactNode } from "react";
import { useState } from "react";
import { toast } from "sonner";

import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { DataTable, Pagination, type ColumnDef } from "~/components/ui/data-table";
import { Input } from "~/components/ui/input";
import { Modal } from "~/components/ui/modal";
import { Select } from "~/components/ui/select";
import { Tabs } from "~/components/ui/tabs";
import { HttpError } from "~/lib/api/http";
import {
  exportLoginLog,
  exportOperationLog,
  getLog,
  listLog,
} from "~/lib/api/monitor";
import type { LogDetailResp, LogPageQuery, LogResp } from "~/lib/api/types";
import { usePermission } from "~/lib/hooks/use-permission";
import { formatDateTime } from "~/lib/utils";

const PAGE_SIZE = 10;

function toInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function defaultStartInput() {
  const date = new Date();
  date.setDate(date.getDate() - 6);
  date.setHours(0, 0, 0, 0);
  return toInputValue(date);
}

function defaultEndInput() {
  const date = new Date();
  date.setHours(23, 59, 0, 0);
  return toInputValue(date);
}

function normalizeDateTime(value: string): string | undefined {
  return value ? value.replace("T", " ") + ":00" : undefined;
}

function buildRange(start: string, end: string): string[] | undefined {
  const startValue = normalizeDateTime(start);
  const endValue = normalizeDateTime(end);
  return startValue && endValue ? [startValue, endValue] : undefined;
}

function statusBadge(status: number, errorMsg?: string | null) {
  return status === 1 ? (
    <Badge tone="success">成功</Badge>
  ) : (
    <span title={errorMsg ?? undefined}>
      <Badge tone="danger">失败</Badge>
    </span>
  );
}

function timeTakenBadge(ms: number) {
  if (ms > 500) return <Badge tone="danger">{ms}ms</Badge>;
  if (ms > 200) return <Badge tone="warning">{ms}ms</Badge>;
  return <Badge tone="success">{ms}ms</Badge>;
}

function prettyPayload(value: string | null | undefined): string {
  if (!value) return "无";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function displayDateTime(value: string): { date: string; time: string } {
  // 复用统一的格式化逻辑：先归一为 "YYYY-MM-DD HH:mm:ss"，再按空格拆分双行展示
  const normalized = formatDateTime(value, "");
  if (!normalized) return { date: "—", time: "" };
  const [date, time] = normalized.split(" ");
  return { date: date ?? normalized, time: time ?? "" };
}

function fallback(value: string | null | undefined): string {
  return value?.trim() ? value : "—";
}

interface LogFilterProps {
  extraLeft?: ReactNode;
  actorPlaceholder: string;
  actorInput: string;
  setActorInput: (value: string) => void;
  ipInput: string;
  setIpInput: (value: string) => void;
  startInput: string;
  setStartInput: (value: string) => void;
  endInput: string;
  setEndInput: (value: string) => void;
  status: number | "";
  setStatus: (value: number | "") => void;
  onSearch: () => void;
  onReset: () => void;
  onExport?: () => void;
  exportPending?: boolean;
}

function LogFilter({
  extraLeft,
  actorPlaceholder,
  actorInput,
  setActorInput,
  ipInput,
  setIpInput,
  startInput,
  setStartInput,
  endInput,
  setEndInput,
  status,
  setStatus,
  onSearch,
  onReset,
  onExport,
  exportPending,
}: LogFilterProps) {
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-md border border-zinc-200 bg-white p-3">
      {extraLeft}
      <Input
        className="!w-48"
        placeholder={actorPlaceholder}
        value={actorInput}
        onChange={(e) => setActorInput(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") onSearch();
        }}
      />
      <Input
        className="!w-52"
        placeholder="搜索 IP 或地点"
        value={ipInput}
        onChange={(e) => setIpInput(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") onSearch();
        }}
      />
      <Input
        className="!w-52"
        type="datetime-local"
        value={startInput}
        onChange={(e) => setStartInput(e.target.value)}
      />
      <Input
        className="!w-52"
        type="datetime-local"
        value={endInput}
        onChange={(e) => setEndInput(e.target.value)}
      />
      <Select
        className="!w-28"
        value={status}
        onChange={(e) => {
          const value = e.target.value;
          setStatus(value === "" ? "" : Number(value));
        }}
      >
        <option value="">全部状态</option>
        <option value="1">成功</option>
        <option value="2">失败</option>
      </Select>
      <Button variant="outline" size="sm" onClick={onSearch}>
        <Search size={14} />
        搜索
      </Button>
      <Button variant="outline" size="sm" onClick={onReset}>
        <RefreshCw size={14} />
        重置
      </Button>
      {onExport ? (
        <Button
          variant="outline"
          size="sm"
          disabled={exportPending}
          onClick={onExport}
        >
          <Download size={14} />
          导出
        </Button>
      ) : null}
    </div>
  );
}

function LoginLogTable() {
  const { hasPermission } = usePermission();
  const [page, setPage] = useState(1);
  const [actorInput, setActorInput] = useState("");
  const [actor, setActor] = useState("");
  const [ipInput, setIpInput] = useState("");
  const [ip, setIp] = useState("");
  const [startInput, setStartInput] = useState(defaultStartInput);
  const [endInput, setEndInput] = useState(defaultEndInput);
  const [range, setRange] = useState(() => buildRange(startInput, endInput));
  const [statusInput, setStatusInput] = useState<number | "">("");
  const [status, setStatus] = useState<number | "">("");

  const query: LogPageQuery = {
    page,
    size: PAGE_SIZE,
    module: "登录",
    createUserString: actor || undefined,
    ip: ip || undefined,
    status: status === "" ? undefined : status,
    createTime: range,
    sort: ["createTime,desc"],
  };

  const listQuery = useQuery({
    queryKey: ["monitor", "log", "login", query],
    queryFn: () => listLog(query),
  });

  const exportMutation = useMutation({
    mutationFn: () => exportLoginLog(query),
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "导出失败"),
  });

  const applySearch = () => {
    setPage(1);
    setActor(actorInput);
    setIp(ipInput);
    setStatus(statusInput);
    setRange(buildRange(startInput, endInput));
  };

  const reset = () => {
    const start = defaultStartInput();
    const end = defaultEndInput();
    setPage(1);
    setActorInput("");
    setActor("");
    setIpInput("");
    setIp("");
    setStartInput(start);
    setEndInput(end);
    setRange(buildRange(start, end));
    setStatusInput("");
    setStatus("");
  };

  const columns: ColumnDef<LogResp>[] = [
    {
      key: "index",
      title: "序号",
      width: "70px",
      align: "center",
      render: (_row, index) => (page - 1) * PAGE_SIZE + index + 1,
    },
    {
      key: "createTime",
      title: "登录时间",
      width: "180px",
      render: (row) => (
        <span className="whitespace-nowrap">{formatDateTime(row.createTime)}</span>
      ),
    },
    { key: "createUserString", title: "用户昵称", render: (row) => row.createUserString ?? "—" },
    { key: "description", title: "登录行为", render: (row) => row.description ?? "—" },
    {
      key: "status",
      title: "状态",
      width: "90px",
      align: "center",
      render: (row) => statusBadge(row.status, row.errorMsg),
    },
    { key: "ip", title: "登录 IP", render: (row) => row.ip ?? "—" },
    { key: "address", title: "登录地点", render: (row) => row.address ?? "—" },
    { key: "browser", title: "浏览器", render: (row) => row.browser ?? "—" },
    { key: "os", title: "终端系统", render: (row) => row.os ?? "—" },
  ];

  const data = listQuery.data;

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <LogFilter
        actorPlaceholder="搜索登录用户"
        actorInput={actorInput}
        setActorInput={setActorInput}
        ipInput={ipInput}
        setIpInput={setIpInput}
        startInput={startInput}
        setStartInput={setStartInput}
        endInput={endInput}
        setEndInput={setEndInput}
        status={statusInput}
        setStatus={setStatusInput}
        onSearch={applySearch}
        onReset={reset}
        onExport={
          hasPermission("monitor:log:export")
            ? () => exportMutation.mutate()
            : undefined
        }
        exportPending={exportMutation.isPending}
      />
      <DataTable
        columns={columns}
        data={data?.list ?? []}
        rowKey={(row) => row.id}
        loading={listQuery.isLoading}
        stickyHeader
        containerClassName="min-h-0 flex-1 overflow-auto"
        tableClassName="min-w-[1120px]"
      />
      {data ? (
        <Pagination
          page={data.page}
          size={data.size}
          total={data.total}
          onPageChange={setPage}
        />
      ) : null}
    </div>
  );
}

function LogDetailModal({
  detail,
  onClose,
}: {
  detail: LogDetailResp | null;
  onClose: () => void;
}) {
  return (
    <Modal open={!!detail} onClose={onClose} title="日志详情" size="lg">
      {detail ? (
        <div className="max-h-[75vh] space-y-5 overflow-auto text-sm">
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <Info label="日志 ID" value={detail.id} />
            <Info label="Trace ID" value={detail.traceId ?? "—"} />
            <Info label="操作人" value={detail.createUserString ?? "—"} />
            <Info label="操作时间" value={formatDateTime(detail.createTime)} />
            <Info label="操作内容" value={detail.description ?? "—"} />
            <Info label="所属模块" value={detail.module ?? "—"} />
            <Info label="操作 IP" value={detail.ip ?? "—"} />
            <Info label="操作地点" value={detail.address ?? "—"} />
            <Info label="浏览器" value={detail.browser ?? "—"} />
            <Info label="终端系统" value={detail.os ?? "—"} />
            <Info label="状态" value={detail.status === 1 ? "成功" : "失败"} />
            <Info label="耗时" value={`${detail.timeTaken}ms`} />
            <Info label="请求方法" value={detail.requestMethod ?? "—"} />
            <Info label="状态码" value={detail.statusCode?.toString() ?? "—"} />
          </div>
          <Info label="请求 URI" value={detail.requestUrl ?? "—"} wide />
          <Payload title="响应头" value={detail.responseHeaders} />
          <Payload title="响应体" value={detail.responseBody} />
          <Payload title="请求头" value={detail.requestHeaders} />
          <Payload title="请求体" value={detail.requestBody} />
        </div>
      ) : null}
    </Modal>
  );
}

function Info({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <div className={wide ? "md:col-span-2" : undefined}>
      <div className="text-xs text-zinc-400">{label}</div>
      <div className="mt-1 break-all text-zinc-800">{value}</div>
    </div>
  );
}

function Payload({ title, value }: { title: string; value: string | null | undefined }) {
  return (
    <div>
      <div className="mb-2 font-medium text-zinc-800">{title}</div>
      <pre className="max-h-56 overflow-auto rounded-md bg-zinc-950 p-3 text-xs leading-relaxed text-zinc-100">
        {prettyPayload(value)}
      </pre>
    </div>
  );
}

function OperationLogTable() {
  const { hasPermission } = usePermission();
  const [page, setPage] = useState(1);
  const [actorInput, setActorInput] = useState("");
  const [actor, setActor] = useState("");
  const [ipInput, setIpInput] = useState("");
  const [ip, setIp] = useState("");
  const [descriptionInput, setDescriptionInput] = useState("");
  const [description, setDescription] = useState("");
  const [startInput, setStartInput] = useState(defaultStartInput);
  const [endInput, setEndInput] = useState(defaultEndInput);
  const [range, setRange] = useState(() => buildRange(startInput, endInput));
  const [statusInput, setStatusInput] = useState<number | "">("");
  const [status, setStatus] = useState<number | "">("");
  const [detail, setDetail] = useState<LogDetailResp | null>(null);

  const query: LogPageQuery = {
    page,
    size: PAGE_SIZE,
    description: description || undefined,
    createUserString: actor || undefined,
    ip: ip || undefined,
    status: status === "" ? undefined : status,
    createTime: range,
    sort: ["createTime,desc"],
  };

  const listQuery = useQuery({
    queryKey: ["monitor", "log", "operation", query],
    queryFn: () => listLog(query),
  });

  const detailMutation = useMutation({
    mutationFn: getLog,
    onSuccess: setDetail,
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "加载详情失败"),
  });

  const exportMutation = useMutation({
    mutationFn: () => exportOperationLog(query),
    onError: (err: unknown) =>
      toast.error(err instanceof HttpError ? err.message : "导出失败"),
  });

  const applySearch = () => {
    setPage(1);
    setDescription(descriptionInput);
    setActor(actorInput);
    setIp(ipInput);
    setStatus(statusInput);
    setRange(buildRange(startInput, endInput));
  };

  const reset = () => {
    const start = defaultStartInput();
    const end = defaultEndInput();
    setPage(1);
    setDescriptionInput("");
    setDescription("");
    setActorInput("");
    setActor("");
    setIpInput("");
    setIp("");
    setStartInput(start);
    setEndInput(end);
    setRange(buildRange(start, end));
    setStatusInput("");
    setStatus("");
  };

  const columns: ColumnDef<LogResp>[] = [
    {
      key: "index",
      title: "序号",
      width: "64px",
      align: "center",
      render: (_row, index) => (page - 1) * PAGE_SIZE + index + 1,
    },
    {
      key: "createTime",
      title: "操作时间",
      width: "132px",
      render: (row) => {
        const time = displayDateTime(row.createTime);
        return (
          <div className="whitespace-nowrap leading-5">
            <div className="text-zinc-800">{time.date}</div>
            <div className="text-xs text-zinc-500">{time.time}</div>
          </div>
        );
      },
    },
    {
      key: "createUserString",
      title: "操作人",
      width: "150px",
      render: (row) => (
        <div className="max-w-36 break-all leading-5 text-zinc-800">
          {fallback(row.createUserString)}
        </div>
      ),
    },
    {
      key: "description",
      title: "操作内容",
      render: (row) => (
        <div className="min-w-56 max-w-xl leading-5">
          <div className="break-words text-zinc-800">{fallback(row.description)}</div>
          <div className="mt-1 text-xs text-zinc-500">{fallback(row.module)}</div>
        </div>
      ),
    },
    {
      key: "client",
      title: "客户端",
      width: "210px",
      render: (row) => (
        <div className="leading-5">
          <div className="break-all text-zinc-800">{fallback(row.ip)}</div>
          <div className="text-xs text-zinc-500">
            {fallback(row.address)}
          </div>
          <div className="mt-1 text-xs text-zinc-400">
            {[row.browser, row.os].filter(Boolean).join(" / ") || "—"}
          </div>
        </div>
      ),
    },
    {
      key: "result",
      title: "结果",
      width: "96px",
      align: "center",
      render: (row) => (
        <div className="inline-flex flex-col items-center gap-1">
          {statusBadge(row.status, row.errorMsg)}
          {timeTakenBadge(row.timeTaken)}
        </div>
      ),
    },
    {
      key: "actions",
      title: "操作",
      width: "90px",
      align: "right",
      render: (row) =>
        hasPermission("monitor:log:get") ? (
          <Button
            size="sm"
            variant="ghost"
            disabled={detailMutation.isPending}
            onClick={() => detailMutation.mutate(row.id)}
          >
            <Eye size={14} />
            详情
          </Button>
        ) : (
          "—"
        ),
    },
  ];

  const data = listQuery.data;

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <LogFilter
        extraLeft={
          <Input
            className="!w-48"
            placeholder="搜索操作内容"
            value={descriptionInput}
            onChange={(e) => setDescriptionInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") applySearch();
            }}
          />
        }
        actorPlaceholder="搜索操作人"
        actorInput={actorInput}
        setActorInput={setActorInput}
        ipInput={ipInput}
        setIpInput={setIpInput}
        startInput={startInput}
        setStartInput={setStartInput}
        endInput={endInput}
        setEndInput={setEndInput}
        status={statusInput}
        setStatus={setStatusInput}
        onSearch={applySearch}
        onReset={reset}
        onExport={
          hasPermission("monitor:log:export")
            ? () => exportMutation.mutate()
            : undefined
        }
        exportPending={exportMutation.isPending}
      />

      <DataTable
        columns={columns}
        data={data?.list ?? []}
        rowKey={(row) => row.id}
        loading={listQuery.isLoading}
        stickyHeader
        containerClassName="min-h-0 flex-1 overflow-auto"
        tableClassName="min-w-[900px]"
      />
      {data ? (
        <Pagination
          page={data.page}
          size={data.size}
          total={data.total}
          onPageChange={setPage}
        />
      ) : null}
      <LogDetailModal detail={detail} onClose={() => setDetail(null)} />
    </div>
  );
}

/** 系统日志页：复刻 ContiNew 登录日志 / 操作日志双 Tab 监控功能。 */
export default function MonitorLogPage() {
  const [tab, setTab] = useState("login");

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <div>
        <h2 className="text-xl font-semibold">系统日志</h2>
        <p className="mt-1 text-sm text-zinc-500">
          查看登录日志与操作日志，支持状态、时间、操作人和 IP 条件筛选。
        </p>
      </div>

      <Tabs
        value={tab}
        onChange={setTab}
        className="flex min-h-0 flex-1 flex-col"
        panelClassName="min-h-0 flex-1"
        items={[
          {
            key: "login",
            label: (
              <span className="inline-flex items-center gap-2">
                <LockKeyhole size={14} />
                登录日志
              </span>
            ),
            content: <LoginLogTable />,
          },
          {
            key: "operation",
            label: (
              <span className="inline-flex items-center gap-2">
                <Shuffle size={14} />
                操作日志
              </span>
            ),
            content: <OperationLogTable />,
          },
        ]}
      />
    </div>
  );
}
