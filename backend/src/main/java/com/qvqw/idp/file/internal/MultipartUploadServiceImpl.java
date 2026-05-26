package com.qvqw.idp.file.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.file.FileItem;
import com.qvqw.idp.file.FileTypeEnum;
import com.qvqw.idp.file.MultipartUploadService;
import com.qvqw.idp.file.model.req.MultipartUploadInitReq;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.file.model.resp.MultipartUploadInitResp;
import com.qvqw.idp.file.model.resp.MultipartUploadPartResp;
import com.qvqw.idp.storage.PartETag;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StorageHandlerFactory;
import com.qvqw.idp.storage.StorageService;
import com.qvqw.idp.storage.StoredObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 分片上传业务实现。
 *
 * <p>底层 IO 委托给 {@link StorageHandler}（LOCAL / S3 两套实现），Redis 仅记录 uploadId 元数据 + 已上传分片 ETag。</p>
 */
@Service
public class MultipartUploadServiceImpl implements MultipartUploadService {

    private final FileRepository repository;
    private final StorageService storageService;
    private final StorageHandlerFactory handlerFactory;
    private final MultipartUploadStateCache stateCache;
    private final FileServiceImpl fileService;

    public MultipartUploadServiceImpl(FileRepository repository,
                                      StorageService storageService,
                                      StorageHandlerFactory handlerFactory,
                                      MultipartUploadStateCache stateCache,
                                      FileServiceImpl fileService) {
        this.repository = repository;
        this.storageService = storageService;
        this.handlerFactory = handlerFactory;
        this.stateCache = stateCache;
        this.fileService = fileService;
    }

    @Override
    @Transactional
    public MultipartUploadInitResp init(MultipartUploadInitReq req) {
        // 1. 秒传
        FileItem existing = repository.findFirstBySha256AndDeleted(req.getSha256(), 0).orElse(null);
        Storage storage = storageService.getDefaultStorage();
        if (existing != null) {
            FileItem clone = duplicateForFastUpload(existing, normalizeParent(req.getParentPath()), req.getFileName());
            MultipartUploadInitResp resp = new MultipartUploadInitResp();
            resp.setExisting(fileService.toResp(clone, Map.of(storage.getId(), storage)));
            return resp;
        }

        String ext = FileNameGenerator.extOf(req.getFileName());
        if (ext.isEmpty()) {
            throw new BusinessException("不支持无扩展名的文件");
        }
        if (!FileTypeEnum.allExtensions().contains(ext)) {
            throw new BusinessException("不支持的文件扩展名: " + ext);
        }
        if (storage.getStatus() != 1) {
            throw new BusinessException("默认存储已禁用: " + storage.getCode());
        }
        String parent = normalizeParent(req.getParentPath());
        // 触发父目录链路自动创建
        ensureParentDirsViaFileService(parent, storage);

        StorageHandler handler = handlerFactory.get(storage);
        String storageName = FileNameGenerator.generateStorageName(ext);
        String objectKey = FileNameGenerator.datePrefix() + storageName;
        String contentType = guessContentType(req.getFileName());
        String uploadId = handler.initMultipartUpload(objectKey, contentType);

        MultipartUploadState state = new MultipartUploadState();
        state.setUploadId(uploadId);
        state.setObjectKey(objectKey);
        state.setOriginalName(req.getFileName());
        state.setParentPath(parent);
        state.setContentType(contentType);
        state.setFileSize(req.getFileSize());
        state.setSha256(req.getSha256());
        state.setStorageId(storage.getId());
        state.setExtension(ext);
        stateCache.save(state);

        MultipartUploadInitResp resp = new MultipartUploadInitResp();
        resp.setUploadId(uploadId);
        return resp;
    }

