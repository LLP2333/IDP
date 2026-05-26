package com.qvqw.idp.file.model.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件分页查询条件。
 */
@Schema(description = "文件查询条件")
public class FileQuery {

    @Schema(description = "在原始名上模糊搜索")
    private String originalName;

    @Schema(description = "上级目录绝对路径。type 指定时此字段忽略", example = "/")
    private String parentPath;

    @Schema(description = "文件类型整数：0=目录,1=其他,2=图片,3=文档,4=视频,5=音频")
    private Integer type;

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
