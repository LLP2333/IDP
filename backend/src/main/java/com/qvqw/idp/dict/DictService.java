package com.qvqw.idp.dict;

import com.qvqw.idp.dict.model.req.DictItemReq;
import com.qvqw.idp.dict.model.req.DictReq;
import com.qvqw.idp.dict.model.resp.DictItemResp;
import com.qvqw.idp.dict.model.resp.DictResp;

import java.util.List;

/**
 * 字典服务（对外暴露）。
 *
 * <p>字典本身较简单，只暴露 “全量列表 + CRUD”；明细按字典维度增删改查。
 * 业务模块通过 {@link #listItemsByCode(String)} 取某分类的可选项，例如
 * notice 模块拿 {@code notice_type} 字典渲染分类选择器。</p>
 */
public interface DictService {

    /**
     * 列出全部字典。
     *
     * @return 按 ID 升序返回
     */
    List<DictResp> listDict();

    /**
     * 查询字典详情。
     *
     * @param id 字典 ID
     * @return 字典 DTO
     */
    DictResp getDict(Long id);

    /**
     * 新增字典。
     *
     * @param req 字典请求体
     * @return 新建字典 ID
     */
    Long createDict(DictReq req);

    /**
     * 修改字典。系统内置字典禁止改 code。
     *
     * @param id  字典 ID
     * @param req 字典请求体
     */
    void updateDict(Long id, DictReq req);

    /**
     * 批量删除字典；系统内置字典不允许删除。
     * 同时联动删除该字典下全部明细项。
     *
     * @param ids 字典 ID 列表
     */
    void deleteDict(List<Long> ids);

    /**
     * 列出某字典下的全部明细（按 sort 升序）。
     *
     * @param dictId 字典 ID
     * @return 明细列表
     */
    List<DictItemResp> listItems(Long dictId);

    /**
     * 按字典 {@code code} 列出明细（仅启用项），供业务模块作为下拉数据使用。
     *
     * @param code 字典编码
     * @return 启用的明细列表
     */
    List<DictItemResp> listItemsByCode(String code);

    /**
     * 新增字典明细。
     *
     * @param dictId 字典 ID
     * @param req    明细请求体
     * @return 新建明细 ID
     */
    Long createItem(Long dictId, DictItemReq req);

    /**
     * 修改字典明细。系统内置项不允许改 value。
     *
     * @param itemId 明细 ID
     * @param req    明细请求体
     */
    void updateItem(Long itemId, DictItemReq req);

    /**
     * 批量删除字典明细；系统内置项不允许删除。
     *
     * @param itemIds 明细 ID 列表
     */
    void deleteItems(List<Long> itemIds);
}
