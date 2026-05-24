package com.qvqw.idp.permission;

import com.qvqw.idp.common.api.R;
import com.qvqw.idp.permission.annotation.HasPermission;
import com.qvqw.idp.permission.model.query.PermissionQuery;
import com.qvqw.idp.permission.model.req.PermissionReq;
import com.qvqw.idp.permission.model.resp.PermissionResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
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
 * 权限管理 API。
 *
 * <p>提供权限 CRUD 与树形结构查询；分配权限到角色的操作在 {@code /system/role/{id}/permission}。</p>
 */
@Tag(name = "权限管理", description = "权限 CRUD + 树形结构")
@RestController
@RequestMapping("/system/permission")
@Validated
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "权限列表")
    @HasPermission("system:permission:list")
    @GetMapping
    public R<List<PermissionResp>> list(PermissionQuery query) {
        return R.ok(permissionService.list(query));
    }

    @Operation(summary = "权限树", description = "按父子层级返回；前端权限选择树直接消费此结构")
    @HasPermission("system:permission:list")
    @GetMapping("/tree")
    public R<List<PermissionResp>> tree() {
        return R.ok(permissionService.tree());
    }

    @Operation(summary = "权限详情")
    @HasPermission("system:permission:list")
    @GetMapping("/{id}")
    public R<PermissionResp> get(@Parameter(description = "权限 ID") @PathVariable Long id) {
        return R.ok(permissionService.get(id));
    }

    @Operation(summary = "新增权限")
    @HasPermission("system:permission:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid PermissionReq req) {
        return R.ok(permissionService.create(req));
    }

    @Operation(summary = "修改权限")
    @HasPermission("system:permission:update")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "权限 ID") @PathVariable Long id,
                          @RequestBody @Valid PermissionReq req) {
        permissionService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "批量删除权限", description = "请求体形如 {\"ids\":[1,2,3]}")
    @HasPermission("system:permission:delete")
    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        permissionService.delete(req.getIds());
        return R.ok();
    }

    /** 批量删除通用请求体。 */
    @Schema(description = "按 ID 列表批量删除请求体")
    public static class DeleteIdsReq {

        @Schema(description = "权限 ID 列表", example = "[1,2,3]", requiredMode = Schema.RequiredMode.REQUIRED)
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
