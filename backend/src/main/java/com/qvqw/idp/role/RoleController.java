package com.qvqw.idp.role;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.role.model.query.RoleQuery;
import com.qvqw.idp.role.model.req.RoleReq;
import com.qvqw.idp.role.model.resp.RoleResp;
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
import java.util.Map;

/**
 * 角色管理 API。
 */
@RestController
@RequestMapping("/system/role")
@Validated
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public R<PageResp<RoleResp>> page(RoleQuery query,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return R.ok(roleService.page(query, page, size));
    }

    @GetMapping("/list")
    public R<List<RoleResp>> list(RoleQuery query) {
        return R.ok(roleService.list(query));
    }

    @GetMapping("/{id}")
    public R<RoleResp> get(@PathVariable Long id) {
        return R.ok(roleService.get(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody @Valid RoleReq req) {
        return R.ok(roleService.create(req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid RoleReq req) {
        roleService.update(id, req);
        return R.ok();
    }

    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        roleService.delete(req.getIds());
        return R.ok();
    }

    @GetMapping("/{id}/user/id")
    public R<List<Long>> listUserId(@PathVariable Long id) {
        return R.ok(roleService.listUserIdsByRoleId(id));
    }

    public static class DeleteIdsReq {

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
