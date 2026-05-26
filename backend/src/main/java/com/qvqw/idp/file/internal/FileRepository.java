package com.qvqw.idp.file.internal;

import com.qvqw.idp.file.FileItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 文件 Repository。
 */
public interface FileRepository extends JpaRepository<FileItem, Long>, JpaSpecificationExecutor<FileItem> {

    /** 按 path 与 deleted 查询；用于父目录是否存在判断。 */
    Optional<FileItem> findFirstByPathAndTypeAndDeleted(String path, Integer type, Integer deleted);

    /** 按 parent + name + type 查询；用于重名校验。 */
    Optional<FileItem> findFirstByParentPathAndNameAndTypeAndDeleted(String parentPath, String name, Integer type, Integer deleted);

    /** 按 SHA256 查询正常文件；用于秒传。 */
    Optional<FileItem> findFirstBySha256AndDeleted(String sha256, Integer deleted);

    /** 列出某个父目录下的所有项（含回收站）。 */
    List<FileItem> findAllByParentPath(String parentPath);

    /** 列出某个父目录下的正常项。 */
    List<FileItem> findAllByParentPathAndDeleted(String parentPath, Integer deleted);

    /** 统计某些存储下的文件数（含回收站）。 */
    long countByStorageIdIn(Collection<Long> storageIds);

    /**
     * 按文件类型聚合统计（仅正常文件，DIR 不计入）。
     */
    @Query("select new com.qvqw.idp.file.internal.FileTypeStat(f.type, sum(f.size), count(f.id)) "
            + "from FileItem f where f.deleted = 0 and f.type <> 0 group by f.type")
    List<FileTypeStat> aggregateByType();

    /** 按 originalName 模糊（lower） + 其他条件查询，由 Specification 处理；此处仅占位。 */

    /** 列出某些 ID 的正常文件。 */
    List<FileItem> findAllByIdInAndDeleted(Collection<Long> ids, Integer deleted);

    /** 列出回收站中的全部文件（按 deletedAt 倒序），用于清空。 */
    List<FileItem> findAllByDeleted(Integer deleted);

    /** 检查某 path 是否有子项。 */
    @Query("select count(f) from FileItem f where f.parentPath = :path and f.storageId = :storageId")
    long countChildren(@Param("path") String path, @Param("storageId") Long storageId);
}
