package com.qvqw.idp.file;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.file.model.query.FileQuery;
import com.qvqw.idp.file.model.req.FileCreateDirReq;
import com.qvqw.idp.file.model.req.FileUpdateReq;
import com.qvqw.idp.file.model.resp.FileDirCalcSizeResp;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.file.model.resp.FileStatisticsResp;
import com.qvqw.idp.file.model.resp.FileUploadResp;
import com.qvqw.idp.menu.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件管理 API。
 *
 * <p>覆盖：上传 / 列表 / 重命名 / 删除 / 统计 / 秒传校验 / 创建文件夹 / 计算文件夹大小。
 * 所有接口需要 {@code system:file:*} 权限。</p>
 */
@Tag(name = "文件管理", description = "文件 / 文件夹 CRUD、统计、秒传校验、计算文件夹大小")
@RestController
@RequestMapping("/system/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 普通上传（适合 ≤ 100MB 的文件，超过应走分片上传）。
     */
    @Operation(summary = "上传文件", description = "multipart/form-data；超过 100MB 请使用分片上传接口")
    @HasPermission("system:file:upload")
    @PostMapping
    public R<FileUploadResp> upload(@Parameter(description = "上传文件") @RequestParam("file") MultipartFile file,
                                    @Parameter(description = "上级目录，默认 /") @RequestParam(value = "parentPath", required = false, defaultValue = "/") String parentPath)
            throws IOException {
        return R.ok(fileService.upload(file, parentPath));
    }

    /**
     * 文件分页列表（不含回收站）。
     */
    @Operation(summary = "文件分页", description = "按原始名 + 父目录 + 类型过滤")
    @HasPermission("system:file:list")
    @GetMapping
    public R<PageResp<FileResp>> page(FileQuery query,
                                      @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(fileService.page(query, page, size));
    }

    /**
     * 重命名。
     */
    @Operation(summary = "重命名")
    @HasPermission("system:file:update")
    @PutMapping("/{id}")
    public R<Void> rename(@Parameter(description = "文件 ID") @PathVariable Long id,
                          @RequestBody @Valid FileUpdateReq req) {
        fileService.rename(id, req);
        return R.ok();
    }

    /**
     * 批量删除（按存储设置自动走回收站或物理删除）。
     */
    @Operation(summary = "批量删除", description = "请求体形如 {\"ids\":[1,2,3]}")
    @HasPermission("system:file:delete")
    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        fileService.delete(req.getIds());
        return R.ok();
    }

    /**
     * 资源统计。
     */
    @Operation(summary = "资源统计", description = "按类型聚合返回总览 + 明细")
    @HasPermission("system:file:list")
    @GetMapping("/statistics")
    public R<FileStatisticsResp> statistics() {
        return R.ok(fileService.statistics());
    }

    /**
     * 秒传校验。
     */
    @Operation(summary = "按 SHA256 校验秒传", description = "命中已有文件时返回，否则 data=null")
    @HasPermission("system:file:upload")
    @GetMapping("/check")
    public R<FileResp> check(@Parameter(description = "文件 SHA256") @RequestParam("fileHash") String fileHash) {
        return R.ok(fileService.check(fileHash));
    }

    /**
     * 创建文件夹。
     */
    @Operation(summary = "创建文件夹")
    @HasPermission("system:file:createDir")
    @PostMapping("/dir")
    public R<Long> createDir(@RequestBody @Valid FileCreateDirReq req) {
        return R.ok(fileService.createDir(req));
    }

    /**
     * 计算文件夹大小。
     */
    @Operation(summary = "计算文件夹大小")
    @HasPermission("system:file:calcDirSize")
    @GetMapping("/{id}/size")
    public R<FileDirCalcSizeResp> calcDirSize(@Parameter(description = "文件夹 ID") @PathVariable Long id) {
        return R.ok(new FileDirCalcSizeResp(fileService.calcDirSize(id)));
    }

    /**
     * 通用的 “按 ID 列表批量删除” 请求体。
     */
    @Schema(description = "按 ID 列表批量删除请求体")
    public static class DeleteIdsReq {

        @Schema(description = "待删除的 ID 列表", example = "[1,2,3]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "ID 列表不能为空")
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }
}
