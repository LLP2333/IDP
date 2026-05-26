package com.qvqw.idp.file;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.file.model.query.FileQuery;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.menu.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件回收站 API。
 */
@Tag(name = "文件回收站", description = "回收站的列表、还原、删除、清空")
@RestController
@RequestMapping("/system/file/recycle")
public class FileRecycleController {

    private final FileRecycleService fileRecycleService;

    public FileRecycleController(FileRecycleService fileRecycleService) {
        this.fileRecycleService = fileRecycleService;
    }

    /**
     * 回收站分页。
     */
    @Operation(summary = "回收站分页", description = "按原始名 / 类型过滤；按删除时间倒序")
    @HasPermission("system:fileRecycle:list")
    @GetMapping
    public R<PageResp<FileResp>> page(FileQuery query,
                                      @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(fileRecycleService.page(query, page, size));
    }

    /**
     * 还原。
     */
    @Operation(summary = "还原")
    @HasPermission("system:fileRecycle:restore")
    @PutMapping("/{id}")
    public R<Void> restore(@Parameter(description = "文件 ID") @PathVariable Long id) {
        fileRecycleService.restore(id);
        return R.ok();
    }

    /**
     * 物理删除单条。
     */
    @Operation(summary = "物理删除")
    @HasPermission("system:fileRecycle:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "文件 ID") @PathVariable Long id) {
        fileRecycleService.delete(id);
        return R.ok();
    }

    /**
     * 清空。
     */
    @Operation(summary = "清空回收站")
    @HasPermission("system:fileRecycle:clean")
    @DeleteMapping
    public R<Void> clean() {
        fileRecycleService.clean();
        return R.ok();
    }
}
