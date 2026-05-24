package com.qvqw.idp.user;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.user.model.query.UserQuery;
import com.qvqw.idp.user.model.req.UserCreateReq;
import com.qvqw.idp.user.model.req.UserPasswordResetReq;
import com.qvqw.idp.user.model.req.UserRoleUpdateReq;
import com.qvqw.idp.user.model.req.UserUpdateReq;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import com.qvqw.idp.user.model.resp.UserResp;

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

    /** 由 auth 模块用于登录时校验账号。返回值包含原始密码哈希。 */
    Optional<UserCredential> findCredential(String username);

    /** 由 auth 模块加载用户信息以装入 UserContext。 */
    Optional<UserDetailResp> findById(Long id);

    /** 用于密码校验后更新最后修改密码时间（非必须）。 */
    record UserCredential(Long id, String username, String passwordHash, Integer status) {
    }
}
