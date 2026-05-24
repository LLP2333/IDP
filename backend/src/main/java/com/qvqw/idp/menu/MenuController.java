package com.qvqw.idp.menu;

import com.qvqw.idp.common.api.R;
import com.qvqw.idp.menu.annotation.HasPermission;
import com.qvqw.idp.menu.model.query.MenuQuery;
import com.qvqw.idp.menu.model.req.MenuReq;
import com.qvqw.idp.menu.model.resp.MenuResp;
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
 * 菜单管理 API。
 *
 * <p>提供菜单 CRUD 与树形结构查询；分配菜单到角色的操作在 {@code /system/role/{id}/menu}。</p>
 */
@Tag(name = "菜单管理", description = "菜单 CRUD + 树形结构")
@RestController
@RequestMapping("/system/menu")
@Validated
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "菜单列表（平铺）")
    @HasPermission("system:menu:list")
    @GetMapping
    public R<List<MenuResp>> list(MenuQuery query) {
        return R.ok(menuService.list(query));
    }

    @Operation(summary = "菜单树", description = "按父子层级返回；前端菜单管理表格 / 角色分配菜单弹窗直接消费此结构")
    @HasPermission("system:menu:list")
    @GetMapping("/tree")
    public R<List<MenuResp>> tree(MenuQuery query) {
        return R.ok(menuService.tree(query));
    }

    @Operation(summary = "菜单详情")
    @HasPermission("system:menu:list")
    @GetMapping("/{id}")
    public R<MenuResp> get(@Parameter(description = "菜单 ID") @PathVariable Long id) {
        return R.ok(menuService.get(id));
    }

    @Operation(summary = "新增菜单")
    @HasPermission("system:menu:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid MenuReq req) {
        return R.ok(menuService.create(req));
    }

    @Operation(summary = "修改菜单")
    @HasPermission("system:menu:update")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "菜单 ID") @PathVariable Long id,
                          @RequestBody @Valid MenuReq req) {
        menuService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "批量删除菜单", description = "请求体形如 {\"ids\":[1,2,3]}")
    @HasPermission("system:menu:delete")
    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        menuService.delete(req.getIds());
        return R.ok();
    }

    /** 批量删除通用请求体。 */
    @Schema(description = "按 ID 列表批量删除请求体")
    public static class DeleteIdsReq {

        @Schema(description = "菜单 ID 列表", example = "[1,2,3]", requiredMode = Schema.RequiredMode.REQUIRED)
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
