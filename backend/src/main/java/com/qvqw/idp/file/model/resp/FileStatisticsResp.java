package com.qvqw.idp.file.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件资源统计响应 DTO。
 *
 * <p>列表项形如 {@code {type:2,name:"图片",size:1024,number:3}}，前端用于绘制侧边栏统计图。</p>
 */
@Schema(description = "文件资源统计响应")
public class FileStatisticsResp {

    @Schema(description = "类型整数（仅明细项有值）")
    private Integer type;

    @Schema(description = "类型名称（仅明细项有值）")
    private String name;

    @Schema(description = "类型总大小（字节）")
    private Long size = 0L;

    @Schema(description = "文件数量")
    private Long number = 0L;

    @Schema(description = "分类明细数组（仅总览项有值）")
    private List<FileStatisticsResp> data = new ArrayList<>();

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public List<FileStatisticsResp> getData() {
        return data;
    }

    public void setData(List<FileStatisticsResp> data) {
        this.data = data;
    }
}
