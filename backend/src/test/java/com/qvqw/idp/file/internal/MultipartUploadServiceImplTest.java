package com.qvqw.idp.file.internal;

import com.qvqw.idp.file.FileItem;
import com.qvqw.idp.file.model.req.MultipartUploadInitReq;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.file.model.resp.MultipartUploadInitResp;
import com.qvqw.idp.file.model.resp.MultipartUploadPartResp;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StorageHandlerFactory;
import com.qvqw.idp.storage.StorageService;
import com.qvqw.idp.storage.StorageType;
import com.qvqw.idp.storage.StoredObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MultipartUploadServiceImplTest {

    @Mock
    FileRepository repository;

    @Mock
    StorageService storageService;

    @Mock
    StorageHandlerFactory handlerFactory;

    @Mock
    StorageHandler handler;

    MultipartUploadStateCache cache;

    FileServiceImpl fileService;
    MultipartUploadServiceImpl multipartService;

    Storage storage;

    @BeforeEach
    void setUp() {
        cache = new MultipartUploadStateCache(null);
        ThumbnailGenerator thumb = new ThumbnailGenerator();
        fileService = new FileServiceImpl(repository, storageService, handlerFactory, thumb);
        multipartService = new MultipartUploadServiceImpl(repository, storageService, handlerFactory, cache, fileService);
        storage = new Storage();
        storage.setId(1L);
        storage.setCode("local");
        storage.setName("本地");
        storage.setType(StorageType.LOCAL.getValue());
        storage.setBucketName("/tmp/");
        storage.setStatus(1);
        when(storageService.getDefaultStorage()).thenReturn(storage);
        when(storageService.findEntityById(1L)).thenReturn(storage);
        when(storageService.findEntitiesByIds(anyCollection())).thenReturn(List.of(storage));
        when(handlerFactory.get(any(Storage.class))).thenReturn(handler);
        when(repository.save(any(FileItem.class))).thenAnswer(inv -> {
            FileItem f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId((long) Math.abs(System.nanoTime() % 1_000_000));
            }
            return f;
        });
    }

    @Test
    void initShouldUseFastUploadWhenSha256Hits() {
        FileItem existing = new FileItem();
        existing.setId(99L);
        existing.setOriginalName("hi.txt");
        existing.setSize(5L);
        existing.setExtension("txt");
        existing.setType(3);
        existing.setStorageId(1L);
        existing.setSha256("abc");
        existing.setParentPath("/old");
        existing.setPath("/old/hi.txt");
        existing.setName("2025/05/hi.txt");
        when(repository.findFirstBySha256AndDeleted("abc", 0)).thenReturn(Optional.of(existing));

        MultipartUploadInitReq req = new MultipartUploadInitReq();
        req.setFileName("hi.txt");
        req.setFileSize(5L);
        req.setChunkSize(5L * 1024 * 1024);
        req.setSha256("abc");
        req.setParentPath("/");

        MultipartUploadInitResp resp = multipartService.init(req);
        assertThat(resp.getExisting()).isNotNull();
        assertThat(resp.getUploadId()).isNull();
    }

    @Test
    void fullMultipartLifecycleShouldSucceed() throws IOException {
        when(repository.findFirstBySha256AndDeleted(anyString(), eq(0))).thenReturn(Optional.empty());
        when(handler.initMultipartUpload(anyString(), anyString())).thenReturn("uid-1");
        when(handler.uploadPart(anyString(), anyString(), anyInt(), any(), anyLong()))
                .thenAnswer(inv -> "etag-" + inv.getArgument(2));
        when(handler.completeMultipartUpload(anyString(), anyString(), any()))
                .thenAnswer(inv -> new StoredObject((String) inv.getArgument(1), "etag-final", 10L, null));
        when(handler.resolveUrl(anyString())).thenAnswer(inv -> "URL://" + inv.getArgument(0));

        MultipartUploadInitReq initReq = new MultipartUploadInitReq();
        initReq.setFileName("big.mp4");
        initReq.setFileSize(10L);
        initReq.setChunkSize(5L);
        initReq.setSha256("hash-xx");
        initReq.setParentPath("/");
        MultipartUploadInitResp init = multipartService.init(initReq);
        assertThat(init.getUploadId()).isEqualTo("uid-1");

        MockMultipartFile part1 = new MockMultipartFile("file", "p1", null, new byte[]{1, 2, 3, 4, 5});
        MockMultipartFile part2 = new MockMultipartFile("file", "p2", null, new byte[]{6, 7, 8, 9, 0});
        MultipartUploadPartResp r1 = multipartService.uploadPart("uid-1", 1, part1);
        MultipartUploadPartResp r2 = multipartService.uploadPart("uid-1", 2, part2);
        assertThat(r1.getEtag()).isEqualTo("etag-1");
        assertThat(r2.getEtag()).isEqualTo("etag-2");

        FileResp finalResp = multipartService.complete("uid-1");
        assertThat(finalResp.getId()).isNotNull();
        assertThat(finalResp.getOriginalName()).isEqualTo("big.mp4");
        assertThat(finalResp.getUrl()).startsWith("URL://");
    }

    @Test
    void cancelShouldAbortAndRemoveState() {
        MultipartUploadState state = new MultipartUploadState();
        state.setUploadId("uid-2");
        state.setObjectKey("2025/05/x.bin");
        state.setStorageId(1L);
        cache.save(state);

        multipartService.cancel("uid-2");
        verify(handler).abortMultipartUpload("uid-2", "2025/05/x.bin");
        assertThat(cache.get("uid-2")).isNull();
    }
}
