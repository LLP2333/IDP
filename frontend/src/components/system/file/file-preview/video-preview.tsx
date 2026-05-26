"use client";

import { Button } from "~/components/ui/button";
import { Modal } from "~/components/ui/modal";

/**
 * `VideoPreview` Props。
 */
export interface VideoPreviewProps {
  /** 视频 URL，null 表示关闭。 */
  src: string | null;
  /** 文件名，用作标题。 */
  name?: string;
  /** MIME。 */
  contentType?: string | null;
  /** 关闭回调。 */
  onClose: () => void;
}

/**
 * 视频预览模态框：直接用 HTML `<video>`。
 */
export function VideoPreview({ src, name, contentType, onClose }: VideoPreviewProps) {
  return (
    <Modal
      open={!!src}
      onClose={onClose}
      title={name ?? "视频预览"}
      size="lg"
      footer={
        <Button variant="outline" onClick={onClose}>
          关闭
        </Button>
      }
    >
      {src ? (
        <video
          controls
          autoPlay
          src={src}
          className="max-h-[60vh] w-full rounded bg-black"
        >
          {contentType ? <source src={src} type={contentType} /> : null}
          您的浏览器不支持 video 标签。
        </video>
      ) : null}
    </Modal>
  );
}
