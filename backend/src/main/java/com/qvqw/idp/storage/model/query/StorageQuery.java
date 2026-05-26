package com.qvqw.idp.storage.model.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 存储查询条件。
 */
@Schema(description = "存储查询参数")
public class StorageQuery {

    @Schema(description = "存储类型过滤：1=本地，2=S3")
    private Integer type;

    @Schema(description = "搜索关键字（在名称、编码、描述上做 like 匹配）")
    private String keyword;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
