package com.qvqw.idp.option.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公开网站配置（供登录页等未登录场景调用）。
 *
 * <p>仅暴露 SITE 类别中的安全字段，密码策略与登录配置之外的内容不会返回。</p>
 */
@Schema(description = "公开网站配置")
public class SiteConfigResp {

    @Schema(description = "系统标题", example = "IDP 后台管理系统")
    private String title;

    @Schema(description = "系统描述", example = "通用企业级后台管理系统")
    private String description;

    @Schema(description = "Logo URL 或 base64", example = "/logo.svg")
    private String logo;

    @Schema(description = "Favicon URL 或 base64", example = "/favicon.ico")
    private String favicon;

    @Schema(description = "版权声明", example = "Copyright © IDP")
    private String copyright;

    @Schema(description = "备案号")
    private String beian;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getFavicon() {
        return favicon;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getBeian() {
        return beian;
    }

    public void setBeian(String beian) {
        this.beian = beian;
    }
}
