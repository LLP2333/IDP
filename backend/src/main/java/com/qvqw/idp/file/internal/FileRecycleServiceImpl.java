package com.qvqw.idp.file.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.file.FileItem;
import com.qvqw.idp.file.FileRecycleService;
import com.qvqw.idp.file.FileTypeEnum;
import com.qvqw.idp.file.model.query.FileQuery;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StorageHandlerFactory;
import com.qvqw.idp.storage.StorageService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 回收站业务实现。
 *
 * <p>回收站的存储侧路径 = {@code storage.recycleBinPath + 原 objectKey}，因此回收站内文件的
 * {@code name} 字段在 {@code FileServiceImpl.moveToRecycleBin} 中已替换为带 recycleBinPath 的版本。
 * 还原时把 name 还原到 {@code originalKey}（去掉 recycleBinPath 前缀）。</p>
 */
@Service
public class FileRecycleServiceImpl implements FileRecycleService {

    private static final Logger log = LoggerFactory.getLogger(FileRecycleServiceImpl.class);

    private final FileRepository repository;
    private final StorageService storageService;
    private final StorageHandlerFactory handlerFactory;
    private final FileServiceImpl fileService;

    public FileRecycleServiceImpl(FileRepository repository,
                                  StorageService storageService,
                                  StorageHandlerFactory handlerFactory,
                                  FileServiceImpl fileService) {
        this.repository = repository;
        this.storageService = storageService;
        this.handlerFactory = handlerFactory;
        this.fileService = fileService;
    }

    @Override
    public PageResp<FileResp> page(FileQuery query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Specification<FileItem> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 1));
            if (query != null) {
                if (StringUtils.hasText(query.getOriginalName())) {
                    predicates.add(cb.like(cb.lower(root.get("originalName")),
                            "%" + query.getOriginalName().trim().toLowerCase() + "%"));
                }
                if (query.getType() != null && query.getType() != 0) {
                    predicates.add(cb.equal(root.get("type"), query.getType()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<FileItem> result = repository.findAll(spec, pageable);
        Map<Long, Storage> storageMap = loadStorages(result.getContent());
        return PageResp.from(result, item -> fileService.toResp(item, storageMap));
    }

    @Override
    @Transactional
    public void restore(Long id) {
        FileItem item = repository.findById(id).orElseThrow(() -> new BusinessException("文件不存在: " + id));
        if (item.getDeleted() == null || item.getDeleted() != 1) {
            throw new BusinessException("文件不在回收站");
        }
        Storage storage = storageService.findEntityById(item.getStorageId());
        if (storage == null) {
            throw new BusinessException("文件关联的存储已不存在");
        }
        String recycleBinPath = storage.getRecycleBinPath();
        if (recycleBinPath == null || recycleBinPath.isEmpty()) {
            // 没有回收站路径，无法做存储侧还原，仅恢复数据库标记
            item.setDeleted(0);
            item.setDeletedAt(null);
            item.setDeletedBy(null);
            repository.save(item);
            return;
        }
        if (!item.getName().startsWith(recycleBinPath)) {
            throw new BusinessException("回收站对象 key 异常: " + item.getName());
        }
        String originalKey = item.getName().substring(recycleBinPath.length());
        StorageHandler handler = handlerFactory.get(storage);
        try {
            handler.move(item.getName(), originalKey);
        } catch (Exception e) {
            log.warn("还原文件失败 from={} to={} err={}", item.getName(), originalKey, e.getMessage());
            throw new BusinessException("还原文件失败：" + e.getMessage());
        }
        item.setName(originalKey);
        item.setDeleted(0);
        item.setDeletedAt(null);
        item.setDeletedBy(null);
        repository.save(item);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        deleteAll(List.of(id));
    }

    @Override
    @Transactional
    public void clean() {
        List<FileItem> all = repository.findAllByDeleted(1);
        deleteEntities(all);
    }

    @Override
    @Transactional
    public void deleteAll(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<FileItem> items = repository.findAllByIdInAndDeleted(ids, 1);
        deleteEntities(items);
    }

    private void deleteEntities(List<FileItem> items) {
        if (items.isEmpty()) {
            return;
        }
        Map<Long, Storage> storageMap = loadStorages(items);
        for (FileItem item : items) {
            Storage storage = storageMap.get(item.getStorageId());
            if (storage == null) {
                repository.delete(item);
                continue;
            }
            if (item.getType() != null && item.getType() == FileTypeEnum.DIR.getValue()) {
                repository.delete(item);
                continue;
            }
            StorageHandler handler = handlerFactory.get(storage);
            try {
                handler.delete(item.getName());
                if (StringUtils.hasText(item.getThumbnailName())) {
                    handler.delete(item.getThumbnailName());
                }
            } catch (Exception e) {
                log.warn("回收站物理删除失败 key={} err={}", item.getName(), e.getMessage());
            }
            repository.delete(item);
        }
    }

    private Map<Long, Storage> loadStorages(List<FileItem> items) {
        Set<Long> ids = new HashSet<>();
        for (FileItem item : items) {
            if (item.getStorageId() != null) {
                ids.add(item.getStorageId());
            }
        }
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, Storage> map = new HashMap<>();
        for (Storage storage : storageService.findEntitiesByIds(ids)) {
            map.put(storage.getId(), storage);
        }
        return map;
    }
}
