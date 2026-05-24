package com.qvqw.idp.monitor.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/** 系统日志详情响应。 */
@Schema(description = "系统日志详情响应")
public class LogDetailResp extends LogResp {

    @Schema(description = "链路追踪 ID")
    private String traceId;
    @Schema(description = "请求地址")
    private String requestUrl;
    @Schema(description = "请求方法")
    private String requestMethod;
    @Schema(description = "请求头")
    private String requestHeaders;
    @Schema(description = "请求体")
    private String requestBody;
    @Schema(description = "HTTP 状态码")
    private Integer statusCode;
    @Schema(description = "响应头")
    private String responseHeaders;
    @Schema(description = "响应体")
    private String responseBody;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
