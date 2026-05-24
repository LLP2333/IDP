package com.qvqw.idp.monitor;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.monitor.model.query.OnlineUserQuery;
import com.qvqw.idp.monitor.model.resp.OnlineUserResp;
import jakarta.servlet.http.HttpServletRequest;

/** 在线用户服务。 */
public interface OnlineUserService {

    /**
     * 记录一次登录成功后的在线会话。
     *
     * @param token    JWT 原文
     * @param jti      JWT ID
     * @param userId   用户 ID
     * @param username 用户名
     * @param nickname 用户昵称
     * @param request  登录请求，可为空
     */
    void recordLogin(String token, String jti, Long userId, String username, String nickname,
                     HttpServletRequest request);

    /**
     * 刷新在线会话的最后活跃时间。
     *
     * @param token JWT 原文
     */
    void touch(String token);

    /**
     * 移除指定在线会话。
     *
     * @param token JWT 原文
     */
    void remove(String token);

    /**
     * 分页查询在线用户。
     *
     * @param query 查询条件
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 在线用户分页数据
     */
    PageResp<OnlineUserResp> page(OnlineUserQuery query, int page, int size);

    /**
     * 强制指定 token 下线。
     *
     * @param token JWT 原文
     */
    void kickout(String token);
}
