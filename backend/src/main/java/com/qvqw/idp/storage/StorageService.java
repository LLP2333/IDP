package com.qvqw.idp.storage;

import com.qvqw.idp.storage.model.query.StorageQuery;
import com.qvqw.idp.storage.model.req.StorageCreateReq;
import com.qvqw.idp.storage.model.req.StorageStatusUpdateReq;
import com.qvqw.idp.storage.model.req.StorageUpdateReq;
import com.qvqw.idp.storage.model.resp.StorageResp;

import java.util.List;

/**
 * 存储管理对外服务接口。
 *
 * <p>跨模块协作约定：{@code file} 模块通过 {@link #getDefaultStorage()} 与 {@link #getByCode(String)}
 * 获取存储实体；通过 {@link #findEntityById(Long)} 按 ID 反查；通过 {@link #countByIds(java.util.Collection)}
 * 等做关联校验。</p>
 */
public interface StorageService {

    /**
     * 查询存储列表（按 sort 升序、id 升序）。
     *
     * @param query 过滤条件
     * @return 列表
     */
    List<StorageResp> list(StorageQuery query);

    /**
     * 查询单个存储详情。
     *
     * @param id 存储 ID
     * @return DTO
     */
    StorageResp get(Long id);

    /**
     * 新增存储。
     *
     * @param req 请求
     * @return 新增的存储 ID
     */
    Long create(StorageCreateReq req);

    /**
     * 修改存储。
     *
     * @param id  存储 ID
     * @param req 请求
     */
    void update(Long id, StorageUpdateReq req);

    /**
     * 批量删除存储；默认存储与有关联文件的存储拒绝删除。
     *
     * @param ids 存储 ID 列表
     */
    void delete(List<Long> ids);

    /**
     * 切换启用状态；默认存储不能禁用。
     *
     * @param id  存储 ID
     * @param req 请求
     */
    void updateStatus(Long id, StorageStatusUpdateReq req);

    /**
     * 设为默认存储；要求目标存储为启用状态。
     *
     * @param id 存储 ID
     */
    void setDefault(Long id);

    /**
     * 获取当前默认存储；不存在抛出业务异常。
     *
     * @return 默认存储实体
     */
    Storage getDefaultStorage();

    /**
     * 按 code 查询存储实体；不存在抛出业务异常。
     *
     * @param code 存储编码
     * @return 存储实体
     */
    Storage getByCode(String code);

    /**
     * 按 ID 查询存储实体；不存在返回 {@code null}。
     *
     * @param id 存储 ID
     * @return 存储实体
     */
    Storage findEntityById(Long id);

    /**
     * 按 ID 列表批量查询存储实体。
     *
     * @param ids 存储 ID 列表
     * @return 存储实体列表
     */
    List<Storage> findEntitiesByIds(java.util.Collection<Long> ids);
}
