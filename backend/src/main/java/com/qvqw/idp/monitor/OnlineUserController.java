package com.qvqw.idp.monitor;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.menu.annotation.HasPermission;
import com.qvqw.idp.monitor.model.query.OnlineUserQuery;
import com.qvqw.idp.monitor.model.resp.OnlineUserResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 在线用户 API。 */
@Tag(name = "在线用户", description = "在线用户监控与强退")
@RestController
@RequestMapping("/monitor/online")
@Validated
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    public OnlineUserController(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }

    /**
     * 分页查询在线用户。
     *
     * @param query 查询条件
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 在线用户分页数据
     */
    @Operation(summary = "在线用户分页")
    @HasPermission("monitor:online:list")
    @GetMapping
    public R<PageResp<OnlineUserResp>> page(OnlineUserQuery query,
                                            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(onlineUserService.page(query, page, size));
    }

    /**
     * 强退指定在线用户。
     *
     * @param token JWT 原文
     * @return 空响应
     */
    @Operation(summary = "强退在线用户")
    @HasPermission("monitor:online:kickout")
    @DeleteMapping("/{token}")
    public R<Void> kickout(@Parameter(description = "JWT 原文") @PathVariable String token) {
        onlineUserService.kickout(token);
        return R.ok();
    }
}
