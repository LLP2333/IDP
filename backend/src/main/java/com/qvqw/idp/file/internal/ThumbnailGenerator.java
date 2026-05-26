package com.qvqw.idp.file.internal;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 图片缩略图生成器。
 *
 * <p>使用 thumbnailator 把原图缩放到 100x100（保持比例，长边对齐），输出 JPEG 字节。
 * 失败时返回 {@code null}，由调用方决定是否吞掉异常（参考项目的 {@code setIgnoreThumbnailException} 行为）。</p>
 */
@Component
class ThumbnailGenerator {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailGenerator.class);
    private static final int SIZE = 100;

    /**
     * 生成缩略图字节。
     *
     * @param data 原图字节
     * @return JPEG 字节；失败返回 {@code null}
     */
    byte[] generate(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try (InputStream in = new ByteArrayInputStream(data);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(in).size(SIZE, SIZE).outputFormat("jpg").toOutputStream(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.debug("生成缩略图失败：{}", e.getMessage());
            return null;
        }
    }
}
