package com.qvqw.idp.message;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.message.model.req.MessageCreateReq;
import com.qvqw.idp.message.model.query.MessageQuery;
import com.qvqw.idp.message.model.resp.MessageResp;

import java.util.List;

/**
 * 站内消息服务（对外暴露）。
 *
 * <p>面向 notice 等业务模块的统一 “消息发送” 入口：通知发布时调用 {@link #publish}
 * 给指定用户群体下发系统消息；普通用户通过 {@link #page} 等查询自己的收件箱。</p>
 */
public interface MessageService {

    /**
     * 发布消息：写入消息主表 + 为每个 userId 创建未读 {@link MessageLog}。
     *
     * <p>{@code userIds} 为 {@code null} 或空时，自动 fan-out 给全体启用用户。</p>
     *
     * @param req     消息正文（标题 / 内容 / 跳转路径）
     * @param userIds 收件人 ID 列表，为空时表示所有启用用户
     * @return 新消息 ID
     */
    Long publish(MessageCreateReq req, List<Long> userIds);

    /**
     * 分页查询当前登录用户的收件箱。
     *
     * @param query 查询条件（title 模糊、isRead）
     * @param page  页码（从 1 开始）
     * @param size  每页大小
     * @return 分页消息
     */
    PageResp<MessageResp> page(MessageQuery query, int page, int size);

    /**
     * 当前登录用户的未读消息数量。
     *
     * @return 未读条数
     */
    long unreadCount();

    /**
     * 当前登录用户将某条消息标记为已读。
     *
     * <p>消息不在收件箱内 / 不存在时静默忽略（已读幂等）。</p>
     *
     * @param messageId 消息 ID
     */
    void read(Long messageId);

    /**
     * 当前登录用户一键标记全部为已读。
     */
    void readAll();
}
