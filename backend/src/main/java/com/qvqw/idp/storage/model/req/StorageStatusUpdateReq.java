package com.qvqw.idp.storage.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 切换存储状态请求体。
 */
@Schema(description = "切换存储启用 / 禁用状态请求")
public class StorageStatusUpdateReq {

    @Schema(description = "目标状态：1=启用，2=禁用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
