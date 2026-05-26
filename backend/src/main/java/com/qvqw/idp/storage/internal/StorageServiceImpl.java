package com.qvqw.idp.storage.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageReferenceChecker;
import com.qvqw.idp.storage.StorageService;
import com.qvqw.idp.storage.StorageType;
import com.qvqw.idp.storage.model.query.StorageQuery;
import com.qvqw.idp.storage.model.req.StorageCreateReq;
import com.qvqw.idp.storage.model.req.StorageStatusUpdateReq;
import com.qvqw.idp.storage.model.req.StorageUpdateReq;
import com.qvqw.idp.storage.model.resp.StorageResp;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 存储服务实现。
 *
 * <p>编辑流程：</p>
 * <ol>
 *   <li>新增 / 修改时按 {@link StorageType} 分支校验必填字段；</li>
 *   <li>S3 类型的 SecretKey 落库前用 {@link StorageSecretCipher} 加密；</li>
 *   <li>关键改动（删除 / 修改 / 状态切换）发布 {@link StorageChangedEvent}，
 *   通知 {@code file} 模块刷新句柄缓存（{@code StorageHandlerFactory}）。</li>
 * </ol>
 *
 * <p>“是否有关联文件” 的校验通过 {@link StorageReferenceChecker}（NamedInterface）由 {@code file}
 * 模块实现并注入，避免反向依赖。</p>
 */
@Service
public class StorageServiceImpl implements StorageService {

    private final StorageRepository repository;
    private final StorageSecretCipher secretCipher;
    private final ApplicationEventPublisher publisher;
    private final StorageReferenceChecker referenceChecker;

    public StorageServiceImpl(StorageRepository repository,
                              StorageSecretCipher secretCipher,
                              ApplicationEventPublisher publisher,
                              StorageReferenceChecker referenceChecker) {
        this.repository = repository;
        this.secretCipher = secretCipher;
        this.publisher = publisher;
        this.referenceChecker = referenceChecker;
    }

