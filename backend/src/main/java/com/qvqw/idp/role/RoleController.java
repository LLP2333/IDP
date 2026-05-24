package com.qvqw.idp.role;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.role.model.query.RoleQuery;
import com.qvqw.idp.role.model.req.RoleReq;
import com.qvqw.idp.role.model.resp.RoleResp;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理 API：分页、不分页列表、详情、新增、修改、删除、查询角色下用户。
 */
@Tag(name = "角色管理", description = "角色 CRUD 与查询角色下的用户")
@RestController
@RequestMapping("/system/role")
@Validated
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 角色分页查询。
     *
     * @param query 模糊匹配关键字 / 状态
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 分页结果
     */
    @Operation(summary = "角色分页", description = "按 name/code 关键字模糊匹配，可选 status 过滤。")
    @GetMapping
    public R<PageResp<RoleResp>> page(RoleQuery query,
                                      @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(roleService.page(query, page, size));
    }

    /**
     * 不分页查询全部角色（按 sort 升序），用于表单中的角色选择器等场景。
     *
     * @param query 状态过滤
     * @return 角色列表
     */
    @Operation(summary = "角色列表（不分页）")
    @GetMapping("/list")
    public R<List<RoleResp>> list(RoleQuery query) {
        return R.ok(roleService.list(query));
    }

    /**
     * 获取角色详情。
     *
     * @param id 角色 ID
     * @return 角色信息
     */
    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public R<RoleResp> get(@Parameter(description = "角色 ID") @PathVariable Long id) {
        return R.ok(roleService.get(id));
    }

    /**
     * 新增角色，code 必须唯一。
     *
     * @param req 角色请求体
     * @return 新建角色 ID
     */
    @Operation(summary = "新增角色")
    @PostMapping
    public R<Long> create(@RequestBody @Valid RoleReq req) {
        return R.ok(roleService.create(req));
    }

    /**
     * 修改角色；系统内置角色不允许修改 code。
     *
     * @param id  角色 ID
     * @param req 角色请求体
     */
    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "角色 ID") @PathVariable Long id,
                          @RequestBody @Valid RoleReq req) {
        roleService.update(id, req);
        return R.ok();
    }

    /**
     * 批量删除角色；系统内置或仍被引用的角色不允许删除。
     *
     * @param req 角色 ID 列表
     */
    @Operation(summary = "批量删除角色", description = "请求体形如 {\"ids\":[1,2,3]}")
    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        roleService.delete(req.getIds());
        return R.ok();
    }

    /**
     * 列出某角色下的所有用户 ID。
     *
     * @param id 角色 ID
     * @return 用户 ID 列表
     */
    @Operation(summary = "角色下用户 ID 列表")
    @GetMapping("/{id}/user/id")
    public R<List<Long>> listUserId(@Parameter(description = "角色 ID") @PathVariable Long id) {
        return R.ok(roleService.listUserIdsByRoleId(id));
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
