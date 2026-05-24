package com.qvqw.idp.auth;

import com.qvqw.idp.auth.model.req.LoginReq;
import com.qvqw.idp.auth.model.resp.LoginResp;
import com.qvqw.idp.auth.model.resp.UserInfoResp;
import com.qvqw.idp.menu.model.resp.MenuResp;

import java.util.List;

/**
 * 认证服务（对外暴露）。
 */
public interface AuthService {

    LoginResp login(LoginReq req);

    void logout(String jti);

    UserInfoResp getCurrentUserInfo();

    /**
     * 获取当前登录用户可见的菜单路由（{@code type=1/2} 树，按 sort 升序）。
     *
     * <p>admin 角色返回全部启用菜单；普通用户聚合其角色绑定的菜单去重后返回。</p>
     *
     * @return 菜单树
     */
    List<MenuResp> getCurrentUserRoute();
}
