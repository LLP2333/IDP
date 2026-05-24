package com.qvqw.idp.user;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.user.model.query.UserQuery;
import com.qvqw.idp.user.model.req.UserCreateReq;
import com.qvqw.idp.user.model.req.UserPasswordResetReq;
import com.qvqw.idp.user.model.req.UserRoleUpdateReq;
import com.qvqw.idp.user.model.req.UserUpdateReq;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import com.qvqw.idp.user.model.resp.UserResp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理 API。
 */
@RestController
@RequestMapping("/system/user")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public R<PageResp<UserResp>> page(UserQuery query,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return R.ok(userService.page(query, page, size));
    }

    @GetMapping("/{id}")
    public R<UserDetailResp> get(@PathVariable Long id) {
        return R.ok(userService.get(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody @Valid UserCreateReq req) {
        return R.ok(userService.create(req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid UserUpdateReq req) {
        userService.update(id, req);
        return R.ok();
    }

    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        userService.delete(req.getIds());
        return R.ok();
    }

    @PatchMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody @Valid UserPasswordResetReq req) {
        userService.resetPassword(id, req);
        return R.ok();
    }

    @PatchMapping("/{id}/role")
    public R<Void> updateRole(@PathVariable Long id, @RequestBody @Valid UserRoleUpdateReq req) {
        userService.updateRole(id, req);
        return R.ok();
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
