package com.qvqw.idp.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 当前登录用户自助修改基本信息请求。
 *
 * <p>仅允许用户修改自己的非敏感字段：昵称、邮箱、手机、性别。
 * 与 {@link UserUpdateReq} 区别：</p>
 * <ul>
 *   <li>不允许修改 {@code status} / {@code description} / {@code roleIds}（防止越权）；</li>
 *   <li>由 {@link com.qvqw.idp.user.UserController#updateProfile} 接收，
 *       任何登录用户都可调用，无需 {@code system:user:*} 权限。</li>
 * </ul>
 */
@Schema(description = "修改当前用户基本信息请求")
public class UserBasicInfoUpdateReq {

    @Schema(description = "昵称")
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    @Schema(description = "邮箱（留空表示清除）")
    @Email(message = "邮箱格式不合法")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    @Schema(description = "手机号（留空表示清除）")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不合法")
    private String phone;

    @Schema(description = "性别：0=未知, 1=男, 2=女", example = "1")
    private Integer gender;

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
}
