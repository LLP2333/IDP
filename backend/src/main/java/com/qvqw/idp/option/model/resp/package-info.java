/**
 * 系统参数模块对外暴露的响应 DTO（命名接口）。
 *
 * <p>auth / user 等模块可能会读取 {@link com.qvqw.idp.option.model.resp.OptionResp}
 * 用于把配置回写到登录响应、用户信息等地方。</p>
 */
@org.springframework.modulith.NamedInterface("model")
package com.qvqw.idp.option.model.resp;