    private void ensureParentDirsViaFileService(String parent, Storage storage) {
        if ("/".equals(parent)) {
            return;
        }
        // 借助 FileServiceImpl 内部方法递归创建：这里通过暴露 createDir 链路实现
        // 为避免暴露 internal 细节，简单复制一套：
        String[] parts = parent.split("/");
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
            FileItem d = new FileItem();
            d.setName(part);
            d.setOriginalName(part);
            d.setSize(0L);
            d.setExtension("");
            d.setType(FileTypeEnum.DIR.getValue());
            d.setStorageId(storage.getId());
            d.setParentPath(last);
            d.setPath(FileNameGenerator.composePath(last, part));
            d.setDeleted(0);
            repository.save(d);
            last = path;
        }
    }

    private FileItem duplicateForFastUpload(FileItem existing, String parentPath, String displayName) {
        FileItem item = new FileItem();
        item.setName(existing.getName());
        item.setOriginalName(FileNameGenerator.resolveUniqueOriginalName(
                displayName == null ? existing.getOriginalName() : displayName,
                parentPath, existing.getStorageId(), repository));
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

    @Override
    public MultipartUploadPartResp uploadPart(String uploadId, int partNumber, MultipartFile part) throws IOException {
        if (partNumber < 1) {
            throw new BusinessException("分片编号必须 ≥ 1");
        }
        MultipartUploadState state = stateCache.get(uploadId);
        if (state == null) {
            throw new BusinessException("上传会话不存在或已过期: " + uploadId);
        }
        Storage storage = storageService.findEntityById(state.getStorageId());
        if (storage == null) {
            throw new BusinessException("存储已不存在");
        }
        StorageHandler handler = handlerFactory.get(storage);
        String etag = handler.uploadPart(uploadId, state.getObjectKey(), partNumber, part.getInputStream(), part.getSize());

        // 同 partNumber 重复上传：覆盖
        state.getParts().removeIf(p -> p.getPartNumber() == partNumber);
        state.getParts().add(new MultipartUploadState.Part(partNumber, etag));
        stateCache.save(state);
        return new MultipartUploadPartResp(partNumber, etag);
    }

    @Override
    @Transactional
    public FileResp complete(String uploadId) {
        MultipartUploadState state = stateCache.get(uploadId);
        if (state == null) {
            throw new BusinessException("上传会话不存在或已过期: " + uploadId);
        }
        if (state.getParts() == null || state.getParts().isEmpty()) {
            throw new BusinessException("尚未上传任何分片");
        }
        Storage storage = storageService.findEntityById(state.getStorageId());
        if (storage == null) {
            throw new BusinessException("存储已不存在");
        }
        StorageHandler handler = handlerFactory.get(storage);
        List<PartETag> parts = state.getParts().stream()
                .map(p -> new PartETag(p.getPartNumber(), p.getEtag()))
                .toList();
        StoredObject stored = handler.completeMultipartUpload(uploadId, state.getObjectKey(), parts);

        FileTypeEnum fileType = FileTypeEnum.getByExtension(state.getExtension());
        String resolvedOriginal = FileNameGenerator.resolveUniqueOriginalName(
                state.getOriginalName(), state.getParentPath(), state.getStorageId(), repository);

        FileItem item = new FileItem();
        item.setName(state.getObjectKey());
        item.setOriginalName(resolvedOriginal);
        item.setSize(stored.size() > 0 ? stored.size() : state.getFileSize());
        item.setExtension(state.getExtension());
        item.setContentType(state.getContentType());
        item.setType(fileType.getValue());
        item.setSha256(state.getSha256());
        item.setStorageId(state.getStorageId());
        item.setParentPath(state.getParentPath());
        item.setPath(FileNameGenerator.composePath(state.getParentPath(), resolvedOriginal));
        item.setDeleted(0);
        item = repository.save(item);
        stateCache.remove(uploadId);
        return fileService.toResp(item, Map.of(storage.getId(), storage));
    }

    @Override
    public void cancel(String uploadId) {
        MultipartUploadState state = stateCache.get(uploadId);
        if (state != null) {
            Storage storage = storageService.findEntityById(state.getStorageId());
            if (storage != null) {
                try {
                    handlerFactory.get(storage).abortMultipartUpload(uploadId, state.getObjectKey());
                } catch (Exception ignored) {
                }
            }
        }
        stateCache.remove(uploadId);
    }

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

    private static String guessContentType(String fileName) {
        String ext = FileNameGenerator.extOf(fileName);
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }
}
