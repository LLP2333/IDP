"use client";

import { forwardRef, type InputHTMLAttributes } from "react";

import { cn } from "~/lib/utils";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, invalid, ...rest }, ref) => (
    <input
      ref={ref}
      className={cn(
        "h-9 w-full rounded-md border bg-white px-3 text-sm transition-colors",
        "placeholder:text-zinc-400",
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
Input.displayName = "Input";
