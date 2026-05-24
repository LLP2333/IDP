package com.qvqw.idp.option.model.query;

import com.qvqw.idp.option.OptionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 参数列表查询条件。
 *
 * <p>{@link #category} 与 {@link #codes} 均可选；都不传时返回全部参数。</p>
 */
@Schema(description = "参数查询条件")
public class OptionQuery {

    @Schema(description = "类别（SITE / PASSWORD / LOGIN）", example = "SITE")
    private OptionCategory category;

    @Schema(description = "键列表", example = "[\"SITE_TITLE\", \"SITE_COPYRIGHT\"]")
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
