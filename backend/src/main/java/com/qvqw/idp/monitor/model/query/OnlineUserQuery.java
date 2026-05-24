package com.qvqw.idp.monitor.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/** 在线用户查询条件。 */
@Schema(description = "在线用户查询条件")
public class OnlineUserQuery {

    @Schema(description = "用户名 / 昵称关键字")
    private String nickname;

    @Schema(description = "登录时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private List<LocalDateTime> loginTime;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public List<LocalDateTime> getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(List<LocalDateTime> loginTime) {
        this.loginTime = loginTime;
    }
}
