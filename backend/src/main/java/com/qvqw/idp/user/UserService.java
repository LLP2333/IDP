package com.qvqw.idp.user;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.user.model.query.UserQuery;
import com.qvqw.idp.user.model.req.UserCreateReq;
import com.qvqw.idp.user.model.req.UserPasswordChangeReq;
import com.qvqw.idp.user.model.req.UserPasswordResetReq;
import com.qvqw.idp.user.model.req.UserRoleUpdateReq;
import com.qvqw.idp.user.model.req.UserUpdateReq;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import com.qvqw.idp.user.model.resp.UserResp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务。
 */
public interface UserService {

    PageResp<UserResp> page(UserQuery query, int page, int size);

    UserDetailResp get(Long id);

    Long create(UserCreateReq req);

    void update(Long id, UserUpdateReq req);

    void delete(List<Long> ids);

    void resetPassword(Long id, UserPasswordResetReq req);

    void updateRole(Long id, UserRoleUpdateReq req);

    /** 当前登录用户自助修改密码（旧密码校验 + 新密码策略校验 + 历史密码校验）。 */
    void changeCurrentPassword(Long userId, UserPasswordChangeReq req);

    /** 由 auth 模块用于登录时校验账号。返回值包含原始密码哈希。 */
    Optional<UserCredential> findCredential(String username);

    /** 由 auth 模块加载用户信息以装入 UserContext。 */
    Optional<UserDetailResp> findById(Long id);

    /**
     * 累加某用户的登录失败计数，到达阈值时同步设置锁定到期时间。
     *
     * @param userId    用户 ID
     * @param maxCount  失败阈值（{@code 0} 表示禁用锁定，直接跳过）
     * @param lockUntil 达到阈值时设置的锁定到期时间（{@code null} 表示不锁定）
     * @return 累加后的失败次数
     */
    int increasePwdErrorCount(Long userId, int maxCount, LocalDateTime lockUntil);

    /** 清零某用户的登录失败计数 + 锁定时间（登录成功 / 管理员重置时调用）。 */
    void resetPwdErrorAndUnlock(Long userId);

    /**
     * 登录用 credential，比 {@link UserDetailResp} 更紧凑，避免暴露过多字段。
     */
    record UserCredential(Long id, String username, String passwordHash, Integer status,
                          LocalDateTime pwdResetAt, Integer pwdErrorCount,
                          LocalDateTime pwdLockedUntil) {
    }
}
