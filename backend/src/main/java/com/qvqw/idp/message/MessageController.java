package com.qvqw.idp.message;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.message.model.query.MessageQuery;
import com.qvqw.idp.message.model.resp.MessageResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 个人消息中心 API。
 *
 * <p>所有接口都以 “当前登录用户” 为隐式过滤条件；任何登录用户都可访问，
 * 不需要 {@code system:message:*} 权限。管理员发消息走 notice 模块或其他业务模块的服务层。</p>
 */
@Tag(name = "消息中心", description = "当前用户的收件箱、未读数、标已读")
@RestController
@RequestMapping("/system/message")
@Validated
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 当前用户消息分页。
     *
     * @param query 查询条件
     * @param page  页码
     * @param size  每页大小
     * @return 分页消息
     */
    @Operation(summary = "消息分页", description = "当前登录用户的收件箱（支持标题模糊 + 已读筛选）")
    @GetMapping
    public R<PageResp<MessageResp>> page(MessageQuery query,
                                         @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                         @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(messageService.page(query, page, size));
    }

    /**
     * 未读消息条数（顶栏 bell 角标使用）。
     *
     * @return 未读数量
     */
    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        return R.ok(Map.of("count", messageService.unreadCount()));
    }

    /**
     * 标记某条消息为已读。
     *
     * @param id 消息 ID
     */
    @Operation(summary = "标记已读")
    @PostMapping("/{id}/read")
    public R<Void> read(@Parameter(description = "消息 ID") @PathVariable Long id) {
        messageService.read(id);
        return R.ok();
    }

    /**
     * 一键标记全部为已读。
     */
    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public R<Void> readAll() {
        messageService.readAll();
        return R.ok();
    }
}
