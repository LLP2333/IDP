package com.qvqw.idp.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增用户请求。
 */
@Schema(description = "新增用户请求")
public class UserCreateReq {

    @Schema(description = "用户名（字母开头，仅可包含字母/数字/下划线，长度 2-64）",
            example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,63}$", message = "用户名必须以字母开头，仅可包含字母、数字、下划线")
    private String username;

    @Schema(description = "初始密码（长度 6-32）", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
    private String password;

    @Schema(description = "昵称（可选，留空时取用户名）", example = "张三")
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不合法")
    private String email;

    @Schema(description = "手机号（中国大陆 11 位）", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不合法")
    private String phone;

    @Schema(description = "性别：0=未知, 1=男, 2=女", example = "1")
    private Integer gender;

    @Schema(description = "备注 / 描述", example = "技术部成员")
    @Size(max = 255, message = "描述长度不能超过 255")
    private String description;

    @Schema(description = "状态：1=启用, 0=禁用，默认 1", example = "1")
    private Integer status;

    @Schema(description = "关联角色 ID 列表", example = "[2]")
    private List<Long> roleIds;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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
