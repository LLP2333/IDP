package com.qvqw.idp.dict;

import com.qvqw.idp.common.api.R;
import com.qvqw.idp.dict.model.req.DictItemReq;
import com.qvqw.idp.dict.model.req.DictReq;
import com.qvqw.idp.dict.model.resp.DictItemResp;
import com.qvqw.idp.dict.model.resp.DictResp;
import com.qvqw.idp.menu.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典管理 API。
 *
 * <p>字典本身偏静态、量级小，故不做分页；明细按字典维度增删改查。
 * “按 code 查明细” 接口对所有登录用户开放，方便前端各业务页面拉取下拉数据，
 * 不需要专门的 {@code system:dict:list} 权限。</p>
 */
@Tag(name = "字典管理", description = "字典与字典明细的 CRUD")
@RestController
@RequestMapping("/system/dict")
@Validated
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /**
     * 列出全部字典（不分页）。
     *
     * @return 字典列表
     */
    @Operation(summary = "字典列表（不分页）")
    @HasPermission("system:dict:list")
    @GetMapping("/list")
    public R<List<DictResp>> list() {
        return R.ok(dictService.listDict());
    }

    /**
     * 字典详情。
     *
     * @param id 字典 ID
     * @return 字典 DTO
     */
    @Operation(summary = "字典详情")
    @HasPermission("system:dict:list")
    @GetMapping("/{id}")
    public R<DictResp> get(@Parameter(description = "字典 ID") @PathVariable Long id) {
        return R.ok(dictService.getDict(id));
    }

    /**
     * 新增字典。
     *
     * @param req 字典请求体
     * @return 新建字典 ID
     */
    @Operation(summary = "新增字典")
    @HasPermission("system:dict:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid DictReq req) {
        return R.ok(dictService.createDict(req));
    }

    /**
     * 修改字典。
     *
     * @param id  字典 ID
     * @param req 字典请求体
     */
    @Operation(summary = "修改字典")
    @HasPermission("system:dict:update")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "字典 ID") @PathVariable Long id,
                          @RequestBody @Valid DictReq req) {
        dictService.updateDict(id, req);
        return R.ok();
    }

    /**
     * 批量删除字典（同时删除所有明细）。
     *
     * @param req 字典 ID 列表
     */
    @Operation(summary = "批量删除字典", description = "请求体形如 {\"ids\":[1,2,3]}")
    @HasPermission("system:dict:delete")
    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        dictService.deleteDict(req.getIds());
        return R.ok();
    }

    /**
     * 按字典编码查询明细（仅启用项），登录用户即可访问。
     *
     * <p>各业务模块（如 notice）通过该接口获取分类选择器的可选值。</p>
     *
     * @param code 字典编码
     * @return 明细列表
     */
    @Operation(summary = "按字典编码查询明细", description = "登录用户即可调用，业务下拉数据源")
    @GetMapping("/{code}/item")
    public R<List<DictItemResp>> listItemByCode(@Parameter(description = "字典编码") @PathVariable String code) {
        return R.ok(dictService.listItemsByCode(code));
    }

    /**
     * 列出某字典下的全部明细（含禁用项，admin 配置用）。
     *
     * @param id 字典 ID
     * @return 明细列表
     */
    @Operation(summary = "字典下明细列表（含禁用）")
    @HasPermission("system:dict:list")
    @GetMapping("/{id}/item/all")
    public R<List<DictItemResp>> listItem(@Parameter(description = "字典 ID") @PathVariable Long id) {
        return R.ok(dictService.listItems(id));
    }

    /**
     * 新增字典明细。
     *
     * @param id  字典 ID
     * @param req 明细请求体
     * @return 新建明细 ID
     */
    @Operation(summary = "新增字典明细")
    @HasPermission("system:dict:add")
    @PostMapping("/{id}/item")
    public R<Long> createItem(@Parameter(description = "字典 ID") @PathVariable Long id,
                              @RequestBody @Valid DictItemReq req) {
        return R.ok(dictService.createItem(id, req));
    }

    /**
     * 修改字典明细。
     *
     * @param itemId 明细 ID
     * @param req    明细请求体
     */
    @Operation(summary = "修改字典明细")
    @HasPermission("system:dict:update")
    @PutMapping("/item/{itemId}")
    public R<Void> updateItem(@Parameter(description = "明细 ID") @PathVariable Long itemId,
                              @RequestBody @Valid DictItemReq req) {
        dictService.updateItem(itemId, req);
        return R.ok();
    }

    /**
     * 批量删除字典明细。
     *
     * @param req 明细 ID 列表
     */
    @Operation(summary = "批量删除字典明细", description = "请求体形如 {\"ids\":[1,2,3]}")
    @HasPermission("system:dict:delete")
    @DeleteMapping("/item")
    public R<Void> deleteItem(@RequestBody @Valid DeleteIdsReq req) {
        dictService.deleteItems(req.getIds());
        return R.ok();
    }

    /**
     * 通用的 “按 ID 列表批量删除” 请求体。
     */
    @Schema(description = "按 ID 列表批量删除请求体")
    public static class DeleteIdsReq {

        @Schema(description = "待删除的 ID 列表", example = "[1,2,3]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "ID 列表不能为空")
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }
}
