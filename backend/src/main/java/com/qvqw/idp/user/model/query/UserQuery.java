package com.qvqw.idp.user.model.query;

/**
 * 用户分页查询条件。
 */
public class UserQuery {

    private String username;
    private Integer status;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
