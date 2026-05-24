package com.qvqw.idp.option;

import com.qvqw.idp.common.api.R;
import com.qvqw.idp.option.model.query.OptionQuery;
import com.qvqw.idp.option.model.req.OptionImageUploadReq;
import com.qvqw.idp.option.model.req.OptionReq;
import com.qvqw.idp.option.model.req.OptionValueResetReq;
import com.qvqw.idp.option.model.resp.LoginConfigResp;
import com.qvqw.idp.option.model.resp.OptionImageUploadResp;
import com.qvqw.idp.option.model.resp.OptionResp;
import com.qvqw.idp.option.model.resp.SiteConfigResp;
import com.qvqw.idp.permission.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统参数管理 API。
 *
 * <p>包含 3 类接口：</p>
 * <ul>
 *   <li>需登录 + 权限：list / update / reset / 图片上传，供后台 “系统配置” 页使用；</li>
 *   <li>公开接口（白名单）：{@code /site} / {@code /login}，供登录页 / 公开页拉取标题、Logo、
 *       验证码开关等渲染所需配置。</li>
 * </ul>
 */
@Tag(name = "系统参数", description = "网站配置 / 安全配置 / 登录配置")
@RestController
@RequestMapping("/system/option")
public class OptionController {

    private final OptionService optionService;

    public OptionController(OptionService optionService) {
        this.optionService = optionService;
    }

    /**
     * 列表查询参数。
     *
     * @param query 类别 / code 过滤
     * @return 参数列表
     */
    @Operation(summary = "参数列表", description = "按类别 / code 过滤；常用于配置页加载某一类")
    @HasPermission({"system:siteConfig:get", "system:securityConfig:get", "system:loginConfig:get"})
    @GetMapping
    public R<List<OptionResp>> list(OptionQuery query) {
        return R.ok(optionService.list(query));
    }

    /**
     * 批量更新参数。
     *
     * @param reqs 待更新项列表
     */
    @Operation(summary = "批量更新参数")
    @HasPermission({"system:siteConfig:update", "system:securityConfig:update", "system:loginConfig:update"})
    @PutMapping
    public R<Void> update(@RequestBody @Valid @NotEmpty(message = "参数列表不能为空") List<OptionReq> reqs) {
        optionService.update(reqs);
        return R.ok();
    }

    /**
     * 重置参数为默认值。
     */
    @Operation(summary = "重置参数")
    @HasPermission({"system:siteConfig:update", "system:securityConfig:update", "system:loginConfig:update"})
    @PatchMapping("/value")
    public R<Void> resetValue(@RequestBody @Valid OptionValueResetReq req) {
        optionService.resetValue(req);
        return R.ok();
    }

    /**
     * 上传图片（base64，供 Logo / Favicon 使用）。
     *
     * @param req 图片 base64
     * @return 标准化后的 dataUrl
     */
    @Operation(summary = "上传图片", description = "校验 base64 大小与前缀，回显给前端写回表单")
    @HasPermission({"system:siteConfig:update"})
    @PostMapping("/image")
    public R<OptionImageUploadResp> uploadImage(@RequestBody @Valid OptionImageUploadReq req) {
        return R.ok(new OptionImageUploadResp(optionService.validateImage(req.getDataUrl())));
    }

    /**
     * 公开 SITE 配置（无需登录）。
     *
     * @return 网站配置
     */
    @Operation(summary = "公开网站配置", description = "供登录页拉取系统标题 / Logo / Favicon 等")
    @SecurityRequirements
    @GetMapping("/site")
    public R<SiteConfigResp> publicSite() {
        return R.ok(optionService.getPublicSite());
    }

    /**
     * 公开 LOGIN 配置（无需登录）。
     *
     * @return 登录配置
     */
    @Operation(summary = "公开登录配置", description = "供登录页判断是否需要展示验证码")
    @SecurityRequirements
    @GetMapping("/login")
    public R<LoginConfigResp> publicLogin() {
        return R.ok(optionService.getPublicLogin());
    }
}
