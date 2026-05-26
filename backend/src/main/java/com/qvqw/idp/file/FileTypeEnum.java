package com.qvqw.idp.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件类型枚举。
 *
 * <p>用于前端按类型筛选与图标渲染；DIR 为文件夹特殊类型，其余按扩展名归类。</p>
 */
public enum FileTypeEnum {

    /** 文件夹。 */
    DIR(0, "目录", Collections.emptyList()),

    /** 其他（未归类）。 */
    UNKNOWN(1, "其他", Collections.emptyList()),

    /** 图片。 */
    IMAGE(2, "图片", List.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "ico", "svg", "tiff")),

    /** 文档。 */
    DOC(3, "文档", List.of("txt", "pdf", "doc", "xls", "ppt", "docx", "xlsx", "pptx", "csv", "md", "rtf")),

    /** 视频。 */
    VIDEO(4, "视频", List.of("mp4", "avi", "mkv", "flv", "webm", "wmv", "m4v", "mov", "mpg", "rmvb", "3gp")),

    /** 音频。 */
    AUDIO(5, "音频", List.of("mp3", "flac", "wav", "ogg", "midi", "m4a", "aac", "amr", "ac3", "aiff"));

    private final int value;
    private final String description;
    private final List<String> extensions;

    FileTypeEnum(int value, String description, List<String> extensions) {
        this.value = value;
        this.description = description;
        this.extensions = extensions;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * 按扩展名查询文件类型。
     *
     * @param extension 扩展名（不带 {@code .}，大小写无关）
     * @return 文件类型；未知扩展名返回 {@link #UNKNOWN}
     */
    public static FileTypeEnum getByExtension(String extension) {
        if (extension == null) {
            return UNKNOWN;
        }
        String ext = extension.toLowerCase();
        return Arrays.stream(values())
                .filter(t -> t.extensions.contains(ext))
                .findFirst()
                .orElse(UNKNOWN);
    }

    /**
     * 按整型值反查文件类型；未知返回 {@code null}。
     */
    public static FileTypeEnum ofValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (FileTypeEnum t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        return null;
    }

    /**
     * 全部支持的扩展名（用于上传白名单校验）。
     */
    public static Set<String> allExtensions() {
        return Arrays.stream(values()).flatMap(t -> t.extensions.stream()).collect(Collectors.toSet());
    }
}
