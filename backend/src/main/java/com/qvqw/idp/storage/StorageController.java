package com.qvqw.idp.storage;

import com.qvqw.idp.common.api.R;
import com.qvqw.idp.menu.annotation.HasPermission;
import com.qvqw.idp.storage.model.query.StorageQuery;
import com.qvqw.idp.storage.model.req.StorageCreateReq;
import com.qvqw.idp.storage.model.req.StorageStatusUpdateReq;
import com.qvqw.idp.storage.model.req.StorageUpdateReq;
import com.qvqw.idp.storage.model.resp.StorageResp;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 存储管理 API。
 *
 * <p>所有接口需要 {@code system:storage:*} 权限；前端通过 {@code /admin/system/config}
 * 下的 “存储配置” Tab 调用。</p>
 */
@Tag(name = "存储管理", description = "本地存储 / S3 存储引擎的 CRUD、状态、默认设置")
@RestController
@RequestMapping("/system/storage")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * 存储列表。
     */
    @Operation(summary = "存储列表", description = "按类型 / 关键字过滤；按 sort 升序")
    @HasPermission("system:storage:list")
    @GetMapping("/list")
    public R<List<StorageResp>> list(StorageQuery query) {
        return R.ok(storageService.list(query));
    }

    /**
     * 存储详情。
     */
    @Operation(summary = "存储详情")
    @HasPermission("system:storage:get")
    @GetMapping("/{id}")
    public R<StorageResp> get(@Parameter(description = "存储 ID") @PathVariable Long id) {
        return R.ok(storageService.get(id));
    }

    /**
     * 新增存储。
     */
    @Operation(summary = "新增存储")
    @HasPermission("system:storage:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid StorageCreateReq req) {
        return R.ok(storageService.create(req));
    }

    /**
     * 修改存储。
     */
    @Operation(summary = "修改存储")
    @HasPermission("system:storage:update")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "存储 ID") @PathVariable Long id,
                          @RequestBody @Valid StorageUpdateReq req) {
        storageService.update(id, req);
        return R.ok();
    }

    /**
     * 批量删除存储。
     */
    @Operation(summary = "批量删除存储", description = "请求体形如 {\"ids\":[1,2,3]}")
    @HasPermission("system:storage:delete")
    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        storageService.delete(req.getIds());
        return R.ok();
    }

    /**
     * 切换状态。
     */
    @Operation(summary = "切换状态")
    @HasPermission("system:storage:updateStatus")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@Parameter(description = "存储 ID") @PathVariable Long id,
                                @RequestBody @Valid StorageStatusUpdateReq req) {
        storageService.updateStatus(id, req);
        return R.ok();
    }

    /**
     * 设为默认存储。
     */
    @Operation(summary = "设为默认存储")
    @HasPermission("system:storage:setDefault")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@Parameter(description = "存储 ID") @PathVariable Long id) {
        storageService.setDefault(id);
        return R.ok();
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
