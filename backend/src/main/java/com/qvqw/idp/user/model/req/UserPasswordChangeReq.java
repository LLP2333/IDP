package com.qvqw.idp.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 当前用户自助修改密码请求。
 *
 * <p>由 {@link com.qvqw.idp.user.UserController#changePassword} 接收；逻辑：</p>
 * <ol>
 *   <li>{@link #oldPassword} 必须匹配当前用户密码；</li>
 *   <li>{@link #newPassword} 走 {@code PasswordValidator}（强度 + 历史校验）；</li>
 *   <li>更新 {@code password / pwdResetAt}，写入历史密码并保留最近 N 条。</li>
 * </ol>
 */
@Schema(description = "修改当前用户密码请求")
public class UserPasswordChangeReq {

    @Schema(description = "原密码", example = "OldPass#123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码（需满足密码策略）", example = "NewPass#234",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(max = 100, message = "密码长度不能超过 100")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