    @Override
    public List<StorageResp> list(StorageQuery query) {
        Specification<Storage> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null) {
                if (query.getType() != null) {
                    predicates.add(cb.equal(root.get("type"), query.getType()));
                }
                if (StringUtils.hasText(query.getKeyword())) {
                    String pattern = "%" + query.getKeyword().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("code")), pattern),
                            cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)));
                }
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Storage> list = repository.findAll(spec, Sort.by(Sort.Direction.ASC, "sort").and(Sort.by(Sort.Direction.ASC, "id")));
        return list.stream().map(this::toResp).toList();
    }

    @Override
    public StorageResp get(Long id) {
        Storage storage = repository.findById(id)
                .orElseThrow(() -> new BusinessException("存储不存在: " + id));
        return toResp(storage);
    }

    @Override
    @Transactional
    public Long create(StorageCreateReq req) {
        StorageType type = StorageType.ofValue(req.getType());
        if (type == null) {
            throw new BusinessException("非法存储类型");
        }
        validateCreate(req, type);
        if (repository.existsByCode(req.getCode())) {
            throw new BusinessException("编码已存在: " + req.getCode());
        }
        Storage storage = new Storage();
        storage.setName(req.getName());
        storage.setCode(req.getCode());
        storage.setType(type.getValue());
        storage.setBucketName(normalizeBucket(req.getBucketName(), type));
        storage.setDomain(normalizeDomain(req.getDomain()));
        storage.setDescription(req.getDescription());
        storage.setSort(req.getSort());
        storage.setStatus(req.getStatus());
        storage.setIsDefault(Boolean.FALSE);
        storage.setRecycleBinEnabled(Boolean.TRUE.equals(req.getRecycleBinEnabled()));
        storage.setRecycleBinPath(normalizeRecycleBinPath(req.getRecycleBinPath(), req.getRecycleBinEnabled()));
        if (type == StorageType.S3) {
            storage.setAccessKey(req.getAccessKey());
            storage.setEndpoint(req.getEndpoint());
            String secret = req.getSecretKey();
            if (!StringUtils.hasText(secret)) {
                throw new BusinessException("Secret Key 不能为空");
            }
            storage.setSecretKey(secretCipher.encrypt(secret));
        }
        storage = repository.save(storage);
        publisher.publishEvent(new StorageChangedEvent(storage.getId()));
        return storage.getId();
    }

    @Override
    @Transactional
    public void update(Long id, StorageUpdateReq req) {
        Storage storage = repository.findById(id)
                .orElseThrow(() -> new BusinessException("存储不存在: " + id));
        StorageType type = StorageType.ofValue(storage.getType());
        if (type == null) {
            throw new BusinessException("存储记录损坏：未知类型");
        }
        validateUpdate(req, type);
        if (Boolean.TRUE.equals(storage.getIsDefault()) && req.getStatus() != null
                && req.getStatus() == 2) {
            throw new BusinessException("默认存储不能禁用");
        }
        storage.setName(req.getName());
        storage.setBucketName(normalizeBucket(req.getBucketName(), type));
        storage.setDomain(normalizeDomain(req.getDomain()));
        storage.setDescription(req.getDescription());
        storage.setSort(req.getSort());
        storage.setStatus(req.getStatus());
        if (type == StorageType.S3) {
            storage.setAccessKey(req.getAccessKey());
            storage.setEndpoint(req.getEndpoint());
            if (StringUtils.hasText(req.getSecretKey())) {
                storage.setSecretKey(secretCipher.encrypt(req.getSecretKey()));
            }
        }
        repository.save(storage);
        publisher.publishEvent(new StorageChangedEvent(storage.getId()));
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Storage> list = repository.findAllById(ids);
        for (Storage storage : list) {
            if (Boolean.TRUE.equals(storage.getIsDefault())) {
                throw new BusinessException("默认存储不能删除：" + storage.getName());
            }
        }
        long referenced = referenceChecker.countFilesByStorageIds(ids);
        if (referenced > 0) {
            throw new BusinessException("所选存储下尚有文件，请先清理");
        }
        repository.deleteAllByIdInBatch(ids);
        for (Storage storage : list) {
            publisher.publishEvent(new StorageChangedEvent(storage.getId()));
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, StorageStatusUpdateReq req) {
        Storage storage = repository.findById(id)
                .orElseThrow(() -> new BusinessException("存储不存在: " + id));
        if (Boolean.TRUE.equals(storage.getIsDefault()) && req.getStatus() == 2) {
            throw new BusinessException("默认存储不能禁用");
        }
        storage.setStatus(req.getStatus());
        repository.save(storage);
        publisher.publishEvent(new StorageChangedEvent(storage.getId()));
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        Storage storage = repository.findById(id)
                .orElseThrow(() -> new BusinessException("存储不存在: " + id));
        if (storage.getStatus() != 1) {
            throw new BusinessException("仅启用的存储可设为默认");
        }
        if (Boolean.TRUE.equals(storage.getIsDefault())) {
            return;
        }
        repository.findFirstByIsDefaultTrue().ifPresent(prev -> {
            prev.setIsDefault(Boolean.FALSE);
            repository.save(prev);
        });
        storage.setIsDefault(Boolean.TRUE);
        repository.save(storage);
    }

    @Override
    public Storage getDefaultStorage() {
        return repository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new BusinessException("尚未配置默认存储"));
    }

    @Override
    public Storage getByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new BusinessException("存储不存在: " + code));
    }

    @Override
    public Storage findEntityById(Long id) {
        if (id == null) {
            return null;
        }
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<Storage> findEntitiesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return repository.findAllById(ids);
    }

    /**
     * 把 {@link Storage} 实体转为对外 DTO，SecretKey 永远脱敏。
     */
    StorageResp toResp(Storage storage) {
        StorageResp resp = new StorageResp();
        resp.setId(storage.getId());
        resp.setName(storage.getName());
        resp.setCode(storage.getCode());
        resp.setType(storage.getType());
        resp.setAccessKey(storage.getAccessKey());
        if (StringUtils.hasText(storage.getSecretKey())) {
            resp.setSecretKey("******");
        }
        resp.setEndpoint(storage.getEndpoint());
        resp.setBucketName(storage.getBucketName());
        resp.setDomain(storage.getDomain());
        resp.setRecycleBinEnabled(storage.getRecycleBinEnabled());
        resp.setRecycleBinPath(storage.getRecycleBinPath());
        resp.setDescription(storage.getDescription());
        resp.setIsDefault(storage.getIsDefault());
        resp.setSort(storage.getSort());
        resp.setStatus(storage.getStatus());
        resp.setCreatedAt(storage.getCreatedAt());
        resp.setUpdatedAt(storage.getUpdatedAt());
        return resp;
    }

    private static void validateCreate(StorageCreateReq req, StorageType type) {
        validateCommon(req.getBucketName(), req.getDomain(), type);
        if (type == StorageType.S3) {
            if (!StringUtils.hasText(req.getAccessKey())) {
                throw new BusinessException("Access Key 不能为空");
            }
            if (!StringUtils.hasText(req.getSecretKey())) {
                throw new BusinessException("Secret Key 不能为空");
            }
            if (!StringUtils.hasText(req.getEndpoint())) {
                throw new BusinessException("Endpoint 不能为空");
            }
        }
        if (Boolean.TRUE.equals(req.getRecycleBinEnabled())
                && !StringUtils.hasText(req.getRecycleBinPath())) {
            throw new BusinessException("启用回收站时回收站路径不能为空");
        }
    }

    private static void validateUpdate(StorageUpdateReq req, StorageType type) {
        validateCommon(req.getBucketName(), req.getDomain(), type);
        if (type == StorageType.S3) {
            if (!StringUtils.hasText(req.getAccessKey())) {
                throw new BusinessException("Access Key 不能为空");
            }
            if (!StringUtils.hasText(req.getEndpoint())) {
                throw new BusinessException("Endpoint 不能为空");
            }
        }
    }

    private static void validateCommon(String bucket, String domain, StorageType type) {
        if (!StringUtils.hasText(bucket)) {
            throw new BusinessException("桶名 / 存储路径不能为空");
        }
        if (type == StorageType.LOCAL && !StringUtils.hasText(domain)) {
            throw new BusinessException("本地存储必须配置访问域名");
        }
        if (StringUtils.hasText(domain) && !(domain.startsWith("http://") || domain.startsWith("https://"))) {
            throw new BusinessException("访问域名必须以 http:// 或 https:// 开头");
        }
    }

    private static String normalizeBucket(String bucket, StorageType type) {
        if (bucket == null) {
            return null;
        }
        if (type == StorageType.LOCAL) {
            return bucket.endsWith("/") ? bucket : bucket + "/";
        }
        return bucket;
    }

    private static String normalizeDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return domain;
        }
        return domain.endsWith("/") ? domain : domain + "/";
    }

    private static String normalizeRecycleBinPath(String path, Boolean enabled) {
        if (!Boolean.TRUE.equals(enabled) || !StringUtils.hasText(path)) {
            return null;
        }
        String p = path.startsWith("/") ? path.substring(1) : path;
        return p.endsWith("/") ? p : p + "/";
    }

    /**
     * 存储配置变更事件。
     *
     * <p>用于通知 {@link com.qvqw.idp.storage.internal.StorageHandlerFactory} 失效本地缓存。</p>
     */
    public record StorageChangedEvent(Long storageId) implements Serializable {
    }
}
