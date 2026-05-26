package com.qvqw.idp.file.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建文件夹请求体。
 */
@Schema(description = "创建文件夹请求")
public class FileCreateDirReq {

    @Schema(description = "上级目录绝对路径（以 / 开头，根目录传 /）", example = "/", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "上级目录不能为空")
    @Pattern(regexp = "^/.*", message = "上级目录必须以 / 开头")
    private String parentPath;

    @Schema(description = "文件夹名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件夹名不能为空")
    @Size(max = 200, message = "文件夹名长度不能超过 200 个字符")
    private String originalName;

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
}
