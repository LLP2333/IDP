package com.qvqw.idp.monitor.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 系统日志响应。 */
@Schema(description = "系统日志响应")
public class LogResp {

    @Schema(description = "日志 ID")
    private String id;
    @Schema(description = "日志描述")
    private String description;
    @Schema(description = "所属模块")
    private String module;
    @Schema(description = "耗时，单位毫秒")
    private Long timeTaken;
    @Schema(description = "IP")
    private String ip;
    @Schema(description = "地点")
    private String address;
    @Schema(description = "浏览器")
    private String browser;
    @Schema(description = "终端系统")
    private String os;
    @Schema(description = "状态：1=成功，2=失败")
    private Integer status;
    @Schema(description = "错误信息")
    private String errorMsg;
    @Schema(description = "操作人展示名")
    private String createUserString;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getCreateUserString() {
        return createUserString;
    }

    public void setCreateUserString(String createUserString) {
        this.createUserString = createUserString;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
