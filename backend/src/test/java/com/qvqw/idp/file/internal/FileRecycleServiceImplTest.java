package com.qvqw.idp.file.internal;

import com.qvqw.idp.file.FileItem;
import com.qvqw.idp.file.FileTypeEnum;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StorageHandlerFactory;
import com.qvqw.idp.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileRecycleServiceImplTest {

    @Mock
    FileRepository repository;

    @Mock
    StorageService storageService;

    @Mock
    StorageHandlerFactory handlerFactory;

    @Mock
    StorageHandler handler;

    FileServiceImpl fileService;
    FileRecycleServiceImpl recycleService;

    Storage storage;

    @BeforeEach
    void setUp() {
        ThumbnailGenerator thumb = new ThumbnailGenerator();
        fileService = new FileServiceImpl(repository, storageService, handlerFactory, thumb);
        recycleService = new FileRecycleServiceImpl(repository, storageService, handlerFactory, fileService);
        storage = new Storage();
        storage.setId(1L);
        storage.setName("本地");
        storage.setCode("local");
        storage.setRecycleBinEnabled(true);
        storage.setRecycleBinPath(".RECYCLE.BIN/");
        when(handlerFactory.get(any(Storage.class))).thenReturn(handler);
        when(storageService.findEntityById(1L)).thenReturn(storage);
        when(storageService.findEntitiesByIds(anyCollection())).thenReturn(List.of(storage));
    }

    @Test
    void restoreShouldMoveBackAndClearDeleted() {
        FileItem item = new FileItem();
        item.setId(1L);
        item.setName(".RECYCLE.BIN/2025/05/aa.txt");
        item.setType(FileTypeEnum.DOC.getValue());
        item.setStorageId(1L);
        item.setDeleted(1);
        when(repository.findById(1L)).thenReturn(Optional.of(item));

        recycleService.restore(1L);
        verify(handler).move(".RECYCLE.BIN/2025/05/aa.txt", "2025/05/aa.txt");
        assertThat(item.getDeleted()).isEqualTo(0);
        assertThat(item.getName()).isEqualTo("2025/05/aa.txt");
    }

    @Test
    void restoreNotInRecycleBinShouldFail() {
        FileItem item = new FileItem();
        item.setId(1L);
        item.setDeleted(0);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> recycleService.restore(1L)).hasMessageContaining("不在回收站");
    }

    @Test
    void deletePhysicalShouldRemoveFromStorageAndDb() {
        FileItem item = new FileItem();
        item.setId(2L);
        item.setName(".RECYCLE.BIN/x.txt");
        item.setType(FileTypeEnum.DOC.getValue());
        item.setStorageId(1L);
        item.setDeleted(1);
        when(repository.findAllByIdInAndDeleted(List.of(2L), 1)).thenReturn(List.of(item));

        recycleService.delete(2L);

        verify(handler).delete(".RECYCLE.BIN/x.txt");
        verify(repository).delete(item);
    }

    @Test
    void cleanShouldRemoveAll() {
        FileItem a = new FileItem();
        a.setId(3L);
        a.setName(".RECYCLE.BIN/a");
        a.setType(FileTypeEnum.DOC.getValue());
        a.setStorageId(1L);
        a.setDeleted(1);
        FileItem b = new FileItem();
        b.setId(4L);
        b.setName(".RECYCLE.BIN/b");
        b.setType(FileTypeEnum.IMAGE.getValue());
        b.setStorageId(1L);
        b.setDeleted(1);

        when(repository.findAllByDeleted(1)).thenReturn(List.of(a, b));
        recycleService.clean();
        verify(handler).delete(".RECYCLE.BIN/a");
        verify(handler).delete(".RECYCLE.BIN/b");
        verify(repository).delete(a);
        verify(repository).delete(b);
    }
}
