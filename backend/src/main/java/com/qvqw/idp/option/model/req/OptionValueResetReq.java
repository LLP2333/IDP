package com.qvqw.idp.option.model.req;

import com.qvqw.idp.option.OptionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 参数重置请求。
 *
 * <p>{@link #category} 与 {@link #codes} 至少要传一个；都传时仅按 {@code codes} 过滤。</p>
 */
@Schema(description = "参数重置请求")
public class OptionValueResetReq {

    @Schema(description = "类别（按类别重置一整组）", example = "SITE")
    private OptionCategory category;

    @Schema(description = "键列表（按 code 精确重置）", example = "[\"SITE_TITLE\"]")
    private List<String> codes;

    public OptionCategory getCategory() {
        return category;
    }

    public void setCategory(OptionCategory category) {
        this.category = category;
    }

    public List<String> getCodes() {
        return codes;
    }

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }
}
