package com.qvqw.idp.option;

/**
 * 系统参数类别。
 *
 * <p>所有 {@link SystemOption} 都必须归属其中一个类别；前端会按类别查询并展示为不同的配置 Tab。</p>
 */
public enum OptionCategory {

    /** 网站基础配置：标题 / Logo / Favicon / 版权 / 备案号等。 */
    SITE,

    /** 密码安全策略：登录锁定、密码强度、历史重复校验、密码到期等。 */
    PASSWORD,

    /** 登录相关配置：验证码开关等。 */
    LOGIN
}
