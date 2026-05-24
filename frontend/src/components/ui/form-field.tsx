"use client";

import { type ReactNode } from "react";

import { cn } from "~/lib/utils";

interface FormFieldProps {
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  className?: string;
  children: ReactNode;
}

export function FormField({
  label,
  required,
  error,
  hint,
  className,
  children,
}: FormFieldProps) {
  return (
    <div className={cn("flex flex-col gap-1", className)}>
      <label className="text-xs font-medium text-zinc-700">
        {label}
        {required ? <span className="ml-0.5 text-red-500">*</span> : null}
      </label>
      {children}
      {error ? <span className="text-xs text-red-500">{error}</span> : null}
      {!error && hint ? (
        <span className="text-xs text-zinc-400">{hint}</span>
      ) : null}
    </div>
  );
}
