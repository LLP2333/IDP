"use client";

import { forwardRef, type SelectHTMLAttributes } from "react";

import { cn } from "~/lib/utils";

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  invalid?: boolean;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, invalid, ...rest }, ref) => (
    <select
      ref={ref}
      className={cn(
        "h-9 w-full rounded-md border bg-white px-2 text-sm transition-colors",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-1",
        invalid
          ? "border-red-400 focus-visible:ring-red-400"
          : "border-zinc-300 focus-visible:ring-blue-500",
        "disabled:cursor-not-allowed disabled:bg-zinc-50",
        className,
      )}
      {...rest}
    />
  ),
);
Select.displayName = "Select";
