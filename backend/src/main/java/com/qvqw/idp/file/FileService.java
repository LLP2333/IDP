package com.qvqw.idp.file;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.file.model.query.FileQuery;
import com.qvqw.idp.file.model.req.FileCreateDirReq;
import com.qvqw.idp.file.model.req.FileUpdateReq;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.file.model.resp.FileStatisticsResp;
import com.qvqw.idp.file.model.resp.FileUploadResp;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件管理对外服务接口。
 */
public interface FileService {

    /**
     * 上传文件到默认存储。
     *
     * @param file       上传的文件
     * @param parentPath 上级目录（{@code /} 表示根）
     * @return 上传结果
     * @throws IOException 读取上传流失败时
     */
    FileUploadResp upload(MultipartFile file, String parentPath) throws IOException;

    /**
     * 文件 / 文件夹分页（不含回收站）。
     *
     * @param query    过滤条件
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @return 分页结果
     */
    PageResp<FileResp> page(FileQuery query, int page, int size);

    /**
     * 文件 / 文件夹重命名。
     *
     * @param id  ID
     * @param req 请求
     */
    void rename(Long id, FileUpdateReq req);

    /**
     * 批量删除：根据存储的 {@code recycleBinEnabled} 决定走回收站 or 物理删除。
     *
     * @param ids 文件 / 文件夹 ID 列表
     */
    void delete(List<Long> ids);

    /**
     * 资源统计（按类型聚合）。
     *
     * @return 统计结果
     */
    FileStatisticsResp statistics();

    /**
     * 按 SHA256 检查文件是否已存在；存在返回 DTO（用于秒传），否则返回 {@code null}。
     *
     * @param fileHash SHA256
     * @return 已存在文件 DTO 或 null
     */
    FileResp check(String fileHash);

    /**
     * 创建文件夹。
     *
     * @param req 请求
     * @return 文件夹 ID
     */
    Long createDir(FileCreateDirReq req);

    /**
     * 计算文件夹总大小（递归）。
     *
     * @param id 文件夹 ID
     * @return 总字节数
     */
    Long calcDirSize(Long id);

    /**
     * 跨模块查询：返回某存储下的文件数（含回收站），供 storage 模块判断关联关系。
     *
     * @param storageIds 存储 ID 列表
     * @return 文件数
     */
    long countByStorageIds(java.util.Collection<Long> storageIds);
}
