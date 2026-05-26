package com.qvqw.idp.file;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.file.model.query.FileQuery;
import com.qvqw.idp.file.model.resp.FileResp;

import java.util.List;

/**
 * 文件回收站对外服务接口。
 */
public interface FileRecycleService {

    /**
     * 回收站分页查询。
     *
     * @param query 过滤条件
     * @param page  页码（从 1 开始）
     * @param size  每页条数
     * @return 分页结果
     */
    PageResp<FileResp> page(FileQuery query, int page, int size);

    /**
     * 还原文件。
     *
     * @param id 文件 ID
     */
    void restore(Long id);

    /**
     * 物理删除单个文件。
     *
     * @param id 文件 ID
     */
    void delete(Long id);

    /**
     * 清空回收站（按当前用户的可见范围；当前管理端无用户隔离，清空全部）。
     */
    void clean();

    /**
     * 跨模块协助：批量物理删除，用于 storage 删除前手工清理时复用逻辑（内部用）。
     *
     * @param ids 文件 ID 列表
     */
    void deleteAll(List<Long> ids);
}
