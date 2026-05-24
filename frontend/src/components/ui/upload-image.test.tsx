import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { UploadImage } from "./upload-image";

vi.mock("~/lib/api/option", () => ({
  uploadOptionImage: vi.fn(),
}));
import { uploadOptionImage } from "~/lib/api/option";

const mockedUpload = uploadOptionImage as unknown as ReturnType<typeof vi.fn>;

beforeEach(() => {
  mockedUpload.mockReset();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("UploadImage", () => {
  it("超过 maxSize 时显示错误且不上传", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <UploadImage code="SITE_LOGO" value={null} onChange={onChange} maxSize={10} />,
    );
    const file = new File([new Uint8Array(20)], "x.png", { type: "image/png" });
    const input = document.querySelector('input[type="file"]')!;
    await user.upload(input as HTMLInputElement, file);
    expect(await screen.findByText(/文件超过/)).toBeInTheDocument();
    expect(mockedUpload).not.toHaveBeenCalled();
  });

  it("点击 “清空” 触发 onChange(null)", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<UploadImage code="SITE_LOGO" value="data:image/png;base64,YYY" onChange={onChange} />);
    await user.click(screen.getByText("清空"));
    expect(onChange).toHaveBeenCalledWith(null);
  });
});
