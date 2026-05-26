package com.qvqw.idp.file.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件夹大小计算响应 DTO。
 */
@Schema(description = "文件夹大小响应")
public class FileDirCalcSizeResp {

    @Schema(description = "文件夹总大小（字节）")
    private Long size;

    public FileDirCalcSizeResp() {
    }

    public FileDirCalcSizeResp(Long size) {
        this.size = size;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
