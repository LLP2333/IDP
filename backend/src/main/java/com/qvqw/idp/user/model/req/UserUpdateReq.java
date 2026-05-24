package com.qvqw.idp.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 修改用户请求（用户名不可修改）。
 */
@Schema(description = "修改用户请求")
public class UserUpdateReq {

    @Schema(description = "昵称")
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不合法")
    private String email;

    @Schema(description = "手机号")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不合法")
    private String phone;

    @Schema(description = "性别：0=未知, 1=男, 2=女")
    private Integer gender;

    @Schema(description = "备注 / 描述")
    @Size(max = 255, message = "描述长度不能超过 255")
    private String description;

    @Schema(description = "状态：1=启用, 0=禁用")
    private Integer status;

    @Schema(description = "角色 ID 列表（全量覆盖）")
    private List<Long> roleIds;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
