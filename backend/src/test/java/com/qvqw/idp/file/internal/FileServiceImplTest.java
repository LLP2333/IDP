package com.qvqw.idp.file.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.file.FileItem;
import com.qvqw.idp.file.FileTypeEnum;
import com.qvqw.idp.file.model.query.FileQuery;
import com.qvqw.idp.file.model.req.FileCreateDirReq;
import com.qvqw.idp.file.model.req.FileUpdateReq;
import com.qvqw.idp.file.model.resp.FileUploadResp;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StorageHandlerFactory;
import com.qvqw.idp.storage.StorageService;
import com.qvqw.idp.storage.StorageType;
import com.qvqw.idp.storage.StoredObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileServiceImplTest {

    @Mock
    FileRepository repository;

    @Mock
    StorageService storageService;

    @Mock
    StorageHandlerFactory handlerFactory;

    @Mock
    StorageHandler handler;

    @Mock
    ThumbnailGenerator thumbnailGenerator;

    FileServiceImpl service;

    Storage defaultStorage;

    @BeforeEach
    void setUp() {
        service = new FileServiceImpl(repository, storageService, handlerFactory, thumbnailGenerator);
        defaultStorage = new Storage();
        defaultStorage.setId(1L);
        defaultStorage.setCode("local");
        defaultStorage.setName("本地");
        defaultStorage.setType(StorageType.LOCAL.getValue());
        defaultStorage.setBucketName("/tmp/files/");
        defaultStorage.setDomain("http://localhost:8080/file/local/");
        defaultStorage.setRecycleBinEnabled(true);
        defaultStorage.setRecycleBinPath(".RECYCLE.BIN/");
        defaultStorage.setStatus(1);
        when(storageService.getDefaultStorage()).thenReturn(defaultStorage);
        when(storageService.findEntitiesByIds(anyCollection())).thenReturn(List.of(defaultStorage));
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
    void uploadEmptyFileShouldFail() {
        MultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.upload(empty, "/")).hasMessageContaining("上传文件不能为空");
    }

    @Test
    void uploadWithoutExtensionShouldFail() {
        MultipartFile noExt = new MockMultipartFile("file", "noext", "application/octet-stream", new byte[]{1, 2});
        assertThatThrownBy(() -> service.upload(noExt, "/")).hasMessageContaining("无扩展名");
    }

    @Test
    void uploadDisallowedExtensionShouldFail() {
        MultipartFile mf = new MockMultipartFile("file", "evil.exe", "application/x-msdownload", new byte[]{1, 2});
        assertThatThrownBy(() -> service.upload(mf, "/")).hasMessageContaining("不支持的文件扩展名");
    }

    @Test
    void uploadNormalFileShouldPersistAndReturnUrl() throws IOException {
        MultipartFile mf = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());
        when(repository.findFirstBySha256AndDeleted(anyString(), eq(0))).thenReturn(Optional.empty());
        when(handler.upload(any(), anyString(), anyLong(), any())).thenAnswer(inv -> {
            String key = inv.getArgument(1);
            long sz = inv.getArgument(2);
            return new StoredObject(key, "etag", sz, "text/plain");
        });
        when(handler.resolveUrl(anyString())).thenAnswer(inv -> "URL://" + inv.getArgument(0));

        FileUploadResp resp = service.upload(mf, "/");
        assertThat(resp.getId()).isNotNull();
        assertThat(resp.getUrl()).startsWith("URL://");
    }

    @Test
    void uploadShouldHitFastUploadWhenSha256Exists() throws IOException {
        MultipartFile mf = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());
        FileItem existing = new FileItem();
        existing.setId(99L);
        existing.setName("2025/05/abc.txt");
        existing.setOriginalName("hello.txt");
        existing.setSize(5L);
        existing.setExtension("txt");
        existing.setType(FileTypeEnum.DOC.getValue());
        existing.setSha256("sha");
        existing.setStorageId(1L);
        existing.setParentPath("/old");
        existing.setPath("/old/hello.txt");
        when(repository.findFirstBySha256AndDeleted(anyString(), eq(0))).thenReturn(Optional.of(existing));
        when(handler.resolveUrl(anyString())).thenAnswer(inv -> "URL://" + inv.getArgument(0));

        FileUploadResp resp = service.upload(mf, "/");
        assertThat(resp.getId()).isNotNull();
        verify(handler, never()).upload(any(), anyString(), anyLong(), any());
    }

    @Test
    void renameDuplicateShouldFail() {
        FileItem item = new FileItem();
        item.setId(1L);
        item.setOriginalName("old.txt");
        item.setParentPath("/");
        item.setPath("/old.txt");
        item.setType(FileTypeEnum.DOC.getValue());
        item.setStorageId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(repository.findFirstByParentPathAndNameAndTypeAndDeleted(eq("/"), eq("dup.txt"), eq(FileTypeEnum.DOC.getValue()), eq(0)))
                .thenReturn(Optional.of(new FileItem()));

        FileUpdateReq req = new FileUpdateReq();
        req.setOriginalName("dup.txt");
        assertThatThrownBy(() -> service.rename(1L, req)).hasMessageContaining("同目录下已存在同名");
    }

    @Test
    void renameShouldUpdatePath() {
        FileItem item = new FileItem();
        item.setId(2L);
        item.setOriginalName("old.txt");
        item.setParentPath("/docs");
        item.setPath("/docs/old.txt");
        item.setType(FileTypeEnum.DOC.getValue());
        item.setStorageId(1L);
        when(repository.findById(2L)).thenReturn(Optional.of(item));
        when(repository.findFirstByParentPathAndNameAndTypeAndDeleted(any(), any(), any(), any())).thenReturn(Optional.empty());

        FileUpdateReq req = new FileUpdateReq();
        req.setOriginalName("new.txt");
        service.rename(2L, req);
        assertThat(item.getOriginalName()).isEqualTo("new.txt");
        assertThat(item.getPath()).isEqualTo("/docs/new.txt");
    }

    @Test
    void deleteDirectoryWithChildrenShouldFail() {
        FileItem dir = new FileItem();
        dir.setId(3L);
        dir.setOriginalName("d");
        dir.setType(FileTypeEnum.DIR.getValue());
        dir.setStorageId(1L);
        dir.setPath("/d");
        dir.setParentPath("/");
        when(repository.findAllByIdInAndDeleted(List.of(3L), 0)).thenReturn(List.of(dir));
        when(repository.countChildren("/d", 1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(List.of(3L))).hasMessageContaining("非空");
    }

    @Test
    void deleteFileWithRecycleBinShouldMoveAndMark() {
        FileItem item = new FileItem();
        item.setId(4L);
        item.setName("2025/05/aa.txt");
        item.setOriginalName("aa.txt");
        item.setType(FileTypeEnum.DOC.getValue());
        item.setStorageId(1L);
        item.setPath("/aa.txt");
        item.setParentPath("/");
        when(repository.findAllByIdInAndDeleted(List.of(4L), 0)).thenReturn(List.of(item));

        service.delete(List.of(4L));

        verify(handler).move("2025/05/aa.txt", ".RECYCLE.BIN/2025/05/aa.txt");
        assertThat(item.getDeleted()).isEqualTo(1);
        assertThat(item.getName()).isEqualTo(".RECYCLE.BIN/2025/05/aa.txt");
    }

    @Test
    void deleteFileWithoutRecycleBinShouldHardDelete() {
        defaultStorage.setRecycleBinEnabled(false);
        FileItem item = new FileItem();
        item.setId(5L);
        item.setName("k.png");
        item.setType(FileTypeEnum.IMAGE.getValue());
        item.setStorageId(1L);
        item.setPath("/k.png");
        item.setParentPath("/");
        item.setThumbnailName("t.png");
        when(repository.findAllByIdInAndDeleted(List.of(5L), 0)).thenReturn(List.of(item));

        service.delete(List.of(5L));

        verify(handler).delete("k.png");
        verify(handler).delete("t.png");
        verify(repository).delete(item);
    }

    @Test
    void createDirShouldRejectDuplicate() {
        FileCreateDirReq req = new FileCreateDirReq();
        req.setParentPath("/");
        req.setOriginalName("docs");
        when(repository.findFirstByParentPathAndNameAndTypeAndDeleted("/", "docs", FileTypeEnum.DIR.getValue(), 0))
                .thenReturn(Optional.of(new FileItem()));
        assertThatThrownBy(() -> service.createDir(req)).hasMessageContaining("已存在同名文件夹");
    }

    @Test
    void calcDirSizeShouldAggregateRecursively() {
        FileItem dir = new FileItem();
        dir.setId(6L);
        dir.setType(FileTypeEnum.DIR.getValue());
        dir.setPath("/d");
        dir.setStorageId(1L);
        when(repository.findById(6L)).thenReturn(Optional.of(dir));

        FileItem subDir = new FileItem();
        subDir.setId(7L);
        subDir.setType(FileTypeEnum.DIR.getValue());
        subDir.setPath("/d/sub");
        subDir.setStorageId(1L);
        subDir.setSize(0L);

        FileItem fileA = new FileItem();
        fileA.setId(8L);
        fileA.setType(FileTypeEnum.DOC.getValue());
        fileA.setStorageId(1L);
        fileA.setSize(100L);

        FileItem fileB = new FileItem();
        fileB.setId(9L);
        fileB.setType(FileTypeEnum.DOC.getValue());
        fileB.setStorageId(1L);
        fileB.setSize(200L);

        when(repository.findAllByParentPathAndDeleted("/d", 0)).thenReturn(List.of(subDir, fileA));
        when(repository.findAllByParentPathAndDeleted("/d/sub", 0)).thenReturn(List.of(fileB));

        Long size = service.calcDirSize(6L);
        assertThat(size).isEqualTo(300L);
    }

    @Test
    void pageShouldFilterByDeletedZero() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>()));
        service.page(new FileQuery(), 1, 10);
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void countByStorageIdsDelegatesToRepository() {
        when(repository.countByStorageIdIn(List.of(1L, 2L))).thenReturn(7L);
        assertThat(service.countByStorageIds(List.of(1L, 2L))).isEqualTo(7L);
    }
}
