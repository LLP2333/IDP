package com.qvqw.idp.file;

import com.qvqw.idp.common.api.R;
import com.qvqw.idp.file.model.req.MultipartUploadInitReq;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.file.model.resp.MultipartUploadInitResp;
import com.qvqw.idp.file.model.resp.MultipartUploadPartResp;
import com.qvqw.idp.menu.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 分片上传 API。
 *
 * <p>典型调用顺序：</p>
 * <pre>
 *   POST /system/multipart-upload                 -> init
 *   PUT  /system/multipart-upload/{uploadId}/part -> 反复上传分片
 *   POST /system/multipart-upload/{uploadId}      -> complete
 *   DEL  /system/multipart-upload/{uploadId}      -> cancel
 * </pre>
 */
@Tag(name = "分片上传", description = "支持断点续传与秒传的大文件上传协议")
@RestController
@RequestMapping("/system/multipart-upload")
public class MultipartUploadController {

    private final MultipartUploadService multipartUploadService;

    public MultipartUploadController(MultipartUploadService multipartUploadService) {
        this.multipartUploadService = multipartUploadService;
    }

    /**
     * 初始化分片上传。
     */
    @Operation(summary = "初始化分片上传", description = "命中 SHA256 时直接返回已有文件（秒传）")
    @HasPermission("system:file:upload")
    @PostMapping
    public R<MultipartUploadInitResp> init(@RequestBody @Valid MultipartUploadInitReq req) {
        return R.ok(multipartUploadService.init(req));
    }

    /**
     * 上传单个分片。
     */
    @Operation(summary = "上传分片")
    @HasPermission("system:file:upload")
    @PutMapping("/{uploadId}/part")
    public R<MultipartUploadPartResp> uploadPart(@Parameter(description = "uploadId") @PathVariable String uploadId,
                                                 @Parameter(description = "分片编号，从 1 开始") @RequestParam("partNumber") int partNumber,
                                                 @Parameter(description = "分片内容") @RequestParam("file") MultipartFile file)
            throws IOException {
        return R.ok(multipartUploadService.uploadPart(uploadId, partNumber, file));
    }

    /**
     * 完成分片上传：合并 + 入库。
     */
    @Operation(summary = "完成分片上传", description = "合并所有分片并写入数据库")
    @HasPermission("system:file:upload")
    @PostMapping("/{uploadId}")
    public R<FileResp> complete(@Parameter(description = "uploadId") @PathVariable String uploadId) {
        return R.ok(multipartUploadService.complete(uploadId));
    }

    /**
     * 取消分片上传。
     */
    @Operation(summary = "取消分片上传")
    @HasPermission("system:file:upload")
    @DeleteMapping("/{uploadId}")
    public R<Void> cancel(@Parameter(description = "uploadId") @PathVariable String uploadId) {
        multipartUploadService.cancel(uploadId);
        return R.ok();
    }
}
