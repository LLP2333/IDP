package com.qvqw.idp.option;

import com.qvqw.idp.option.model.query.OptionQuery;
import com.qvqw.idp.option.model.req.OptionReq;
import com.qvqw.idp.option.model.req.OptionValueResetReq;
import com.qvqw.idp.option.model.resp.LoginConfigResp;
import com.qvqw.idp.option.model.resp.OptionResp;
import com.qvqw.idp.option.model.resp.SiteConfigResp;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 系统参数服务（对外暴露）。
 *
 * <p>除常规的 list / update / reset 外，还提供 “按类别取 Map” 与 “按 code 取值并解析” 两个
 * 跨模块查询能力，供 auth / user 等模块消费密码策略、登录验证码开关、SITE 配置等。</p>
 */
public interface OptionService {

    /**
     * 列表查询参数。
     *
     * @param query 类别 / code 过滤条件
     * @return 参数列表
     */
    List<OptionResp> list(OptionQuery query);

    /**
     * 取某类别下所有参数的 {@code code -> value} 映射。
     *
     * @param category 类别
     * @return 不可为 {@code null}；空时返回空 Map
     */
    Map<String, String> getByCategory(OptionCategory category);

    /**
     * 批量更新参数。
     *
     * <p>实现内部会做：</p>
     * <ol>
     *   <li>校验每一项 ID 存在；</li>
     *   <li>密码策略类的取值范围 + 跨字段约束校验；</li>
     *   <li>清理 Redis 缓存。</li>
     * </ol>
     *
     * @param reqs 待更新项列表
     */
    void update(List<OptionReq> reqs);

    /**
     * 重置为默认值。
     *
     * @param req 类别 / code 指定要重置的范围
     */
    void resetValue(OptionValueResetReq req);

    /**
     * 按 code 取参数值并解析为指定类型。
     *
     * @param code   参数键
     * @param mapper 解析函数（如 {@code Integer::parseInt}）
     * @param <T>    目标类型
     * @return 解析后的值；参数不存在或值为空时返回 {@code null}
     */
    <T> T getValue(String code, Function<String, T> mapper);

    /**
     * {@link #getValue(String, Function)} 的整数特化版本，常用于密码策略阈值。
     *
     * @param code         参数键
     * @param defaultValue 解析失败 / 不存在时的兜底值
     * @return 整数值
     */
    int getIntOrDefault(String code, int defaultValue);

    /**
     * 校验上传图片并返回标准化 dataUrl。
     *
     * @param dataUrl base64 字符串（必须以 {@code data:image/} 开头）
     * @return 校验后原样返回
     */
    String validateImage(String dataUrl);

    /**
     * 取公开的 SITE 配置（白名单字段）。
     *
     * @return 网站配置；字段可能为 {@code null}
     */
    SiteConfigResp getPublicSite();

    /**
     * 取公开的 LOGIN 配置（白名单字段）。
     *
     * @return 登录配置
     */
    LoginConfigResp getPublicLogin();
}
