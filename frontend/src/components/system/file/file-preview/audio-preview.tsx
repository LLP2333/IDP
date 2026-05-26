"use client";

import { Button } from "~/components/ui/button";
import { Modal } from "~/components/ui/modal";

/**
 * `AudioPreview` Props。
 */
export interface AudioPreviewProps {
  /** 音频 URL，null 表示关闭。 */
  src: string | null;
  /** 文件名。 */
  name?: string;
  /** MIME。 */
  contentType?: string | null;
  /** 关闭回调。 */
  onClose: () => void;
}

/**
 * 音频预览模态框：用 HTML `<audio>`。
 */
export function AudioPreview({ src, name, contentType, onClose }: AudioPreviewProps) {
  return (
    <Modal
      open={!!src}
      onClose={onClose}
      title={name ?? "音频预览"}
      size="sm"
      footer={
        <Button variant="outline" onClick={onClose}>
          关闭
        </Button>
      }
    >
      {src ? (
        <audio controls autoPlay src={src} className="w-full">
          {contentType ? <source src={src} type={contentType} /> : null}
          您的浏览器不支持 audio 标签。
        </audio>
      ) : null}
      {/* TODO: 后续接入 onlyoffice/kkfile 时增加 Office 预览 */}
    </Modal>
  );
}
