package com.qvqw.idp.file.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.file.FileItem;
import com.qvqw.idp.file.FileService;
import com.qvqw.idp.file.FileTypeEnum;
import com.qvqw.idp.file.model.query.FileQuery;
import com.qvqw.idp.file.model.req.FileCreateDirReq;
import com.qvqw.idp.file.model.req.FileUpdateReq;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.file.model.resp.FileStatisticsResp;
import com.qvqw.idp.file.model.resp.FileUploadResp;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StorageHandlerFactory;
import com.qvqw.idp.storage.StorageService;
import com.qvqw.idp.storage.StoredObject;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件管理业务实现。
 *
 * <p>关键约束：</p>
 * <ul>
 *   <li>{@code parentPath} 以 {@code /} 开头不以 {@code /} 结尾，根目录为 {@code /}；</li>
 *   <li>每次上传都走 “默认存储”；</li>
 *   <li>上传前根据 SHA256 命中已有文件可秒传（直接复用既有文件的存储 URL 与缩略图）；</li>
 *   <li>多级父目录自动创建，{@code storageId} 必须与父级一致。</li>
 * </ul>
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileRepository repository;
    private final StorageService storageService;
    private final StorageHandlerFactory handlerFactory;
    private final ThumbnailGenerator thumbnailGenerator;

    public FileServiceImpl(FileRepository repository,
                           StorageService storageService,
                           StorageHandlerFactory handlerFactory,
                           ThumbnailGenerator thumbnailGenerator) {
        this.repository = repository;
        this.storageService = storageService;
        this.handlerFactory = handlerFactory;
        this.thumbnailGenerator = thumbnailGenerator;
    }

    // ====== 上传 ======

    @Override
    @Transactional
    public FileUploadResp upload(MultipartFile file, String parentPath) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) {
            throw new BusinessException("文件名不能为空");
        }
        String ext = FileNameGenerator.extOf(original);
        if (ext.isEmpty()) {
            throw new BusinessException("不支持无扩展名的文件");
        }
        if (!FileTypeEnum.allExtensions().contains(ext)) {
            throw new BusinessException("不支持的文件扩展名: " + ext);
        }

        Storage storage = storageService.getDefaultStorage();
        if (storage.getStatus() != 1) {
            throw new BusinessException("默认存储已禁用: " + storage.getCode());
        }
        String normalizedParent = normalizeParent(parentPath);
        ensureParentDirs(normalizedParent, storage);

        // 读取所有字节用于 SHA256 + 缩略图（适用于完整版的中等大小文件；超大文件应走分片上传）
        byte[] bytes = file.getBytes();
        String sha256 = sha256Hex(bytes);

        // 秒传
        FileItem existing = repository.findFirstBySha256AndDeleted(sha256, 0).orElse(null);
        if (existing != null) {
            FileItem clone = duplicateRecord(existing, normalizedParent);
            FileResp resp = toResp(clone, Map.of(storage.getId(), storage));
            return new FileUploadResp(clone.getId(), resp.getUrl(), resp.getThumbnailUrl(), clone.getMetadata());
        }

        StorageHandler handler = handlerFactory.get(storage);
        String storageName = FileNameGenerator.generateStorageName(ext);
        String resolvedOriginal = FileNameGenerator.resolveUniqueOriginalName(original, normalizedParent, storage.getId(), repository);
        String datePrefix = FileNameGenerator.datePrefix();
        String objectKey = datePrefix + storageName;
        StoredObject stored = handler.upload(new ByteArrayInputStream(bytes), objectKey, bytes.length, file.getContentType());

        FileTypeEnum fileType = FileTypeEnum.getByExtension(ext);

        String thumbnailName = null;
        Long thumbnailSize = null;
        if (fileType == FileTypeEnum.IMAGE) {
            byte[] thumbBytes = thumbnailGenerator.generate(bytes);
            if (thumbBytes != null && thumbBytes.length > 0) {
                String thumbStorageName = "thumb_" + storageName.replaceAll("\\.[^.]+$", ".jpg");
                String thumbObjectKey = datePrefix + thumbStorageName;
                handler.upload(new ByteArrayInputStream(thumbBytes), thumbObjectKey, thumbBytes.length, "image/jpeg");
                thumbnailName = thumbObjectKey;
                thumbnailSize = (long) thumbBytes.length;
            }
        }

        FileItem item = new FileItem();
        item.setName(objectKey);
        item.setOriginalName(resolvedOriginal);
        item.setSize(stored.size());
        item.setExtension(ext);
        item.setContentType(file.getContentType());
        item.setType(fileType.getValue());
        item.setSha256(sha256);
        item.setStorageId(storage.getId());
        item.setThumbnailName(thumbnailName);
        item.setThumbnailSize(thumbnailSize);
        item.setParentPath(normalizedParent);
        // setParentPath 内部会刷新 path，但 name 此时是 objectKey；我们需要 path 用展示名
        item.setPath(FileNameGenerator.composePath(normalizedParent, resolvedOriginal));
        item.setDeleted(0);
        item = repository.save(item);

        FileResp resp = toResp(item, Map.of(storage.getId(), storage));
        return new FileUploadResp(item.getId(), resp.getUrl(), resp.getThumbnailUrl(), item.getMetadata());
    }

    /**
     * 秒传：复用已有文件的存储对象与缩略图，仅在数据库插入新行（不同 parentPath / originalName）。
     */
    private FileItem duplicateRecord(FileItem existing, String parentPath) {
        FileItem item = new FileItem();
        item.setName(existing.getName());
        item.setOriginalName(FileNameGenerator.resolveUniqueOriginalName(
                existing.getOriginalName(), parentPath, existing.getStorageId(), repository));
        item.setSize(existing.getSize());
        item.setExtension(existing.getExtension());
        item.setContentType(existing.getContentType());
        item.setType(existing.getType());
        item.setSha256(existing.getSha256());
        item.setStorageId(existing.getStorageId());
        item.setThumbnailName(existing.getThumbnailName());
        item.setThumbnailSize(existing.getThumbnailSize());
        item.setMetadata(existing.getMetadata());
        item.setParentPath(parentPath);
        item.setPath(FileNameGenerator.composePath(parentPath, item.getOriginalName()));
        item.setDeleted(0);
        return repository.save(item);
    }

    // ====== 查询 ======

    @Override
    public PageResp<FileResp> page(FileQuery query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.ASC, "type").and(Sort.by(Sort.Direction.DESC, "updatedAt")));
        Specification<FileItem> spec = buildSpec(query, /* deleted= */ 0);
        Page<FileItem> result = repository.findAll(spec, pageable);
        Map<Long, Storage> storageMap = loadStorages(result.getContent());
        return PageResp.from(result, item -> toResp(item, storageMap));
    }

    @Override
    public FileStatisticsResp statistics() {
        List<FileTypeStat> stats = repository.aggregateByType();
        FileStatisticsResp resp = new FileStatisticsResp();
        if (stats.isEmpty()) {
            return resp;
        }
        List<FileStatisticsResp> details = new ArrayList<>();
        long totalSize = 0L;
        long totalNum = 0L;
        for (FileTypeStat s : stats) {
            FileStatisticsResp item = new FileStatisticsResp();
            item.setType(s.type());
            FileTypeEnum fte = FileTypeEnum.ofValue(s.type());
            item.setName(fte == null ? "其他" : fte.getDescription());
            item.setSize(s.size() == null ? 0L : s.size());
            item.setNumber(s.number() == null ? 0L : s.number());
            details.add(item);
            totalSize += item.getSize();
            totalNum += item.getNumber();
        }
        resp.setData(details);
        resp.setSize(totalSize);
        resp.setNumber(totalNum);
        return resp;
    }

    @Override
    public FileResp check(String fileHash) {
        if (!StringUtils.hasText(fileHash)) {
            return null;
        }
        FileItem item = repository.findFirstBySha256AndDeleted(fileHash, 0).orElse(null);
        if (item == null) {
            return null;
        }
        return toResp(item, loadStorages(List.of(item)));
    }

    @Override
    public Long calcDirSize(Long id) {
        FileItem item = repository.findById(id).orElseThrow(() -> new BusinessException("文件不存在: " + id));
        if (item.getType() != FileTypeEnum.DIR.getValue()) {
            throw new BusinessException("仅文件夹支持计算大小");
        }
        return calcDirSizeRecursive(item.getPath(), item.getStorageId());
    }

    private long calcDirSizeRecursive(String dirPath, Long storageId) {
        List<FileItem> children = repository.findAllByParentPathAndDeleted(dirPath, 0).stream()
                .filter(f -> storageId.equals(f.getStorageId())).toList();
        long sum = 0;
        for (FileItem child : children) {
            if (child.getType() == FileTypeEnum.DIR.getValue()) {
                sum += calcDirSizeRecursive(child.getPath(), storageId);
            } else {
                sum += child.getSize() == null ? 0 : child.getSize();
            }
        }
        return sum;
    }

    @Override
    public long countByStorageIds(Collection<Long> storageIds) {
        if (storageIds == null || storageIds.isEmpty()) {
            return 0L;
        }
        return repository.countByStorageIdIn(storageIds);
    }

    // ====== 重命名 ======

    @Override
    @Transactional
    public void rename(Long id, FileUpdateReq req) {
        FileItem item = repository.findById(id).orElseThrow(() -> new BusinessException("文件不存在: " + id));
        String newName = req.getOriginalName().trim();
        if (newName.equals(item.getOriginalName())) {
            return;
        }
        boolean duplicate = repository.findFirstByParentPathAndNameAndTypeAndDeleted(
                item.getParentPath(), newName, item.getType(), 0).isPresent();
        if (duplicate) {
            throw new BusinessException("同目录下已存在同名");
        }
        item.setOriginalName(newName);
        item.setPath(FileNameGenerator.composePath(item.getParentPath(), newName));
        repository.save(item);
    }

    // ====== 删除（含回收站联动） ======

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<FileItem> items = repository.findAllByIdInAndDeleted(ids, 0);
        if (items.isEmpty()) {
            return;
        }
        Map<Long, Storage> storageMap = loadStorages(items);
        for (FileItem item : items) {
            Storage storage = storageMap.get(item.getStorageId());
            if (storage == null) {
                continue;
            }
            if (item.getType() == FileTypeEnum.DIR.getValue()) {
                long children = repository.countChildren(item.getPath(), item.getStorageId());
                if (children > 0) {
                    throw new BusinessException("文件夹 [" + item.getOriginalName() + "] 非空，请先清空内容");
                }
                repository.delete(item);
                continue;
            }
            if (Boolean.TRUE.equals(storage.getRecycleBinEnabled())) {
                moveToRecycleBin(item, storage);
            } else {
                hardDelete(item, storage);
            }
        }
    }

    /**
     * 把文件放入回收站：移动存储侧对象到 recycleBinPath 下，标记 deleted=1。
     */
    private void moveToRecycleBin(FileItem item, Storage storage) {
        StorageHandler handler = handlerFactory.get(storage);
        String recycleBinPath = storage.getRecycleBinPath();
        if (recycleBinPath == null || recycleBinPath.isEmpty()) {
            hardDelete(item, storage);
            return;
        }
        String fromKey = item.getName();
        String toKey = recycleBinPath + fromKey;
        try {
            handler.move(fromKey, toKey);
        } catch (Exception e) {
            log.warn("移动文件到回收站失败 fromKey={} toKey={} err={}", fromKey, toKey, e.getMessage());
        }
        item.setDeleted(1);
        item.setDeletedAt(LocalDateTime.now());
        item.setDeletedBy(UserContextHolder.getUserId());
        item.setName(toKey);
        repository.save(item);
    }

    /**
     * 物理删除：从存储侧删除并从数据库删除。
     */
    private void hardDelete(FileItem item, Storage storage) {
        StorageHandler handler = handlerFactory.get(storage);
        try {
            handler.delete(item.getName());
            if (StringUtils.hasText(item.getThumbnailName())) {
                handler.delete(item.getThumbnailName());
            }
        } catch (Exception e) {
            log.warn("物理删除文件失败 storage={} key={} err={}", storage.getCode(), item.getName(), e.getMessage());
        }
        repository.delete(item);
    }

    // ====== 创建文件夹 ======

    @Override
    @Transactional
    public Long createDir(FileCreateDirReq req) {
        String parent = normalizeParent(req.getParentPath());
        Storage storage = storageService.getDefaultStorage();
        ensureParentDirs(parent, storage);
        boolean duplicate = repository.findFirstByParentPathAndNameAndTypeAndDeleted(parent, req.getOriginalName(),
                FileTypeEnum.DIR.getValue(), 0).isPresent();
        if (duplicate) {
            throw new BusinessException("同目录下已存在同名文件夹");
        }
        FileItem dir = createDir(parent, req.getOriginalName(), storage.getId());
        return dir.getId();
    }

    /**
     * 确保给定父目录的整条链路都已存在；缺失的层级会自动创建。
     */
    private void ensureParentDirs(String parentPath, Storage storage) {
        if (parentPath == null || "/".equals(parentPath)) {
            return;
        }
        String[] parts = parentPath.split("/");
        StringBuilder current = new StringBuilder();
        String last = "/";
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            current.append("/").append(part);
            String path = current.toString();
            FileItem dir = repository.findFirstByPathAndTypeAndDeleted(path, FileTypeEnum.DIR.getValue(), 0).orElse(null);
            if (dir != null) {
                if (!storage.getId().equals(dir.getStorageId())) {
                    throw new BusinessException("文件夹和上传文件存储引擎不一致: " + path);
                }
                last = path;
                continue;
            }
            createDir(last, part, storage.getId());
            last = path;
        }
    }

    private FileItem createDir(String parent, String name, Long storageId) {
        FileItem dir = new FileItem();
        dir.setName(name);
        dir.setOriginalName(name);
        dir.setSize(0L);
        dir.setExtension("");
        dir.setType(FileTypeEnum.DIR.getValue());
        dir.setStorageId(storageId);
        dir.setParentPath(parent);
        dir.setPath(FileNameGenerator.composePath(parent, name));
        dir.setDeleted(0);
        return repository.save(dir);
    }

    // ====== 工具方法 ======

    private static String normalizeParent(String parentPath) {
        if (!StringUtils.hasText(parentPath)) {
            return "/";
        }
        String p = parentPath.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException("计算 SHA256 失败: " + e.getMessage());
        }
    }

    /**
     * 加载这些文件对应的存储映射，避免逐行 N+1。
     */
    Map<Long, Storage> loadStorages(Collection<FileItem> items) {
        Set<Long> ids = new HashSet<>();
        for (FileItem item : items) {
            if (item.getStorageId() != null) {
                ids.add(item.getStorageId());
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Storage> map = new HashMap<>();
        for (Storage storage : storageService.findEntitiesByIds(ids)) {
            map.put(storage.getId(), storage);
        }
        return map;
    }

    /**
     * 把实体转为 DTO，自动拼接 URL。
     */
    FileResp toResp(FileItem item, Map<Long, Storage> storageMap) {
        FileResp resp = new FileResp();
        resp.setId(item.getId());
        resp.setName(item.getName());
        resp.setOriginalName(item.getOriginalName());
        resp.setSize(item.getSize());
        resp.setParentPath(item.getParentPath());
        resp.setPath(item.getPath());
        resp.setExtension(item.getExtension());
        resp.setContentType(item.getContentType());
        resp.setType(item.getType());
        resp.setSha256(item.getSha256());
        resp.setMetadata(item.getMetadata());
        resp.setStorageId(item.getStorageId());
        resp.setCreatedAt(item.getCreatedAt());
        resp.setUpdatedAt(item.getUpdatedAt());
        resp.setDeletedAt(item.getDeletedAt());
        if (item.getType() != null && item.getType() != FileTypeEnum.DIR.getValue()) {
            Storage storage = storageMap.get(item.getStorageId());
            if (storage != null) {
                StorageHandler handler = handlerFactory.get(storage);
                resp.setUrl(handler.resolveUrl(item.getName()));
                if (StringUtils.hasText(item.getThumbnailName())) {
                    resp.setThumbnailUrl(handler.resolveUrl(item.getThumbnailName()));
                }
                resp.setStorageName(storage.getName() + " (" + storage.getCode() + ")");
            }
        }
        return resp;
    }

    /**
     * 构建分页 Specification。
     *
     * @param query   查询条件
     * @param deleted 0=正常,1=回收站
     */
    Specification<FileItem> buildSpec(FileQuery query, Integer deleted) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), deleted));
            if (query != null) {
                if (StringUtils.hasText(query.getOriginalName())) {
                    predicates.add(cb.like(cb.lower(root.get("originalName")),
                            "%" + query.getOriginalName().trim().toLowerCase() + "%"));
                }
                if (query.getType() != null && query.getType() != 0) {
                    predicates.add(cb.equal(root.get("type"), query.getType()));
                } else if (StringUtils.hasText(query.getParentPath())) {
                    predicates.add(cb.equal(root.get("parentPath"), query.getParentPath()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ====== 给 internal 包内其他类用的辅助 ======

    FileRepository repository() {
        return repository;
    }

    StorageService storageService() {
        return storageService;
    }

    StorageHandlerFactory handlerFactory() {
        return handlerFactory;
    }
}
