package com.qvqw.idp.file.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分片上传单分片响应 DTO。
 */
@Schema(description = "分片上传单分片响应")
public class MultipartUploadPartResp {

    @Schema(description = "分片编号")
    private Integer partNumber;

    @Schema(description = "分片 ETag")
    private String etag;

    public MultipartUploadPartResp() {
    }

    public MultipartUploadPartResp(Integer partNumber, String etag) {
        this.partNumber = partNumber;
        this.etag = etag;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}
