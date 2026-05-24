package com.qvqw.idp.user;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.permission.annotation.HasPermission;
import com.qvqw.idp.user.model.query.UserQuery;
import com.qvqw.idp.user.model.req.UserCreateReq;
import com.qvqw.idp.user.model.req.UserPasswordChangeReq;
import com.qvqw.idp.user.model.req.UserPasswordResetReq;
import com.qvqw.idp.user.model.req.UserRoleUpdateReq;
import com.qvqw.idp.user.model.req.UserUpdateReq;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import com.qvqw.idp.user.model.resp.UserResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 用户管理 API：分页、详情、新增、修改、删除、重置密码、分配角色。
 */
@Tag(name = "用户管理", description = "用户 CRUD、重置密码、分配角色")
@RestController
@RequestMapping("/system/user")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户分页查询。
     *
     * @param query 模糊匹配条件（用户名 / 状态）
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 用户列表分页结果
     */
    @Operation(summary = "用户分页", description = "按用户名（模糊）和状态过滤。")
    @HasPermission("system:user:list")
    @GetMapping
    public R<PageResp<UserResp>> page(UserQuery query,
                                      @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(userService.page(query, page, size));
    }

    /**
     * 获取用户详情，包含其所有角色。
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @Operation(summary = "用户详情")
    @HasPermission("system:user:list")
    @GetMapping("/{id}")
    public R<UserDetailResp> get(@Parameter(description = "用户 ID") @PathVariable Long id) {
        return R.ok(userService.get(id));
    }

    /**
     * 新增用户。
     *
     * @param req 用户创建请求
     * @return 新建用户的 ID
     */
    @Operation(summary = "新增用户")
    @HasPermission("system:user:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid UserCreateReq req) {
        return R.ok(userService.create(req));
    }

    /**
     * 修改用户基本信息（用户名不可改）。
     *
     * @param id  用户 ID
     * @param req 待更新字段
     */
    @Operation(summary = "修改用户")
    @HasPermission("system:user:update")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "用户 ID") @PathVariable Long id,
                          @RequestBody @Valid UserUpdateReq req) {
        userService.update(id, req);
        return R.ok();
    }

    /**
     * 批量删除用户；系统内置用户不允许删除。
     *
     * @param req 待删除的用户 ID 列表
     */
    @Operation(summary = "批量删除用户", description = "请求体形如 {\"ids\":[1,2,3]}")
    @HasPermission("system:user:delete")
    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        userService.delete(req.getIds());
        return R.ok();
    }

    /**
     * 管理员重置指定用户的密码。
     *
     * @param id  用户 ID
     * @param req 新密码请求体
     */
    @Operation(summary = "重置用户密码")
    @HasPermission("system:user:resetPassword")
    @PatchMapping("/{id}/password")
    public R<Void> resetPassword(@Parameter(description = "用户 ID") @PathVariable Long id,
                                 @RequestBody @Valid UserPasswordResetReq req) {
        userService.resetPassword(id, req);
        return R.ok();
    }

    /**
     * 重新分配用户的角色集合（全量覆盖）。
     *
     * @param id  用户 ID
     * @param req 角色 ID 列表
     */
    @Operation(summary = "分配角色")
    @HasPermission("system:user:updateRole")
    @PatchMapping("/{id}/role")
    public R<Void> updateRole(@Parameter(description = "用户 ID") @PathVariable Long id,
                              @RequestBody @Valid UserRoleUpdateReq req) {
        userService.updateRole(id, req);
        return R.ok();
    }

    /**
     * 当前用户自助修改密码（不需要 system:user:* 权限，任何登录用户都可调用）。
     *
     * @param req 旧 / 新密码
     */
    @Operation(summary = "修改当前用户密码", description = "用于强制改密 Modal 或个人设置页")
    @PostMapping("/password")
    public R<Void> changePassword(@RequestBody @Valid UserPasswordChangeReq req) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        userService.changeCurrentPassword(userId, req);
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
