package com.qvqw.idp.user;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.user.model.query.UserQuery;
import com.qvqw.idp.user.model.req.UserBasicInfoUpdateReq;
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

    /**
     * 当前登录用户自助修改基本信息（昵称 / 邮箱 / 手机 / 性别）。
     *
     * <p>不允许修改 {@code status} / {@code description} / 角色，避免越权。
     * 字段为 {@code null} 时跳过该字段，邮箱 / 手机为 {@code ""} 表示主动清除。</p>
     *
     * @param userId 当前用户 ID
     * @param req    待修改字段
     */
    void updateCurrentUserBasicInfo(Long userId, UserBasicInfoUpdateReq req);

    /** 由 auth 模块用于登录时校验账号。返回值包含原始密码哈希。 */
    Optional<UserCredential> findCredential(String username);

    /** 由 auth 模块加载用户信息以装入 UserContext。 */
    Optional<UserDetailResp> findById(Long id);

    /**
     * 列出全部启用（{@code status=1}）用户的 ID。
     *
     * <p>供 message / notice 等需要 “全员通知” 的模块按 ID 集合 fan-out。</p>
     *
     * @return 启用用户 ID 列表
     */
    List<Long> listEnabledUserIds();

    /**
     * 按 ID 列表批量取用户的显示名（nickname 优先，否则 username）。
     *
     * <p>供 notice 模块在列表 / 详情中渲染 “发布人” 一列。</p>
     *
     * @param ids 用户 ID 列表
     * @return userId → displayName 映射；不存在的 ID 不会出现在结果中
     */
    java.util.Map<Long, String> mapDisplayNames(List<Long> ids);

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
